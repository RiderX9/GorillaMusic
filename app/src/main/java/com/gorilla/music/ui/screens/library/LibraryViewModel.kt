package com.gorilla.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.AppContainer
import com.gorilla.music.data.model.Album
import com.gorilla.music.data.model.Artist
import com.gorilla.music.data.model.Folder
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab { SONGS, ARTISTS, ALBUMS, FOLDERS, FAVORITES }
enum class SortOrder { TITLE_ASC, TITLE_DESC, ARTIST_ASC, DATE_ADDED_DESC, DURATION_DESC }

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.SONGS,
    val sort: SortOrder = SortOrder.TITLE_ASC,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
)

sealed interface LibraryScreenUiState {
    object Loading : LibraryScreenUiState
    object Empty : LibraryScreenUiState
    data class Success(
        val tab: LibraryTab,
        val sort: SortOrder,
        val albums: List<Album>,
        val artists: List<Artist>,
        val folders: List<Folder>,
        val songs: List<Track>
    ) : LibraryScreenUiState
}

class LibraryViewModel(private val music: MusicRepository) : ViewModel() {

    private val _ui = MutableStateFlow(LibraryUiState())
    val ui: StateFlow<LibraryUiState> = _ui.asStateFlow()

    val uiState: StateFlow<LibraryScreenUiState> = combine(
        music.tracks,
        _ui
    ) { tracks, ui ->
        if (tracks.isEmpty()) {
            LibraryScreenUiState.Empty
        } else {
            LibraryScreenUiState.Success(
                tab = ui.tab,
                sort = ui.sort,
                albums = ui.albums,
                artists = ui.artists,
                folders = ui.folders,
                songs = sortTracks(tracks, ui.sort)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryScreenUiState.Loading)

    val songs: StateFlow<List<Track>> = combine(music.tracks, _ui) { tracks, ui ->
        sortTracks(tracks, ui.sort)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshGroups()
        // Re-derive groupings whenever the track cache changes.
        viewModelScope.launch {
            music.tracks.collect { refreshGroups() }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _ui.value = _ui.value.copy(tab = tab)
    }

    fun setSort(sort: SortOrder) {
        _ui.value = _ui.value.copy(sort = sort)
    }

    private fun refreshGroups() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                albums = music.albums(),
                artists = music.artists(),
                folders = music.folders(),
            )
        }
    }

    private fun sortTracks(tracks: List<Track>, sort: SortOrder): List<Track> = when (sort) {
        SortOrder.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
        SortOrder.ARTIST_ASC -> tracks.sortedBy { it.displayArtist.lowercase() }
        SortOrder.DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAddedSec }
        SortOrder.DURATION_DESC -> tracks.sortedByDescending { it.durationMs }
    }

    companion object {
        val Factory = viewModelFactory { c -> LibraryViewModel(c.musicRepository) }
    }
}
