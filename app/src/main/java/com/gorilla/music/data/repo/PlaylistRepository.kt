package com.gorilla.music.data.repo

import com.gorilla.music.data.db.AppDatabase
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.db.PlaylistSummary
import com.gorilla.music.data.db.PlaylistTrackEntity
import com.gorilla.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** CRUD + reorder for playlists, all persisted in Room. */
class PlaylistRepository(private val db: AppDatabase) {

    private val dao = db.playlistDao()

    fun observePlaylists(): Flow<List<PlaylistEntity>> =
        dao.observePlaylists().map { playlists ->
            playlists.filterNot { isFavoritesPlaylistName(it.name) }
        }

    fun observePlaylistSummaries(): Flow<List<PlaylistSummary>> =
        dao.observePlaylistSummaries().map { playlists ->
            playlists.filterNot { isFavoritesPlaylistName(it.name) }
        }

    fun observeTracks(playlistId: Long): Flow<List<Track>> =
        dao.observeTracksIn(playlistId).map { list -> list.map { it.toTrack() } }

    /** [createdAt] supplied by caller (no time access in repo). */
    suspend fun create(name: String, createdAt: Long): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Untitled" }, createdAt = createdAt))
    }

    suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        val pl = dao.getPlaylist(id)
        if (pl?.name == "Favourites") return@withContext
        dao.rename(id, name.trim().ifBlank { "Untitled" })
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val pl = dao.getPlaylist(id)
        if (pl?.name == "Favourites") {
            val tracks = dao.tracksIn(id)
            tracks.forEach { track ->
                db.trackDao().updateFavorite(track.id, false)
            }
        }
        dao.deletePlaylistTracks(id)
        dao.deletePlaylist(id)
    }

    suspend fun addTrack(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val pos = dao.maxPosition(playlistId) + 1
        dao.insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackId, pos))
        val pl = dao.getPlaylist(playlistId)
        if (pl?.name == "Favourites") {
            db.trackDao().updateFavorite(trackId, true)
        }
    }

    suspend fun removeTrack(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.removeTrack(playlistId, trackId)
        // Re-pack positions so ordering stays contiguous.
        repackPositions(playlistId)
        val pl = dao.getPlaylist(playlistId)
        if (pl?.name == "Favourites") {
            db.trackDao().updateFavorite(trackId, false)
        }
    }

    /** Persist a new explicit order (e.g. after drag reorder). */
    suspend fun reorder(playlistId: Long, orderedTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        val rows = orderedTrackIds.mapIndexed { index, trackId ->
            PlaylistTrackEntity(playlistId, trackId, index)
        }
        dao.upsertRows(rows)
    }

    private suspend fun repackPositions(playlistId: Long) {
        val rows = dao.rowsFor(playlistId)
        val repacked = rows.mapIndexed { index, row -> row.copy(position = index) }
        dao.upsertRows(repacked)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.updatePinned(id, isPinned)
    }

    suspend fun movePlaylist(id: Long, direction: Int) = withContext(Dispatchers.IO) {
        val all = dao.getAllPlaylists()
            .filterNot { isFavoritesPlaylistName(it.name) }
            .toMutableList()
        val idx = all.indexOfFirst { it.id == id }
        if (idx == -1) return@withContext
        
        val newIdx = idx + direction
        if (newIdx in all.indices && all[idx].isPinned == all[newIdx].isPinned) {
            val temp = all[idx]
            all[idx] = all[newIdx]
            all[newIdx] = temp
        }
        
        all.forEachIndexed { i, p ->
            dao.updateOrder(p.id, i)
        }
    }
}

internal fun isFavoritesPlaylistName(name: String): Boolean =
    name.equals("Favorites", ignoreCase = true) ||
        name.equals("Favourites", ignoreCase = true)
