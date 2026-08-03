package com.gorilla.music.playback

import com.gorilla.music.data.model.Track
import com.gorilla.music.data.settings.RepeatMode

/** Observable snapshot of the playback engine, surfaced to the whole app. */
data class PlaybackState(
    val current: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val autoplay: Boolean = false,
) {
    val hasTrack: Boolean get() = current != null
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
