package com.gorilla.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.gorilla.music.MainActivity
import com.gorilla.music.GorillaApp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

internal val EqualizerFrequenciesHz = doubleArrayOf(
    31.0,
    62.0,
    125.0,
    250.0,
    500.0,
    1000.0,
    2000.0,
    4000.0,
    8000.0,
    16000.0,
)

/**
 * Foreground [MediaSessionService] hosting the real ExoPlayer. Provides the system
 * MediaSession (lock-screen + notification transport: play/pause/next/prev/seek) and
 * applies the audio settings that must touch the player:
 *
 *  - gapless  → toggles [ExoPlayer.setPauseAtEndOfMediaItems]; gapless decoding is
 *               native, this switches the audible gap between items on/off.
 *  - crossfade→ a real volume ramp near each item boundary (fade out the tail of the
 *               outgoing track, fade in the head of the incoming one).
 *  - loudness → applies a normalization gain ceiling to the player volume.
 *
 * Settings are read from [PlaybackTuning], updated by MediaPlaybackManager.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var effectsAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizerCreationAttempted = false
    private var bassBoostCreationAttempted = false
    private var virtualizerCreationAttempted = false

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val handler = Handler(Looper.getMainLooper())
    private val fadeTick = object : Runnable {
        override fun run() {
            applyCrossfadeVolume()
            handler.postDelayed(this, FADE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Register ContentObserver for real-time library detection
        val repository = (application as GorillaApp).container.musicRepository
        repository.registerContentObserver(serviceScope)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(
                androidx.media3.exoplayer.DefaultRenderersFactory(this).apply {
                    setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                }
            )
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                pauseAtEndOfMediaItems = !PlaybackTuning.gapless
                skipSilenceEnabled = false
                volume = targetVolume()
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) {
                        // Reset volume at each boundary; the fade loop re-ramps as needed.
                        volume = targetVolume()
                        ensureAudioEffects()
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // Re-read tuning whenever playback resumes.
                        pauseAtEndOfMediaItems = !PlaybackTuning.gapless
                        ensureAudioEffects()
                    }
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        releaseAudioEffects()
                        effectsAudioSessionId = audioSessionId
                        ensureAudioEffects()
                    }
                })
            }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        handler.post(fadeTick)
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SetAudioOutputCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != SetAudioOutputCommand.customAction) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            val requestedDeviceId = args.getInt(AudioOutputDeviceIdKey, NoAudioOutputDevice)
            val preferredDevice = if (requestedDeviceId == NoAudioOutputDevice) {
                null
            } else {
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .firstOrNull { it.id == requestedDeviceId }
                    ?: return Futures.immediateFuture(
                        SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE)
                    )
            }

            player.setPreferredAudioDevice(preferredDevice)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** Normalization gain ceiling: when on, cap at ~0.85 to even out hot masters. */
    private fun targetVolume(): Float =
        if (PlaybackTuning.loudnessNormalization) 0.85f else 1.0f

    /**
     * Real crossfade: in the last [crossfadeMs] of a track, ramp volume toward zero;
     * in the first [crossfadeMs] of a track, ramp from zero up to target. Outside the
     * fade windows the volume holds at the (possibly normalized) target.
     */
    private fun applyCrossfadeVolume() {
        ensureAudioEffects()
        val fadeMs = PlaybackTuning.crossfadeMs
        val ceiling = targetVolume()
        if (fadeMs <= 0 || !player.isPlaying) {
            if (player.volume != ceiling) player.volume = ceiling
            return
        }
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0) return

        val intoTrack = pos
        val remaining = dur - pos

        val vol = when {
            intoTrack < fadeMs && player.hasPreviousMediaItem() ->
                ceiling * (intoTrack.toFloat() / fadeMs)
            remaining < fadeMs && player.hasNextMediaItem() ->
                ceiling * (remaining.toFloat() / fadeMs)
            else -> ceiling
        }.coerceIn(0f, 1f)

        if (kotlin.math.abs(player.volume - vol) > 0.01f) {
            player.volume = vol
        }
    }

    private fun ensureAudioEffects() {
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) {
            PlaybackTuning.updateAudioEffectStatus(
                AudioEffectStatus(
                    equalizer = if (PlaybackTuning.equalizerEnabled) {
                        AudioEffectAvailability.WAITING_FOR_PLAYBACK
                    } else {
                        AudioEffectAvailability.DISABLED
                    },
                    bassBoost = if (PlaybackTuning.equalizerEnabled) {
                        AudioEffectAvailability.WAITING_FOR_PLAYBACK
                    } else {
                        AudioEffectAvailability.DISABLED
                    },
                    virtualizer = if (PlaybackTuning.equalizerEnabled) {
                        AudioEffectAvailability.WAITING_FOR_PLAYBACK
                    } else {
                        AudioEffectAvailability.DISABLED
                    },
                )
            )
            return
        }
        if (sessionId != effectsAudioSessionId) {
            releaseAudioEffects()
            effectsAudioSessionId = sessionId
        }
        if (PlaybackTuning.equalizerEnabled) {
            if (equalizer == null && !equalizerCreationAttempted) {
                equalizerCreationAttempted = true
                equalizer = createEffect("Equalizer") { Equalizer(0, sessionId) }
            }
            if (bassBoost == null && !bassBoostCreationAttempted) {
                bassBoostCreationAttempted = true
                bassBoost = createEffect("BassBoost") { BassBoost(0, sessionId) }
            }
            if (virtualizer == null && !virtualizerCreationAttempted) {
                virtualizerCreationAttempted = true
                virtualizer = createEffect("Virtualizer") { Virtualizer(0, sessionId) }
            }
            val equalizerActive = applyEqualizer()
            val bassBoostActive = applyBassBoost()
            val virtualizerActive = applyVirtualizer()
            PlaybackTuning.updateAudioEffectStatus(
                AudioEffectStatus(
                    equalizer = availability(equalizerActive),
                    bassBoost = availability(bassBoostActive),
                    virtualizer = availability(virtualizerActive),
                    hardwareBandCount = runCatching { equalizer?.numberOfBands?.toInt() }.getOrNull(),
                    bassBoostStrengthSupported = runCatching {
                        bassBoost?.strengthSupported == true
                    }.getOrDefault(false),
                    virtualizerStrengthSupported = runCatching {
                        virtualizer?.strengthSupported == true
                    }.getOrDefault(false),
                )
            )
        } else {
            equalizer?.enabled = false
            bassBoost?.enabled = false
            virtualizer?.enabled = false
            equalizerCreationAttempted = false
            bassBoostCreationAttempted = false
            virtualizerCreationAttempted = false
            PlaybackTuning.updateAudioEffectStatus(AudioEffectStatus())
        }
    }

    private inline fun <T> createEffect(name: String, create: () -> T): T? =
        runCatching(create)
            .onFailure { Timber.w(it, "%s is unavailable for the current audio session", name) }
            .getOrNull()

    private fun availability(active: Boolean): AudioEffectAvailability =
        if (active) AudioEffectAvailability.ACTIVE else AudioEffectAvailability.UNAVAILABLE

    private fun applyEqualizer(): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            if (!eq.hasControl()) return@runCatching false
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val gains = PlaybackTuning.equalizerBandGains
            val bandCount = eq.numberOfBands.toInt()
            for (i in 0 until bandCount) {
                val centerHz = eq.getCenterFreq(i.toShort()) / 1000.0
                val curveGain = interpolatedEqualizerGain(centerHz, gains)
                val gainWithPreamp = curveGain + PlaybackTuning.equalizerPreampDb
                val millibels = (gainWithPreamp * 100)
                    .coerceIn(minLevel.toInt(), maxLevel.toInt())
                    .toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }
            eq.enabled = true
            eq.enabled
        }.onFailure {
            Timber.w(it, "Failed to apply equalizer settings")
        }.getOrDefault(false)
    }

    private fun applyBassBoost(): Boolean {
        val boost = bassBoost ?: return false
        return runCatching {
            if (!boost.hasControl()) return@runCatching false
            val strength = (PlaybackTuning.bassBoostStrength * 10).coerceIn(0, 1000).toShort()
            boost.setStrength(strength)
            boost.enabled = PlaybackTuning.bassBoostStrength > 0
            true
        }.onFailure {
            Timber.w(it, "Failed to apply bass boost settings")
        }.getOrDefault(false)
    }

    private fun applyVirtualizer(): Boolean {
        val v = virtualizer ?: return false
        return runCatching {
            if (!v.hasControl()) return@runCatching false
            val strength = (PlaybackTuning.virtualizerStrength * 10).coerceIn(0, 1000).toShort()
            v.setStrength(strength)
            v.enabled = PlaybackTuning.virtualizerStrength > 0
            true
        }.onFailure {
            Timber.w(it, "Failed to apply virtualizer settings")
        }.getOrDefault(false)
    }

    private fun releaseAudioEffects() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
        equalizerCreationAttempted = false
        bassBoostCreationAttempted = false
        virtualizerCreationAttempted = false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop playback + service if the user swipes the app away while paused.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(fadeTick)
        releaseAudioEffects()

        // Unregister ContentObserver
        val repository = (application as GorillaApp).container.musicRepository
        repository.unregisterContentObserver()
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        internal const val AudioOutputDeviceIdKey = "audio_output_device_id"
        internal const val NoAudioOutputDevice = -1
        internal val SetAudioOutputCommand = SessionCommand(
            "com.gorilla.music.SET_AUDIO_OUTPUT",
            Bundle.EMPTY,
        )

        const val FADE_INTERVAL_MS = 100L
    }
}

internal fun interpolatedEqualizerGain(centerHz: Double, gains: List<Int>): Int {
    if (gains.isEmpty()) return 0
    val frequencies = EqualizerFrequenciesHz
    val x = ln(centerHz.coerceAtLeast(frequencies.first()) / frequencies.first())
    val points = frequencies.map { ln(it / frequencies.first()) }

    if (x <= points.first()) return gains.first()
    if (x >= points.last()) return gains.last()

    for (i in 0 until points.lastIndex) {
        val left = points[i]
        val right = points[i + 1]
        if (x in left..right) {
            val t = ((x - left) / (right - left)).coerceIn(0.0, 1.0)
            return (gains[i] + (gains[i + 1] - gains[i]) * t).roundToInt()
        }
    }
    return 0
}
