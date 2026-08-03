package com.gorilla.music.ui.screens.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleLyricsTimingTest {

    @Test
    fun inactiveLinesNeverReceiveKaraokeProgress() {
        assertEquals(0f, syncedWordProgress(false, 10_000L, 0L, 500L), 0f)
    }

    @Test
    fun activeWordProgressUsesTheCurrentPlaybackFrame() {
        assertEquals(0f, syncedWordProgress(true, 99L, 100L, 300L), 0f)
        assertEquals(0.5f, syncedWordProgress(true, 200L, 100L, 300L), 0.001f)
        assertEquals(1f, syncedWordProgress(true, 300L, 100L, 300L), 0f)
    }
}
