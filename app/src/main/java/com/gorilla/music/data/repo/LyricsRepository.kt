package com.gorilla.music.data.repo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gorilla.music.data.db.AppDatabase
import com.gorilla.music.data.db.LyricsEntity
import com.gorilla.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LyricsResult(val lyrics: String, val isSynced: Boolean)

fun JSONObject.getLyricsField(key: String): String? {
    if (!has(key)) return null
    if (isNull(key)) return null
    val value = optString(key, "")
    if (value.isBlank()) return null
    if (value == "null") return null
    return value
}

class LyricsRepository(
    private val context: Context,
    db: AppDatabase,
    private val music: MusicRepository,
) {
    private val lyricsDao = db.lyricsDao()

    companion object {
        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
    }

    suspend fun getEmbeddedLyrics(track: Track): String? {
        return music.embeddedLyrics(track)
    }

    suspend fun getCachedLyrics(trackId: Long): LyricsEntity? {
        return lyricsDao.getCachedLyrics(trackId)
    }

    suspend fun cacheLyrics(trackId: Long, lyrics: String, isSynced: Boolean) {
        lyricsDao.insertLyrics(
            LyricsEntity(
                trackId = trackId,
                lyrics = lyrics,
                isSynced = isSynced
            )
        )
    }

    suspend fun deleteCachedLyrics(trackId: Long) {
        lyricsDao.deleteCachedLyrics(trackId)
    }

    private val inFlightFetches = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.Deferred<Unit>>()

    suspend fun prefetchAndCacheLyrics(track: Track) = coroutineScope {
        val existing = inFlightFetches[track.id]
        if (existing != null && existing.isActive) {
            existing.await()
            return@coroutineScope
        }
        val job = async(Dispatchers.IO) {
            val cached = getCachedLyrics(track.id)
            if (cached != null && cached.lyrics.isNotBlank()) return@async

            val embedded = getEmbeddedLyrics(track)
            if (!embedded.isNullOrBlank()) {
                cacheLyrics(track.id, embedded, isSynced = false)
                return@async
            }

            if (!isNetworkAvailable()) return@async
            // Race all providers (YouLyPlus / Paxsenix / BetterLyrics / LRCLIB),
            // preferring word-synced lyrics — Echo Music's LyricsHelper pattern.
            val result = LyricsProviders.fetchBest(context, track) { fetchFromLrcLib(track) }
            if (result != null) {
                cacheLyrics(track.id, result.lyrics, result.isSynced)
            }
        }
        inFlightFetches[track.id] = job
        job.await()
        inFlightFetches.remove(track.id)
    }

    fun cleanTrackTitle(title: String, artist: String): String {
        var t = title
        val a = artist.trim()
        if (a.isNotEmpty() && t.startsWith(a, ignoreCase = true)) {
            t = t.substring(a.length).trim()
            if (t.startsWith("-") || t.startsWith("–")) {
                t = t.substring(1).trim()
            }
        }
        return t
            .replace(Regex("\\(.*?\\)"), "")       // Remove (feat. ...) etc
            .replace(Regex("\\[.*?\\]"), "")       // Remove [Radio Edit] etc
            .trim()
    }

    fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex(",.*"), "")             // Take first artist only if multiple
            .replace(Regex("/.*"), "")             // Remove "Artist / Artist2"
            .trim()
    }

    private fun tryExactMatch(artist: String, title: String, album: String, durationSec: Int): LyricsResult? {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("lrclib.net")
            .addPathSegments("api/get")
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("track_name", title)
            .addQueryParameter("album_name", album.trim())
            .addQueryParameter("duration", durationSec.toString())
            .build()

        android.util.Log.d("LRCLIB_DIAG", "Exact Request URL: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                android.util.Log.d("LRCLIB_DIAG", "Exact Response Code: ${response.code}, Body: ${body?.take(200)}")
                if (response.isSuccessful && body != null && body.isNotBlank()) {
                    val json = JSONObject(body)
                    val synced = json.getLyricsField("syncedLyrics")
                    val plain = json.getLyricsField("plainLyrics")
                    if (synced != null) return LyricsResult(synced, isSynced = true)
                    if (plain != null) return LyricsResult(plain, isSynced = false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("LYRICS FETCH FAILED: ${e.javaClass.simpleName}: ${e.message}")
            android.util.Log.e("LyricsRepo", "Exception in tryExactMatch", e)
        }
        return null
    }

    private fun trySearchFallback(artist: String, title: String): LyricsResult? {
        try {
            val searchUrl = HttpUrl.Builder()
                .scheme("https")
                .host("lrclib.net")
                .addPathSegments("api/search")
                .addQueryParameter("q", "$artist $title")
                .build()

            android.util.Log.d("LRCLIB_DIAG", "Search Request URL: $searchUrl")

            val searchRequest = Request.Builder()
                .url(searchUrl)
                .build()

            httpClient.newCall(searchRequest).execute().use { searchResponse ->
                val searchBody = searchResponse.body?.string()
                android.util.Log.d("LRCLIB_DIAG", "Search Response Code: ${searchResponse.code}, Body: ${searchBody?.take(200)}")
                if (searchResponse.isSuccessful && searchBody != null && searchBody.isNotBlank()) {
                    val results = JSONArray(searchBody)
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val synced = item.getLyricsField("syncedLyrics")
                        val plain = item.getLyricsField("plainLyrics")
                        if (synced != null) return LyricsResult(synced, isSynced = true)
                        if (plain != null) return LyricsResult(plain, isSynced = false)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("LYRICS FETCH FAILED: ${e.javaClass.simpleName}: ${e.message}")
            android.util.Log.e("LyricsRepo", "Exception in trySearchFallback", e)
        }
        return null
    }

    suspend fun fetchFromLrcLib(track: Track): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanArtist = cleanArtistName(track.displayArtist)
        val cleanTitle = cleanTrackTitle(track.overrideTitle ?: track.title, cleanArtist)

        // Try exact match with exact duration, then ±3 seconds
        val baseDuration = (track.durationMs / 1000).toInt()
        val durations = listOf(baseDuration, baseDuration + 3, baseDuration - 3)

        for (duration in durations) {
            val result = tryExactMatch(cleanArtist, cleanTitle, track.displayAlbum, duration)
            if (result != null) return@withContext result
        }

        // Final fallback: search endpoint
        return@withContext trySearchFallback(cleanArtist, cleanTitle)
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
