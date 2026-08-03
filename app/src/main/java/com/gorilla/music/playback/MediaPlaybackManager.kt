package com.gorilla.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.data.settings.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-wide singleton owning the [MediaController] bound to [PlaybackService]. All
 * playback (play/pause/seek/queue/shuffle/repeat) goes through here, and the
 * resulting state is published as a [StateFlow] consumed by AppViewModel.
 *
 * The actual ExoPlayer lives in the service (so playback survives in the background);
 * this class is the controller side. Audio settings that must touch the player
 * (crossfade, gapless, loudness) are forwarded to the service via session commands /
 * player config, applied in [PlaybackService].
 */
class MediaPlaybackManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _sleepTimerEndMs = MutableStateFlow<Long?>(null)
    val sleepTimerEndMs: StateFlow<Long?> = _sleepTimerEndMs.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _preferredAudioDeviceId = MutableStateFlow<Int?>(null)
    val preferredAudioDeviceId: StateFlow<Int?> = _preferredAudioDeviceId.asStateFlow()

    /** Full play queue in our domain model, kept in lockstep with the player. */
    private var queue: List<Track> = emptyList()
    private var shuffleEnabled: Boolean = false
    private var autoplayEnabled: Boolean = false
    private var autoplayTracks: List<Track> = emptyList()

    private var onTrackStarted: ((Track) -> Unit)? = null

    fun setOnTrackStarted(cb: (Track) -> Unit) { onTrackStarted = cb }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get()
            controller?.let { attach(it) }
        }, androidx.core.content.ContextCompat.getMainExecutor(appContext))
    }

    private var lyricsRepository: com.gorilla.music.data.repo.LyricsRepository? = null

    fun setLyricsRepository(repo: com.gorilla.music.data.repo.LyricsRepository) {
        this.lyricsRepository = repo
    }

    private fun attach(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                pushState()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                _state.value.current?.let { track ->
                    if (track.id <= 0L) return@let
                    scope.launch(Dispatchers.IO) {
                        lyricsRepository?.prefetchAndCacheLyrics(track)
                    }
                }
            }
        })
        pushState()
        startProgressLoop()
    }

    private fun startProgressLoop() {
        scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration.coerceAtLeast(0),
                    )
                }
                delay(500)
            }
        }
    }

    private fun pushState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        val current = queue.getOrNull(index)
        val repeat = when (c.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        val prev = _state.value
        if (current != null && current.id != prev.current?.id) {
            onTrackStarted?.invoke(current)
        }
        _state.value = prev.copy(
            current = current,
            queue = queue,
            currentIndex = index,
            isPlaying = shouldShowPlaying(c.playWhenReady, c.playbackState),
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            shuffle = shuffleEnabled,
            repeat = repeat,
            autoplay = autoplayEnabled,
        )
    }

    // ---- Queue control ----

    /** Replace the queue and start at [startIndex]. */
    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        autoplayEnabled = false
        autoplayTracks = emptyList()
        val actualStartIndex = startIndex.coerceIn(0, tracks.lastIndex)
        queue = if (shuffleEnabled) {
            tracks.take(actualStartIndex + 1) + tracks.drop(actualStartIndex + 1).shuffled()
        } else {
            tracks
        }
        val items = queue.map { it.toMediaItem() }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, actualStartIndex, 0L)
        c.prepare()
        c.play()
        pushState()
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        if (queue.isEmpty()) {
            playQueue(listOf(track), 0)
            return
        }
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, queue.size)
        queue = queue.toMutableList().apply { add(insertAt, track) }
        c.addMediaItem(insertAt, track.toMediaItem())
        pushState()
    }

    fun addToQueue(track: Track) {
        val c = controller ?: return
        if (queue.isEmpty()) {
            playQueue(listOf(track), 0)
            return
        }
        val autoplayIds = autoplayTracks.mapTo(hashSetOf()) { it.id }
        val insertAt = queue.indexOfFirst { it.id in autoplayIds }.let {
            if (it == -1) queue.size else it
        }
        queue = queue.toMutableList().apply { add(insertAt, track) }
        c.addMediaItem(insertAt, track.toMediaItem())
        pushState()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from !in queue.indices || to !in queue.indices) return
        queue = queue.toMutableList().apply { add(to, removeAt(from)) }
        c.moveMediaItem(from, to)
        pushState()
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in queue.indices) return
        val removed = queue[index]
        queue = queue.toMutableList().apply { removeAt(index) }
        autoplayTracks = autoplayTracks.filterNot { it.id == removed.id }
        c.removeMediaItem(index)
        pushState()
    }

    // ---- Transport ----

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playWhenReady) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }

    fun setPreferredAudioDevice(deviceId: Int?) {
        val c = controller ?: return
        val args = Bundle().apply {
            putInt(
                PlaybackService.AudioOutputDeviceIdKey,
                deviceId ?: PlaybackService.NoAudioOutputDevice,
            )
        }
        val result = c.sendCustomCommand(PlaybackService.SetAudioOutputCommand, args)
        result.addListener({
            val response = runCatching { result.get() }.getOrNull()
            if (response?.resultCode == SessionResult.RESULT_SUCCESS) {
                _preferredAudioDeviceId.value = deviceId
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(appContext))
    }

    fun stop() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
        queue = emptyList()
        autoplayEnabled = false
        autoplayTracks = emptyList()
        _state.value = PlaybackState()
    }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() {
        val c = controller ?: return
        // Restart current if >3s in, otherwise go to previous.
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    /**
     * Fine-grained playback position for word-karaoke lyrics. MediaController
     * extrapolates position between service updates, so reading it per frame
     * is smooth — unlike [PlaybackState.positionMs], which the progress loop
     * only refreshes every 500 ms. Main-thread only (MediaController rule).
     */
    fun currentPositionMs(): Long =
        controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs

    fun skipToQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in queue.indices) return
        c.seekToDefaultPosition(index)
        c.play()
        pushState()
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        scheduleSleepTimer(minutes * 60_000L)
    }

    fun setSleepTimerAtEndOfCurrentTrack() {
        val duration = controller?.duration?.takeIf { it > 0 } ?: _state.value.durationMs
        val position = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
        scheduleSleepTimer((duration - position).coerceAtLeast(1_000L))
    }

    private fun scheduleSleepTimer(delayMs: Long) {
        sleepTimerJob?.cancel()
        _sleepTimerEndMs.value = System.currentTimeMillis() + delayMs
        sleepTimerJob = scope.launch {
            delay(delayMs)
            controller?.pause()
            _sleepTimerEndMs.value = null
            sleepTimerJob = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerEndMs.value = null
    }

    fun toggleShuffle() {
        setShuffle(!shuffleEnabled)
    }

    fun setShuffle(enabled: Boolean) {
        val c = controller ?: return
        if (shuffleEnabled == enabled) return
        shuffleEnabled = enabled
        c.shuffleModeEnabled = false

        if (enabled) {
            val currentIndex = c.currentMediaItemIndex
            val upcoming = queue.drop(currentIndex + 1)
            if (currentIndex in queue.indices && upcoming.size > 1) {
                replaceQueue(
                    newQueue = queue.take(currentIndex + 1) + upcoming.shuffled(),
                    newCurrentIndex = currentIndex,
                )
                return
            }
        }
        pushState()
    }

    fun cycleRepeat() {
        val c = controller ?: return
        val nextMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        if (nextMode != Player.REPEAT_MODE_OFF) clearAutoplay()
        c.repeatMode = nextMode
        pushState()
    }

    fun setRepeat(mode: RepeatMode) {
        val c = controller ?: return
        if (mode != RepeatMode.OFF) clearAutoplay()
        c.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        pushState()
    }

    fun setAutoplay(enabled: Boolean, recommendations: List<Track> = emptyList()) {
        val c = controller ?: return
        if (!enabled) {
            clearAutoplay()
            pushState()
            return
        }

        clearAutoplay()
        autoplayEnabled = true
        c.repeatMode = Player.REPEAT_MODE_OFF
        val queuedIds = queue.mapTo(hashSetOf()) { it.id }
        autoplayTracks = recommendations
            .asSequence()
            .filter { it.id !in queuedIds }
            .distinctBy { it.id }
            .take(AUTOPLAY_QUEUE_SIZE)
            .toList()
        if (autoplayTracks.isNotEmpty()) {
            queue = queue + autoplayTracks
            c.addMediaItems(autoplayTracks.map { it.toMediaItem() })
        }
        pushState()
    }

    private fun clearAutoplay() {
        val c = controller ?: return
        autoplayEnabled = false
        if (autoplayTracks.isEmpty()) return
        val autoplayIds = autoplayTracks.mapTo(hashSetOf()) { it.id }
        val currentIndex = c.currentMediaItemIndex
        for (index in queue.indices.reversed()) {
            if (index != currentIndex && queue[index].id in autoplayIds) {
                queue = queue.toMutableList().apply { removeAt(index) }
                c.removeMediaItem(index)
            }
        }
        autoplayTracks = emptyList()
    }

    private fun replaceQueue(newQueue: List<Track>, newCurrentIndex: Int) {
        val c = controller ?: return
        val positionMs = c.currentPosition.coerceAtLeast(0L)
        val playWhenReady = c.playWhenReady
        queue = newQueue
        c.setMediaItems(
            newQueue.map { it.toMediaItem() },
            newCurrentIndex.coerceIn(0, newQueue.lastIndex),
            positionMs,
        )
        c.prepare()
        c.playWhenReady = playWhenReady
        pushState()
    }

    /**
     * Forwards player-affecting audio settings to the service. Crossfade / gapless /
     * loudness are stored in [PlaybackTuning] and read by the service's player
     * configuration; gapless + skipSilence apply immediately on the controller's player.
     */
    fun applyAudioSettings(
        crossfadeSeconds: Int,
        gapless: Boolean,
        loudnessNormalization: Boolean,
        equalizerEnabled: Boolean,
        equalizerBandGains: List<Int>,
        equalizerPreampDb: Int,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
    ) {
        PlaybackTuning.crossfadeMs = crossfadeSeconds * 1000
        PlaybackTuning.gapless = gapless
        PlaybackTuning.loudnessNormalization = loudnessNormalization
        PlaybackTuning.equalizerEnabled = equalizerEnabled
        PlaybackTuning.equalizerBandGains = equalizerBandGains
            .take(PlaybackTuning.equalizerBandCount)
            .map { it.coerceIn(-12, 12) }
        PlaybackTuning.equalizerPreampDb = equalizerPreampDb.coerceIn(-12, 12)
        PlaybackTuning.bassBoostStrength = bassBoostStrength.coerceIn(0, 100)
        PlaybackTuning.virtualizerStrength = virtualizerStrength.coerceIn(0, 100)
        // Gapless in Media3 = no forced pause between items + skipSilence off; the
        // service applies skipSilence/handleAudioBecomingNoisy on its ExoPlayer. We
        // nudge it through a no-op seek so the service re-reads tuning if needed.
    }

    /** Restore a previously playing track + position without auto-playing. */
    fun prepareResume(track: Track, positionMs: Long) {
        val c = controller ?: return
        queue = listOf(track)
        c.setMediaItem(track.toMediaItem(), positionMs)
        c.prepare()
        c.playWhenReady = false
        pushState()
    }

    private fun Track.toMediaItem(): MediaItem {
        val alacMime = when {
            data.endsWith(".alac", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_ALAC
            mimeType.contains("alac", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_ALAC
            else -> null
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(displayArtist)
            .setAlbumTitle(displayAlbum)
            .setArtworkUri(artworkUri ?: albumArtUri(albumId))
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .apply {
                if (alacMime != null) setMimeType(alacMime)
            }
            .build()
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    companion object {
        private const val AUTOPLAY_QUEUE_SIZE = 25

        internal fun shouldShowPlaying(playWhenReady: Boolean, playbackState: Int): Boolean =
            playWhenReady && playbackState != Player.STATE_ENDED

        @Volatile private var INSTANCE: MediaPlaybackManager? = null
        fun get(context: Context): MediaPlaybackManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaPlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}

/** Mutable tuning shared between the manager (controller) and the service (player). */
object PlaybackTuning {
    const val equalizerBandCount: Int = 10

    private val _audioEffectStatus = MutableStateFlow(AudioEffectStatus())
    val audioEffectStatus: StateFlow<AudioEffectStatus> = _audioEffectStatus.asStateFlow()

    @Volatile var crossfadeMs: Int = 0
    @Volatile var gapless: Boolean = true
    @Volatile var loudnessNormalization: Boolean = false
    @Volatile var equalizerEnabled: Boolean = false
    @Volatile var equalizerBandGains: List<Int> = List(equalizerBandCount) { 0 }
    @Volatile var equalizerPreampDb: Int = 0
    @Volatile var bassBoostStrength: Int = 0
    @Volatile var virtualizerStrength: Int = 0

    internal fun updateAudioEffectStatus(status: AudioEffectStatus) {
        _audioEffectStatus.value = status
    }
}

enum class AudioEffectAvailability {
    DISABLED,
    WAITING_FOR_PLAYBACK,
    ACTIVE,
    UNAVAILABLE,
}

data class AudioEffectStatus(
    val equalizer: AudioEffectAvailability = AudioEffectAvailability.DISABLED,
    val bassBoost: AudioEffectAvailability = AudioEffectAvailability.DISABLED,
    val virtualizer: AudioEffectAvailability = AudioEffectAvailability.DISABLED,
    val hardwareBandCount: Int? = null,
    val bassBoostStrengthSupported: Boolean = false,
    val virtualizerStrengthSupported: Boolean = false,
)
