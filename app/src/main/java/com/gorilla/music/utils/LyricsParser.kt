package com.gorilla.music.utils

import com.gorilla.music.data.model.LyricsEntry
import com.gorilla.music.data.model.WordTimestamp

/**
 * Lyrics parser supporting plain LRC, enhanced (word-synced) LRC with
 * `<mm:ss.xx>` inline word tags, follow-up `<word:start:end|...>` timing
 * lines, `{bg}` background-vocal markers and `{agent:v1|v2}` duet markers.
 *
 * Ported from Echo Music's lyrics/LyricsUtils.kt (romanization/translation
 * machinery dropped).
 */
object LyricsParser {

    private const val MINUTE_IN_MILLIS = 60_000L
    private const val SECOND_IN_MILLIS = 1_000L

    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

    private val RICH_SYNC_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.+)".toRegex()
    private val RICH_SYNC_WORD_REGEX = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>\\s*([^<]+)".toRegex()

    private val AGENT_REGEX = "\\{agent:([^}]+)\\}".toRegex()
    private val BACKGROUND_REGEX = "^\\{bg\\}".toRegex()

    private val HEX_ENTITY_REGEX = "&#x([0-9a-fA-F]+);".toRegex()
    private val DEC_ENTITY_REGEX = "&#(\\d+);".toRegex()

    private fun decodeHtmlEntities(text: String): String =
        text
            .replace(HEX_ENTITY_REGEX) { match ->
                match.groupValues[1].toIntOrNull(16)
                    ?.takeIf { it in 0..0x10FFFF }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
            .replace(DEC_ENTITY_REGEX) { match ->
                match.groupValues[1].toIntOrNull()
                    ?.takeIf { it in 0..0x10FFFF }
                    ?.let { String(Character.toChars(it)) }
                    ?: match.value
            }
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val unescapedLyrics = lyrics
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")

        val decodedLyrics = decodeHtmlEntities(unescapedLyrics)

        val lines = decodedLyrics.lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("[offset:") }

        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) &&
                RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }

        return if (isRichSync) {
            parseRichSyncLyrics(lines)
        } else {
            parseStandardLyrics(lines)
        }
    }

    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()

        lines.forEachIndexed { index, line ->
            val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(line.trim())
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L

                val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * MINUTE_IN_MILLIS + seconds * SECOND_IN_MILLIS + millisPart

                var content = matchResult.groupValues[4].trimStart()

                val agentMatch = AGENT_REGEX.find(content)
                val agent = agentMatch?.groupValues?.get(1)
                if (agentMatch != null) {
                    content = content.replaceFirst(AGENT_REGEX, "")
                }

                val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
                if (isBackground) {
                    content = content.replaceFirst(BACKGROUND_REGEX, "")
                }

                val wordTimings = parseRichSyncWords(content, index, lines)

                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()

                if (plainText.isNotBlank()) {
                    result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = agent, isBackground = isBackground))
                }
            }
        }

        return result.sorted()
    }

    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<WordTimestamp>? {
        val wordMatches = RICH_SYNC_WORD_REGEX.findAll(content).toList()

        if (wordMatches.isEmpty()) return null

        val wordTimings = mutableListOf<WordTimestamp>()

        wordMatches.forEachIndexed { index, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].toLongOrNull() ?: 0L

            val fractionPart = if (match.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            val startTimeSeconds = minutes * 60.0 + seconds + fractionPart

            val wordText = match.groupValues[4].trim()

            val endTimeSeconds = if (index < wordMatches.size - 1) {
                val nextMatch = wordMatches[index + 1]
                val nextMinutes = nextMatch.groupValues[1].toLongOrNull() ?: 0L
                val nextSeconds = nextMatch.groupValues[2].toLongOrNull() ?: 0L
                val nextFraction = nextMatch.groupValues[3].toLongOrNull() ?: 0L
                val nextFractionPart = if (nextMatch.groupValues[3].length == 3) nextFraction / 1000.0 else nextFraction / 100.0
                nextMinutes * 60.0 + nextSeconds + nextFractionPart
            } else {
                val nextLineTime = getNextLineStartTime(currentIndex, allLines)
                nextLineTime ?: (startTimeSeconds + 0.5)
            }

            if (wordText.isNotBlank()) {
                wordTimings.add(WordTimestamp(wordText, startTimeSeconds, endTimeSeconds))
            }
        }

        return if (wordTimings.isNotEmpty()) wordTimings else null
    }

    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Double? {
        if (currentIndex + 1 >= allLines.size) return null

        val nextLine = allLines[currentIndex + 1].trim()
        val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(nextLine) ?: return null

        val minutes = matchResult.groupValues[1].toLongOrNull() ?: return null
        val seconds = matchResult.groupValues[2].toLongOrNull() ?: return null
        val fraction = matchResult.groupValues[3].toLongOrNull() ?: 0L

        val fractionPart = if (matchResult.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
        return minutes * 60.0 + seconds + fractionPart
    }

    private fun parseStandardLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.trim().startsWith("<") || !line.trim().endsWith(">")) {
                val entries = parseLine(line)
                if (entries != null) {
                    val wordTimestamps = if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (nextLine.trim().startsWith("<") && nextLine.trim().endsWith(">")) {
                            parseWordTimestamps(nextLine.trim().removeSurrounding("<", ">"))
                        } else null
                    } else null

                    if (wordTimestamps != null) {
                        result.addAll(entries.map { entry ->
                            LyricsEntry(entry.time, entry.text, wordTimestamps, agent = entry.agent, isBackground = entry.isBackground)
                        })
                    } else {
                        result.addAll(entries)
                    }
                }
            }
            i++
        }
        return result.sorted()
    }

    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size == 3) {
                    WordTimestamp(
                        text = parts[0],
                        startTime = parts[1].toDoubleOrNull() ?: 0.0,
                        endTime = parts[2].toDoubleOrNull() ?: 0.0,
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLine(line: String, words: List<WordTimestamp>? = null): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        var text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) {
            text = text.replaceFirst(AGENT_REGEX, "")
        }

        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) {
            text = text.replaceFirst(BACKGROUND_REGEX, "")
        }

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLongOrNull() ?: 0L
                val sec = timeMatchResult.groupValues[2].toLongOrNull() ?: 0L
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLongOrNull() ?: 0L
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * MINUTE_IN_MILLIS + sec * SECOND_IN_MILLIS + mil
                LyricsEntry(time, text.trim(), words, agent = agent, isBackground = isBackground)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        var low = 0
        var high = lines.lastIndex
        var result = -1

        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lines[middle].time <= position) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }

        return result
    }
}
