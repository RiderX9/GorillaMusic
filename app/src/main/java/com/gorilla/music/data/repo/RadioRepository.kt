package com.gorilla.music.data.repo

import android.net.Uri
import com.gorilla.music.data.model.RadioStation
import com.gorilla.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import kotlin.math.absoluteValue

class RadioRepository {
    private val cachedStations = mutableMapOf<String, List<RadioStation>>()

    suspend fun fetchRegionStations(
        regionKey: String,
        forceRefresh: Boolean = false,
    ): Result<List<RadioStation>> = withContext(Dispatchers.IO) {
        val key = regionKey.lowercase(Locale.US)
        if (!forceRefresh) {
            cachedStations[key]?.let {
                return@withContext Result.success(it)
            }
        }

        val countryCodes = when (key) {
            "balkan" -> listOf("AL", "GR", "RS", "BG", "HR", "RO", "BA", "MK", "ME")
            "europe" -> listOf("DE", "FR", "GB", "HU", "IT", "ES", "NL", "SE", "AT", "CH", "PL")
            "usa" -> listOf("US", "CA")
            else -> emptyList() // worldwide
        }

        runCatching {
            val raw = if (countryCodes.isEmpty()) {
                fetchEndpoint("https://de1.api.radio-browser.info/json/stations/topvote/80?hidebroken=true")
            } else {
                countryCodes.flatMap { code ->
                    fetchEndpoint("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/$code?hidebroken=true&order=votes&reverse=true&limit=24")
                }
            }

            raw.filter { it.isPlayableMusicOrPodcastStation() }
                .distinctBy { it.streamUrl }
                .sortedWith(compareByDescending<RadioStation> { it.votes }.thenByDescending { it.clickCount }.thenByDescending { it.bitrate })
                .take(40)
                .also { cachedStations[key] = it }
        }
    }

    private fun fetchEndpoint(endpoint: String): List<RadioStation> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "GorillaMusic/1.6")
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return parseStations(body)
    }

    private fun parseStations(body: String): List<RadioStation> {
        val array = JSONArray(body)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val streamUrl = item.optString("url_resolved").ifBlank { item.optString("url") }.trim()
                if (name.isBlank() || streamUrl.isBlank()) continue

                val tags = item.optString("tags")
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(4)

                val countryCode = item.optString("countrycode").trim().uppercase(Locale.US)

                add(
                    RadioStation(
                        id = -stableId(streamUrl),
                        name = name,
                        streamUrl = streamUrl,
                        homepage = item.optString("homepage").trim(),
                        favicon = item.optString("favicon").trim()
                            .ifBlank { homepageFavicon(item.optString("homepage").trim()) },
                        tags = tags,
                        country = item.optString("country").trim(),
                        countryCode = countryCode,
                        bitrate = item.optInt("bitrate", 0),
                        codec = item.optString("codec").trim().uppercase(Locale.US),
                        hls = item.optInt("hls", 0) == 1,
                        votes = item.optInt("votes", 0),
                        clickCount = item.optInt("clickcount", 0),
                    )
                )
            }
        }
    }

    private fun RadioStation.isPlayableMusicOrPodcastStation(): Boolean {
        if (!streamUrl.startsWith("http", ignoreCase = true)) return false
        if (hls || streamUrl.isPlaylistUrl()) return false

        val codecAllowed = codec in playableCodecs
        val urlLooksPlayable = streamUrl.lowercase(Locale.US).substringBefore('?').let { url ->
            playableAudioExtensions.any { url.endsWith(it) }
        }
        if (!codecAllowed && !urlLooksPlayable) return false

        val searchText = buildString {
            append(name.lowercase(Locale.US))
            append(' ')
            append(tags.joinToString(" ").lowercase(Locale.US))
        }

        if (blockedRadioTerms.any { searchText.contains(it) }) return false
        return wantedRadioTerms.any { searchText.contains(it) }
    }

    private fun String.isPlaylistUrl(): Boolean {
        val normalized = lowercase(Locale.US).substringBefore('?')
        return playlistExtensions.any { normalized.endsWith(it) }
    }

    private fun stableId(value: String): Long {
        var hash = 1125899906842597L
        value.forEach { char -> hash = 31 * hash + char.code }
        return hash.absoluteValue.coerceAtLeast(1L)
    }

    private fun homepageFavicon(homepage: String): String {
        if (!homepage.startsWith("http", ignoreCase = true)) return ""
        val host = runCatching { URI(homepage).host }.getOrNull()?.removePrefix("www.") ?: return ""
        return "https://www.google.com/s2/favicons?domain=$host&sz=256"
    }

    companion object {
        private val playableCodecs = setOf("MP3", "AAC", "AAC+", "OGG", "OPUS", "FLAC")
        private val playableAudioExtensions = listOf(".mp3", ".aac", ".ogg", ".oga", ".opus", ".flac")
        private val playlistExtensions = listOf(".m3u", ".m3u8", ".pls", ".xspf", ".asx", ".ram", ".wax", ".wpl")
        private val blockedRadioTerms = listOf(
            "news",
            "talk",
            "sports",
            "sport",
            "religion",
            "religious",
            "christian",
            "islam",
            "quran",
            "weather",
            "scanner",
            "police",
            "emergency",
            "traffic",
            "business",
            "politics",
            "public radio",
            "community radio",
            "parliament",
            "sermon",
            "church",
            "bible",
        )
        private val wantedRadioTerms = listOf(
            "music",
            "pop",
            "rock",
            "dance",
            "edm",
            "electronic",
            "house",
            "techno",
            "trance",
            "jazz",
            "classical",
            "hip hop",
            "hip-hop",
            "rap",
            "r&b",
            "soul",
            "metal",
            "indie",
            "alternative",
            "hits",
            "top 40",
            "oldies",
            "80s",
            "90s",
            "00s",
            "latin",
            "reggaeton",
            "podcast",
        )
    }
}

fun RadioStation.toTrack(): Track =
    Track(
        id = id,
        title = name,
        artist = country.ifBlank { "Online Radio" },
        album = tags.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Live Radio",
        albumId = id,
        durationMs = 0L,
        uri = Uri.parse(streamUrl),
        data = streamUrl,
        folder = "radio",
        mimeType = codec.toRadioMimeType(),
        size = 0L,
        dateAddedSec = 0L,
        trackNumber = 0,
        year = 0,
        genre = tags.firstOrNull(),
        artworkUri = favicon.takeIf { it.startsWith("http", ignoreCase = true) }?.let(Uri::parse),
    )

private fun String.toRadioMimeType(): String =
    when (uppercase(Locale.US)) {
        "MP3" -> "audio/mpeg"
        "AAC", "AAC+" -> "audio/aac"
        "OGG" -> "audio/ogg"
        "OPUS" -> "audio/opus"
        "FLAC" -> "audio/flac"
        else -> "audio/mpeg"
    }
