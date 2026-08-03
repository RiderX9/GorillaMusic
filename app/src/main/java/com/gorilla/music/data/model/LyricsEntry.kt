package com.gorilla.music.data.model

/**
 * Word-level timestamp inside a synced lyric line (times in seconds).
 * Ported from Echo Music's lyrics/LyricsEntry.kt.
 */
data class WordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double,
)

/**
 * A parsed lyric line with optional word-level timings, duet agent and
 * background-vocal flag. Ported from Echo Music (romanization/translation
 * flows dropped — Gorilla doesn't use them).
 */
data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    val agent: String? = null,
    val isBackground: Boolean = false,
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}
