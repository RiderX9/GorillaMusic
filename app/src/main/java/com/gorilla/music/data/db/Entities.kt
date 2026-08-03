package com.gorilla.music.data.db

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gorilla.music.data.model.Track

/** Cached track metadata so the library doesn't rescan on every launch. */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uriString: String,
    val data: String,
    val folder: String,
    val mimeType: String,
    val size: Long,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val isFavorite: Boolean = false,
    val overrideTitle: String? = null,
    val overrideArtist: String? = null,
    val overrideAlbum: String? = null,
    val overrideGenre: String? = null,
    val overrideYear: Int? = null,
    val customLyrics: String? = null,
    val isSynced: Boolean = false,
) {
    fun toTrack(): Track = Track(
        id = id,
        title = overrideTitle ?: title,
        artist = overrideArtist ?: artist,
        album = overrideAlbum ?: album,
        albumId = albumId,
        durationMs = durationMs,
        uri = Uri.parse(uriString),
        data = data,
        folder = folder,
        mimeType = mimeType,
        size = size,
        dateAddedSec = dateAddedSec,
        trackNumber = trackNumber,
        year = overrideYear ?: year,
        isFavorite = isFavorite,
        genre = overrideGenre,
        customLyrics = customLyrics,
        isSynced = isSynced,
        overrideTitle = overrideTitle,
        overrideArtist = overrideArtist,
        overrideAlbum = overrideAlbum,
        overrideYear = overrideYear,
    )

    companion object {
        fun from(t: Track) = TrackEntity(
            id = t.id,
            title = t.title,
            artist = t.artist,
            album = t.album,
            albumId = t.albumId,
            durationMs = t.durationMs,
            uriString = t.uri.toString(),
            data = t.data,
            folder = t.folder,
            mimeType = t.mimeType,
            size = t.size,
            dateAddedSec = t.dateAddedSec,
            trackNumber = t.trackNumber,
            year = t.year,
            isFavorite = t.isFavorite,
            overrideTitle = t.overrideTitle,
            overrideArtist = t.overrideArtist,
            overrideAlbum = t.overrideAlbum,
            overrideGenre = t.genre,
            overrideYear = t.overrideYear,
            customLyrics = t.customLyrics,
            isSynced = t.isSynced,
        )
    }
}

/**
 * Cached lyrics for a track (embedded or fetched from LRCLIB). Stored keyed by trackId
 * so the Lyrics sheet never re-fetches once a track has been resolved. [plain] is the
 * display text; [synced] holds raw LRC (`[mm:ss.xx]` lines) for a future karaoke view.
 * [source] records where it came from; [notFound] caches a confirmed miss so we don't
 * keep hammering the network for a track that genuinely has no lyrics anywhere.
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: Long,
    val lyrics: String,
    val isSynced: Boolean,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isPinned: Boolean = false,
    val displayOrder: Int = 0,
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val firstTrackId: Long?,
    val isPinned: Boolean = false,
    val displayOrder: Int = 0,
)

/** Join row linking a track to a playlist with an explicit ordering position. */
@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

/** Records a play event for the "recently played" home row. */
@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val trackId: Long,
    val lastPlayedAt: Long,
    val playCount: Int,
)
