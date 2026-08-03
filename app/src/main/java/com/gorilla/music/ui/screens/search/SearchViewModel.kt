package com.gorilla.music.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

/** Live search over the full local library, debounced for smooth animated filtering. */
@OptIn(FlowPreview::class)
class SearchViewModel(music: MusicRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val allTracks: StateFlow<List<Track>> = music.tracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val results: StateFlow<List<Track>> =
        combine(music.tracks, _query.debounce(120)) { tracks, q ->
            val needle = q.trim().lowercase()
            if (needle.isEmpty()) emptyList()
            else tracks.filter {
                it.title.lowercase().contains(needle) ||
                    it.displayArtist.lowercase().contains(needle) ||
                    it.displayAlbum.lowercase().contains(needle) ||
                    (it.genre?.lowercase()?.contains(needle) == true)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onQueryChange(q: String) { _query.value = q }
    fun clear() { _query.value = "" }

    companion object {
        val Factory = viewModelFactory { c -> SearchViewModel(c.musicRepository) }
    }
}
