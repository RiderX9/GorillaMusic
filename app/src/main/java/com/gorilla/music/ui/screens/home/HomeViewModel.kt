package com.gorilla.music.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.db.PlaylistSummary
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.data.repo.PlaylistRepository
import com.gorilla.music.data.repo.PlayedTrack
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    object Loading : HomeUiState
    object Empty : HomeUiState
    data class Success(
        val tracks: List<Track>,
        val recent: List<Track>,
        val mostPlayed: List<PlayedTrack>,
        val playlists: List<PlaylistSummary>,
    ) : HomeUiState
}

class HomeViewModel(
    private val music: MusicRepository,
    private val playlists: PlaylistRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        music.tracks,
        music.recentlyPlayed(20),
        music.mostPlayed(20),
        playlists.observePlaylistSummaries(),
    ) { tracks, recent, mostPlayed, playlistSummaries ->
        if (tracks.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Success(tracks, recent, mostPlayed, playlistSummaries)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState.Loading)

    companion object {
        val Factory = viewModelFactory { c -> HomeViewModel(c.musicRepository, c.playlistRepository) }
    }
}
