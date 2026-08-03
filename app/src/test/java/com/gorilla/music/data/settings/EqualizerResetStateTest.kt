package com.gorilla.music.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EqualizerResetStateTest {

    @Test
    fun `reset restores every equalizer setting to flat`() {
        val reset = flatEqualizerResetState()

        assertEquals(EqualizerPreset.FLAT, reset.preset)
        assertEquals(EqualizerPreset.FLAT.gains, reset.bands)
        assertEquals(0, reset.preampDb)
        assertEquals(0, reset.bassBoostStrength)
        assertEquals(0, reset.virtualizerStrength)
        assertFalse(reset.loudnessNormalization)
    }
}
