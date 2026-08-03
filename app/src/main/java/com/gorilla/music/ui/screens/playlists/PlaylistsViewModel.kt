package com.gorilla.music.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.data.repo.PlaylistRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PlaylistsViewModel(
    private val playlists: PlaylistRepository,
    private val music: MusicRepository,
) : ViewModel() {

    val allPlaylists: StateFlow<List<PlaylistEntity>> = playlists.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTracks: StateFlow<List<Track>> = music.tracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedId = MutableStateFlow<Long?>(null)
    val selectedId: StateFlow<Long?> = _selectedId.asStateFlow()

    val selectedTracks: StateFlow<List<Track>?> = _selectedId
        // Keep the outgoing playlist's tracks alive while its exit animation completes.
        .filterNotNull()
        .flatMapLatest(playlists::observeTracks)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun observeTracks(id: Long) = playlists.observeTracks(id)

    fun open(id: Long) { _selectedId.value = id }
    fun closeDetail() { _selectedId.value = null }

    fun create(name: String) {
        viewModelScope.launch { playlists.create(name, System.currentTimeMillis()) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { playlists.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (_selectedId.value == id) _selectedId.value = null
            playlists.delete(id)
        }
    }

    fun addTrack(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlists.addTrack(playlistId, trackId) }
    }

    fun removeTrack(playlistId: Long, trackId: Long) {
        viewModelScope.launch { playlists.removeTrack(playlistId, trackId) }
    }

    fun reorder(playlistId: Long, orderedIds: List<Long>) {
        viewModelScope.launch { playlists.reorder(playlistId, orderedIds) }
    }

    companion object {
        val Factory = viewModelFactory { c -> PlaylistsViewModel(c.playlistRepository, c.musicRepository) }
    }
}
