package com.gorilla.music.data.repo

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.gorilla.music.data.db.AppDatabase
import com.gorilla.music.data.db.RecentlyPlayedEntity
import com.gorilla.music.data.db.TrackEntity
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.db.PlaylistTrackEntity
import com.gorilla.music.data.model.Album
import com.gorilla.music.data.model.Artist
import com.gorilla.music.data.model.Folder
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.model.TrackTechInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * Scans device storage for audio, parses tags, and caches everything in Room so the
 * library is only rescanned when explicitly triggered. Exposes grouped views
 * (artists / albums / folders) derived from the cached tracks.
 */
class MusicRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val trackDao = db.trackDao()
    private val recentDao = db.recentlyPlayedDao()

    private val _scanResults = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val scanResults = _scanResults.asSharedFlow()

    private val _mediaStoreChanges = MutableSharedFlow<Unit>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private var contentObserver: ContentObserver? = null
    private var observerJob: Job? = null

    private fun hasPermission(): Boolean {
        val perm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    @OptIn(FlowPreview::class)
    fun registerContentObserver(scope: CoroutineScope) {
        if (contentObserver != null) return

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                _mediaStoreChanges.tryEmit(Unit)
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            contentObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }

        observerJob = scope.launch {
            _mediaStoreChanges
                .debounce(2000)
                .collect {
                    if (hasPermission()) {
                        val added = incrementalScan()
                        if (added > 0) {
                            _scanResults.emit(added)
                        }
                    }
                }
        }
    }

    fun unregisterContentObserver() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        contentObserver = null
        observerJob?.cancel()
        observerJob = null
    }

    private val supportedMimes = setOf(
        "audio/mpeg", "audio/mp3",
        "audio/flac", "audio/x-flac",
        "audio/aac", "audio/mp4", "audio/m4a", "audio/x-m4a",
        "audio/wav", "audio/x-wav",
        "audio/ogg", "application/ogg",
        "audio/alac", "audio/x-alac",
    )

    val tracks: Flow<List<Track>> = trackDao.observeAll().map { list -> list.map { it.toTrack() } }

    fun recentlyPlayed(limit: Int = 20): Flow<List<Track>> =
        recentDao.observeRecent(limit).map { list -> list.map { it.toTrack() } }

    fun mostPlayed(limit: Int = 20): Flow<List<PlayedTrack>> =
        recentDao.observeMostPlayed(limit).map { list ->
            list.map { PlayedTrack(it.track.toTrack(), it.playCount) }
        }

    suspend fun isLibraryCached(): Boolean = withContext(Dispatchers.IO) { trackDao.count() > 0 }

    private fun detectActualFormat(filePath: String, mimeType: String?): String {
        if (mimeType == "audio/mp4" || mimeType == "audio/m4a") {
            try {
                val extractor = android.media.MediaExtractor()
                extractor.setDataSource(filePath)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                    if (mime.contains("alac", ignoreCase = true)) {
                        extractor.release()
                        return "ALAC"
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                // Fall through to default
            }
        }
        return mimeType ?: "Unknown"
    }

    /** Full MediaStore scan → Room. Returns number of tracks found. Favorites are preserved. */
    suspend fun scanLibrary(): Int = withContext(Dispatchers.IO) {
        val found = queryMediaStore()
        if (found.isNotEmpty()) {
            val favorites = trackDao.favoriteIds().filter { it.isFavorite }.map { it.id }.toSet()
            trackDao.upsertAll(found.map { TrackEntity.from(it).copy(isFavorite = it.id in favorites) })
            trackDao.deleteMissing(found.map { it.id })
        } else {
            trackDao.clear()
        }
        found.size
    }

    /**
     * Incremental scan run automatically on app open. Compares the live MediaStore
     * listing against the Room cache by id + last-modified stamp, and only writes rows
     * that are genuinely new or changed (and drops rows whose files vanished). Favorites
     * are preserved. Returns the number of *new* tracks added (for the "Added N" snackbar);
     * a no-op scan returns 0.
     */
    suspend fun incrementalScan(): Int = withContext(Dispatchers.IO) {
        val found = queryMediaStore()
        if (found.isEmpty()) {
            // Don't wipe the cache on an empty incremental result — that's almost
            // certainly a transient MediaStore hiccup, not a truly empty device.
            return@withContext 0
        }
        val cached = trackDao.pathStamps().associate { it.id to it.dateAddedSec }
        val favorites = trackDao.favoriteIds().filter { it.isFavorite }.map { it.id }.toSet()

        val newOrChanged = found.filter { t ->
            val stamp = cached[t.id]
            stamp == null || stamp != t.dateAddedSec
        }
        val addedCount = found.count { it.id !in cached }

        if (newOrChanged.isNotEmpty()) {
            trackDao.upsertAll(newOrChanged.map { TrackEntity.from(it).copy(isFavorite = it.id in favorites) })
        }
        // Remove tracks whose underlying files are gone.
        trackDao.deleteMissing(found.map { it.id })
        addedCount
    }

    private fun queryMediaStore(): List<Track> {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val out = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (c.moveToNext()) {
                val rawMime = c.getString(mimeCol) ?: ""
                val path = c.getString(dataCol) ?: ""
                val ext = path.substringAfterLast('.', "").lowercase()
                if (rawMime.lowercase() !in supportedMimes && ext !in setOf("alac", "caf", "m4a")) continue
                
                val actualMime = detectActualFormat(path, rawMime)
                
                val id = c.getLong(idCol)
                val folderPath = path.substringBeforeLast('/', "")
                val uri = ContentUris.withAppendedId(collection, id)
                out += Track(
                    id = id,
                    title = (c.getString(titleCol) ?: "Unknown").ifBlank { "Unknown" },
                    artist = c.getString(artistCol).orEmptyArtist(),
                    album = c.getString(albumCol) ?: "",
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    uri = uri,
                    data = path,
                    folder = folderPath,
                    mimeType = actualMime,
                    size = c.getLong(sizeCol),
                    dateAddedSec = c.getLong(dateCol),
                    trackNumber = c.getInt(trackCol),
                    year = c.getInt(yearCol),
                )
            }
        }
        return out
    }

    private fun String?.orEmptyArtist(): String =
        if (this == null || this == "<unknown>") "" else this

    suspend fun getTrack(id: Long): Track? = withContext(Dispatchers.IO) {
        trackDao.getById(id)?.toTrack()
    }

    /** Toggles a track's favorite flag, persists immediately, and returns the new value. */
    suspend fun toggleFavorite(trackId: Long): Boolean = withContext(Dispatchers.IO) {
        val next = !(trackDao.isFavorite(trackId) ?: false)
        trackDao.updateFavorite(trackId, next)

        val playlistDao = db.playlistDao()
        var favPlaylist = playlistDao.getByName("Favourites")
        if (favPlaylist == null) {
            val id = playlistDao.insertPlaylist(
                PlaylistEntity(name = "Favourites", createdAt = System.currentTimeMillis())
            )
            favPlaylist = playlistDao.getPlaylist(id)
        }
        if (favPlaylist != null) {
            if (next) {
                val exists = playlistDao.isTrackInPlaylist(favPlaylist.id, trackId)
                if (!exists) {
                    val pos = playlistDao.maxPosition(favPlaylist.id) + 1
                    playlistDao.insertPlaylistTrack(PlaylistTrackEntity(favPlaylist.id, trackId, pos))
                }
            } else {
                playlistDao.removeTrack(favPlaylist.id, trackId)
                // Repack positions
                val rows = playlistDao.rowsFor(favPlaylist.id)
                val repacked = rows.mapIndexed { index, row -> row.copy(position = index) }
                playlistDao.upsertRows(repacked)
            }
        }
        next
    }

    /**
     * Builds a system consent request for deleting [tracks] from shared storage
     * (required on Android 11+). The caller launches the returned [android.content.IntentSender]
     * with an Activity result launcher; on approval, call [onDeleteConfirmed] to drop
     * the rows from the Room cache.
     */
    fun buildDeleteRequest(tracks: List<Track>): android.content.IntentSender {
        val uris = tracks.map { it.uri }
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    /** Remove a track from the Room cache after the OS confirms the file delete. */
    suspend fun onDeleteConfirmed(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.deleteById(trackId)
    }

    /**
     * Builds a system consent request to write a new [newTitle] into a track's
     * MediaStore `TITLE`. Returns null if the write succeeded directly (the app owns
     * the file); otherwise returns the [android.content.IntentSender] to launch for
     * user approval. After approval the caller re-applies via [applyRename].
     */
    suspend fun renameTrack(track: Track, newTitle: String): android.content.IntentSender? =
        withContext(Dispatchers.IO) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newTitle.trim())
                put(MediaStore.Audio.Media.DISPLAY_NAME, newTitle.trim())
            }
            try {
                context.contentResolver.update(track.uri, values, null, null)
                trackDao.updateTitle(track.id, newTitle.trim())
                null
            } catch (e: android.app.RecoverableSecurityException) {
                e.userAction.actionIntent.intentSender
            }
        }

    /** Apply the cached title update after a rename consent request is approved. */
    suspend fun applyRename(track: Track, newTitle: String) = withContext(Dispatchers.IO) {
        val values = android.content.ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, newTitle.trim())
            put(MediaStore.Audio.Media.DISPLAY_NAME, newTitle.trim())
        }
        runCatching { context.contentResolver.update(track.uri, values, null, null) }
        trackDao.updateTitle(track.id, newTitle.trim())
    }

    suspend fun updateOverrides(
        trackId: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?,
        customLyrics: String?,
        isSynced: Boolean,
    ) = withContext(Dispatchers.IO) {
        trackDao.updateOverrides(
            trackId,
            title,
            artist,
            album,
            genre,
            year,
            customLyrics,
            isSynced
        )
    }

    fun buildWriteRequest(track: Track): android.app.PendingIntent {
        return MediaStore.createWriteRequest(context.contentResolver, listOf(track.uri))
    }

    suspend fun writePhysicalTags(
        track: Track,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?,
    ): Boolean = withContext(Dispatchers.IO) {
        val file = java.io.File(track.data)
        if (!file.exists()) return@withContext false
        try {
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val tag = audioFile.tag ?: audioFile.createDefaultTag()

            if (!title.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, title) else tag.deleteField(org.jaudiotagger.tag.FieldKey.TITLE)
            if (!artist.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, artist) else tag.deleteField(org.jaudiotagger.tag.FieldKey.ARTIST)
            if (!album.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, album) else tag.deleteField(org.jaudiotagger.tag.FieldKey.ALBUM)
            if (!genre.isNullOrBlank()) tag.setField(org.jaudiotagger.tag.FieldKey.GENRE, genre) else tag.deleteField(org.jaudiotagger.tag.FieldKey.GENRE)
            if (year != null && year > 0) tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, year.toString()) else tag.deleteField(org.jaudiotagger.tag.FieldKey.YEAR)

            audioFile.tag = tag
            audioFile.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun rescanFile(track: Track, onComplete: () -> Unit = {}) {
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(track.data),
            null
        ) { _, _ ->
            onComplete()
        }
    }

    /** Records a play for the recently-played row. [now] is supplied by the caller. */
    suspend fun recordPlay(trackId: Long, now: Long) = withContext(Dispatchers.IO) {
        val existing = recentDao.get(trackId)
        recentDao.upsert(
            RecentlyPlayedEntity(
                trackId = trackId,
                lastPlayedAt = now,
                playCount = (existing?.playCount ?: 0) + 1,
            )
        )
    }

    // ---- Grouped views (derived from cached tracks) ----

    suspend fun albums(): List<Album> = withContext(Dispatchers.IO) {
        trackDao.getAll().map { it.toTrack() }
            .groupBy { Pair(it.displayAlbum.lowercase(), it.displayArtist.lowercase()) }
            .map { (_, group) ->
                val first = group.first()
                Album(
                    id = first.albumId,
                    title = first.displayAlbum,
                    artist = first.displayArtist,
                    trackCount = group.size,
                    firstTrackId = first.id,
                )
            }.sortedBy { it.title.lowercase() }
    }

    suspend fun artists(): List<Artist> = withContext(Dispatchers.IO) {
        trackDao.getAll().map { it.toTrack() }
            .groupBy { it.displayArtist }
            .map { (name, group) ->
                val representative = group.firstOrNull { it.artworkUri != null } ?: group.first()
                Artist(
                    name = name,
                    trackCount = group.size,
                    albumCount = group.map { it.albumId }.distinct().size,
                    representativeAlbumId = representative.albumId,
                    artworkUri = representative.artworkUri,
                )
            }.sortedBy { it.name.lowercase() }
    }

    suspend fun folders(): List<Folder> = withContext(Dispatchers.IO) {
        trackDao.getAll().map { it.toTrack() }
            .filter { it.folder.isNotBlank() }
            .groupBy { it.folder }
            .map { (path, group) ->
                Folder(
                    path = path,
                    name = path.substringAfterLast('/').ifBlank { path },
                    trackCount = group.size,
                )
            }.sortedBy { it.name.lowercase() }
    }

    /**
     * Extracts detailed technical info for the Track Info sheet using
     * [MediaMetadataRetriever] for tags and [MediaExtractor]/[MediaFormat] for the
     * precise sample rate / channel count / encoding the retriever omits.
     */
    suspend fun techInfo(track: Track): TrackTechInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var bitrate = 0
        var metadataMime: String? = null
        try {
            retriever.setDataSource(context, track.uri)
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.div(1000) ?: 0
            metadataMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (_: Exception) {
            // fall through to defaults
        } finally {
            runCatching { retriever.release() }
        }

        var sampleRate = 0
        var channels = 0
        var encoding = "—"
        var codec = "—"
        var isAlac = false
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, track.uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("audio/")) continue
                if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                encoding = mime.substringAfter('/').uppercase()
                codec = codecLabel(mime)
                if (mime.contains("alac", ignoreCase = true)) {
                    isAlac = true
                }
                if (bitrate == 0 && fmt.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    bitrate = fmt.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                }
                break
            }
        } catch (_: Exception) {
            // leave defaults
        } finally {
            runCatching { extractor.release() }
        }

        val isReallyAlac = isAlac ||
            (metadataMime != null && metadataMime.contains("alac", ignoreCase = true)) ||
            track.mimeType.contains("alac", ignoreCase = true) ||
            track.data.endsWith(".alac", ignoreCase = true) ||
            track.data.endsWith(".caf", ignoreCase = true)

        val finalFormat = if (isReallyAlac) "ALAC" else formatLabel(track.mimeType)
        val finalCodec = if (isReallyAlac) "ALAC" else codec
        val finalEncoding = if (isReallyAlac) "ALAC" else encoding

        TrackTechInfo(
            format = finalFormat,
            codec = finalCodec,
            bitrateKbps = bitrate,
            sampleRateHz = sampleRate,
            channels = channels,
            sizeBytes = track.size,
            durationMs = track.durationMs,
            encoding = finalEncoding,
        )
    }

    /** Loads embedded album art bytes for a track (used for Palette + sheet thumb). */
    suspend fun embeddedArt(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Reads embedded (offline) lyrics from a track's tags. Tries a `.lrc` / `.txt`
     * sidecar file next to the audio first (common for synced lyrics), then the
     * container's own metadata. Returns null when nothing is embedded — never hits the
     * network. Strips `[mm:ss.xx]` LRC timestamps so plain text reads cleanly.
     */
    suspend fun embeddedLyrics(track: Track): String? = withContext(Dispatchers.IO) {
        // 1. Sidecar lyrics file (same base name, .lrc or .txt).
        runCatching {
            if (track.data.isNotBlank()) {
                val base = File(track.data)
                val dir = base.parentFile
                val stem = base.nameWithoutExtension
                if (dir != null) {
                    for (ext in listOf("lrc", "txt")) {
                        val f = File(dir, "$stem.$ext")
                        if (f.exists() && f.canRead()) {
                            val text = f.readText()
                            if (text.isNotBlank()) return@withContext stripLrcTimestamps(text)
                        }
                    }
                }
            }
        }

        // 2. Embedded metadata tag. MediaMetadataRetriever has no public lyrics key,
        //    so we probe MediaExtractor track formats for a "lyrics"/"lyric" entry.
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, track.uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                for (key in listOf("lyrics", "lyric", "com.apple.iTunes.LYRICS")) {
                    if (fmt.containsKey(key)) {
                        val v = runCatching { fmt.getString(key) }.getOrNull()
                        if (!v.isNullOrBlank()) return@withContext stripLrcTimestamps(v)
                    }
                }
            }
        } catch (_: Exception) {
            // no embedded lyrics
        } finally {
            runCatching { extractor.release() }
        }
        null
    }

    private fun stripLrcTimestamps(raw: String): String =
        raw.lineSequence()
            .map { it.replace(Regex("""\[\d{1,2}:\d{2}(\.\d{1,3})?]"""), "").trim() }
            .filter { it.isNotEmpty() && !it.matches(Regex("""\[[a-zA-Z]+:.*]""")) }
            .joinToString("\n")
            .trim()

    private fun formatLabel(mime: String): String = when (mime.lowercase()) {
        "audio/mpeg", "audio/mp3" -> "MP3"
        "audio/flac", "audio/x-flac" -> "FLAC"
        "audio/aac" -> "AAC"
        "audio/mp4", "audio/m4a", "audio/x-m4a" -> "M4A"
        "audio/wav", "audio/x-wav" -> "WAV"
        "audio/ogg", "application/ogg" -> "OGG"
        "audio/alac", "audio/x-alac" -> "ALAC"
        else -> mime.substringAfter('/').uppercase()
    }

    /** Decoder/codec label from the actual elementary-stream MIME (MediaExtractor). */
    private fun codecLabel(mime: String): String = when (mime.lowercase()) {
        "audio/mpeg", "audio/mpeg-l3" -> "MPEG Layer III"
        "audio/mp4a-latm", "audio/aac" -> "AAC (MP4A)"
        "audio/flac", "audio/x-flac" -> "FLAC"
        "audio/raw", "audio/x-wav", "audio/wav" -> "PCM"
        "audio/vorbis" -> "Vorbis"
        "audio/opus" -> "Opus"
        "audio/alac", "audio/x-alac" -> "ALAC"
        else -> mime.substringAfter('/').uppercase()
    }
}

data class PlayedTrack(
    val track: Track,
    val playCount: Int,
)

/** Album-art content URI for a given albumId (MediaStore albumart provider). */
fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
