package com.gorilla.music.ui.screens.nowplaying

import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.LyricsRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class LyricsState {
    object Loading : LyricsState()
    data class Found(val trackId: Long, val lyrics: String, val isSynced: Boolean) : LyricsState()
    object NotFound : LyricsState()
    object Offline : LyricsState()
}

class LyricsViewModel(private val lyricsRepository: LyricsRepository) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private fun setLyricsNotFound(trackId: Long) {
        val current = _lyricsState.value
        if (current is LyricsState.Found && current.trackId == trackId) {
            // A successful result already exists for this track — do not overwrite it
            return
        }
        _lyricsState.value = LyricsState.NotFound
    }

    fun loadLyrics(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            _lyricsState.value = LyricsState.Loading

            // 0. Custom lyrics from tag editor
            if (!track.customLyrics.isNullOrBlank()) {
                _lyricsState.value = LyricsState.Found(track.id, track.customLyrics, track.isSynced)
                return@launch
            }

            // 1. Check Room cache first — instant
            val cached = lyricsRepository.getCachedLyrics(track.id)
            if (cached != null && cached.lyrics.isNotBlank()) {
                _lyricsState.value = LyricsState.Found(track.id, cached.lyrics, cached.isSynced)
                return@launch
            } else if (cached != null) {
                // Bad cache entry — delete it and re-fetch
                lyricsRepository.deleteCachedLyrics(track.id)
            }

            // Not in cache yet — fetch now
            fetchLyrics(track)
        }
    }

    private suspend fun fetchLyrics(track: Track) {
        try {
            // Share the exact same fetch guard as the prefetch trigger
            lyricsRepository.prefetchAndCacheLyrics(track)
            
            // The guard has returned (either fetched successfully, failed, or was already cached)
            // Now safely read the final result from the cache
            val cached = lyricsRepository.getCachedLyrics(track.id)
            if (cached != null && cached.lyrics.isNotBlank()) {
                _lyricsState.value = LyricsState.Found(track.id, cached.lyrics, cached.isSynced)
            } else if (!lyricsRepository.isNetworkAvailable()) {
                _lyricsState.value = LyricsState.Offline
            } else {
                setLyricsNotFound(track.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("LYRICS FETCH FAILED: ${e.javaClass.simpleName}: ${e.message}")
            android.util.Log.e("Lyrics", "Exception in fetchLyrics", e)
            setLyricsNotFound(track.id)
        }
    }

    companion object {
        val Factory = viewModelFactory { c -> LyricsViewModel(c.lyricsRepository) }
    }
}
