package com.gorilla.music.data.repo

import android.content.Context
import com.gorilla.music.data.model.Track
import com.music.paxsenix.Paxsenix
import com.music.youlyplus.YouLyPlus
import iad1tya.echo.music.betterlyrics.BetterLyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Online lyric providers, raced concurrently (Echo Music's LyricsHelper
 * pattern, condensed for Gorilla — no Hilt, no per-provider settings).
 *
 * Word-synced results (enhanced LRC with <mm:ss.xx> word tags or follow-up
 * <word:start:end|...> timing lines) win immediately; otherwise the first
 * line-synced result wins; plain lyrics are the last resort.
 */
object LyricsProviders {

    private const val TAG = "LyricsProviders"

    private interface Provider {
        val name: String
        suspend fun fetch(title: String, artist: String, durationSec: Int, album: String?): String?
    }

    private val youLyPlus = object : Provider {
        override val name = "YouLyPlus"
        override suspend fun fetch(title: String, artist: String, durationSec: Int, album: String?): String? =
            YouLyPlus.getLyrics(title, artist, durationSec, album).getOrNull()
    }

    private val paxsenix = object : Provider {
        override val name = "Paxsenix"
        override suspend fun fetch(title: String, artist: String, durationSec: Int, album: String?): String? =
            Paxsenix.getLyrics(title, artist, durationSec, album).getOrNull()
    }

    private val betterLyrics = object : Provider {
        override val name = "BetterLyrics"
        override suspend fun fetch(title: String, artist: String, durationSec: Int, album: String?): String? =
            BetterLyrics.getLyrics(title, artist, durationSec, album).getOrNull()
    }

    private enum class Quality { WORD_SYNCED, LINE_SYNCED, PLAIN }

    private fun classify(lyrics: String): Quality {
        val trimmed = lyrics.trimStart()
        val lineSynced = trimmed.startsWith("[")
        if (!lineSynced) return Quality.PLAIN
        // Enhanced LRC inline word tags or follow-up word timing lines.
        val wordSynced = lyrics.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>")) ||
            lyrics.lines().any { it.trim().let { l -> l.startsWith("<") && l.endsWith(">") && l.contains("|") } }
        return if (wordSynced) Quality.WORD_SYNCED else Quality.LINE_SYNCED
    }

    /**
     * Race all online providers plus the given LRCLIB fallback. Returns the
     * best result by quality, preferring word-synced immediately.
     */
    suspend fun fetchBest(
        context: Context,
        track: Track,
        lrcLib: suspend () -> LyricsResult?,
    ): LyricsResult? = withContext(Dispatchers.IO) {
        Paxsenix.init(context.applicationContext)

        val title = track.overrideTitle ?: track.title
        val artist = track.displayArtist
        val durationSec = (track.durationMs / 1000).toInt()
        val album = track.displayAlbum.takeIf { it.isNotBlank() }

        data class Response(val lyrics: String, val quality: Quality)

        coroutineScope {
            val providers = listOf(youLyPlus, paxsenix, betterLyrics)
            val channel = Channel<Response?>(providers.size + 1)

            providers.forEach { provider ->
                launch {
                    val lyrics = try {
                        provider.fetch(title, artist, durationSec, album)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "${provider.name} failed: ${e.message}")
                        null
                    }
                    channel.send(lyrics?.takeIf { it.isNotBlank() }?.let { Response(it, classify(it)) })
                }
            }
            launch {
                val result = try {
                    lrcLib()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "LrcLib failed: ${e.message}")
                    null
                }
                channel.send(result?.let { Response(it.lyrics, classify(it.lyrics)) })
            }

            var best: Response? = null
            repeat(providers.size + 1) {
                val response = channel.receive() ?: return@repeat
                if (response.quality == Quality.WORD_SYNCED) {
                    coroutineContext[Job]?.cancelChildren()
                    return@coroutineScope LyricsResult(response.lyrics, isSynced = true)
                }
                if (best == null || response.quality < best!!.quality) {
                    best = response
                }
            }
            best?.let { LyricsResult(it.lyrics, isSynced = it.quality != Quality.PLAIN) }
        }
    }
}
