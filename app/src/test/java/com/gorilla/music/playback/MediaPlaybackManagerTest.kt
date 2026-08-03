package com.gorilla.music.playback

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackManagerTest {

    @Test
    fun `playing state remains active while next track buffers`() {
        assertTrue(
            MediaPlaybackManager.shouldShowPlaying(
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING,
            )
        )
    }

    @Test
    fun `playing state is inactive after user pauses`() {
        assertFalse(
            MediaPlaybackManager.shouldShowPlaying(
                playWhenReady = false,
                playbackState = Player.STATE_READY,
            )
        )
    }

    @Test
    fun `playing state is inactive when queue reaches the end`() {
        assertFalse(
            MediaPlaybackManager.shouldShowPlaying(
                playWhenReady = true,
                playbackState = Player.STATE_ENDED,
            )
        )
    }
}
