package com.gorilla.music.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.model.TrackTechInfo
import com.gorilla.music.data.repo.MusicRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads detailed technical metadata for the Track Info sheet on demand. */
class TrackInfoViewModel(private val music: MusicRepository) : ViewModel() {

    private val _info = MutableStateFlow<TrackTechInfo?>(null)
    val info: StateFlow<TrackTechInfo?> = _info.asStateFlow()

    private var loadedFor: Long = -1

    fun load(track: Track) {
        if (loadedFor == track.id && _info.value != null) return
        loadedFor = track.id
        _info.value = null
        viewModelScope.launch {
            _info.value = music.techInfo(track)
        }
    }

    companion object {
        val Factory = viewModelFactory { c -> TrackInfoViewModel(c.musicRepository) }
    }
}
