package com.gorilla.music.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun clear()

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAll(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: Long): TrackEntity?

    @Query("UPDATE tracks SET overrideTitle = :overrideTitle, overrideArtist = :overrideArtist, overrideAlbum = :overrideAlbum, overrideGenre = :overrideGenre, overrideYear = :overrideYear, customLyrics = :customLyrics, isSynced = :isSynced WHERE id = :id")
    suspend fun updateOverrides(
        id: Long,
        overrideTitle: String?,
        overrideArtist: String?,
        overrideAlbum: String?,
        overrideGenre: String?,
        overrideYear: Int?,
        customLyrics: String?,
        isSynced: Boolean,
    )

    @Query("UPDATE tracks SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)

    @Query("SELECT isFavorite FROM tracks WHERE id = :id")
    suspend fun isFavorite(id: Long): Boolean?

    /** Map of trackId -> isFavorite for the current cache, used to preserve favorites across a rescan. */
    @Query("SELECT id, isFavorite FROM tracks WHERE isFavorite != 0")
    suspend fun favoriteIds(): List<FavoriteFlag>

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT id, dateAddedSec FROM tracks")
    suspend fun pathStamps(): List<TrackStamp>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}

/** Lightweight projection of a cached track's id + last-known modification stamp. */
data class TrackStamp(val id: Long, val dateAddedSec: Long)

/** Projection of a track id and its favorite flag. */
data class FavoriteFlag(val id: Long, val isFavorite: Boolean)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun getCachedLyrics(trackId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(entity: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackId = :trackId")
    suspend fun deleteCachedLyrics(trackId: Long)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): PlaylistEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId)")
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun deletePlaylistTracks(id: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query(
        """
        SELECT p.id AS id,
               p.name AS name,
               p.isPinned AS isPinned,
               p.displayOrder AS displayOrder,
               COUNT(pt.trackId) AS trackCount,
               (
                   SELECT pt2.trackId
                   FROM playlist_tracks pt2
                   WHERE pt2.playlistId = p.id
                   ORDER BY pt2.position ASC
                   LIMIT 1
               ) AS firstTrackId
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.isPinned DESC, p.displayOrder ASC, p.createdAt DESC
        """
    )
    fun observePlaylistSummaries(): Flow<List<PlaylistSummary>>

    @Query("UPDATE playlists SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE playlists SET displayOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, displayOrder ASC, createdAt DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(row: PlaylistTrackEntity)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRows(rows: List<PlaylistTrackEntity>)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun rowsFor(playlistId: Long): List<PlaylistTrackEntity>

    /** Tracks in a playlist, ordered by position, joined against the track cache. */
    @Transaction
    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    fun observeTracksIn(playlistId: Long): Flow<List<TrackEntity>>

    @Transaction
    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    suspend fun tracksIn(playlistId: Long): List<TrackEntity>
}

@Dao
interface RecentlyPlayedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: RecentlyPlayedEntity)

    @Query("SELECT * FROM recently_played WHERE trackId = :trackId")
    suspend fun get(trackId: Long): RecentlyPlayedEntity?

    /** Recently played tracks, most recent first, joined to the cache. */
    @Transaction
    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN recently_played rp ON rp.trackId = t.id
        ORDER BY rp.lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TrackEntity>>

    /** Most-played tracks with their persisted play counts. */
    @Transaction
    @Query(
        """
        SELECT t.*, rp.playCount AS playCount FROM tracks t
        INNER JOIN recently_played rp ON rp.trackId = t.id
        ORDER BY rp.playCount DESC, rp.lastPlayedAt DESC
        LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int): Flow<List<MostPlayedTrack>>

}

data class MostPlayedTrack(
    @Embedded val track: TrackEntity,
    val playCount: Int,
)
