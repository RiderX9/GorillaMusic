package com.gorilla.music.playback

import com.gorilla.music.data.settings.EqualizerPreset
import com.gorilla.music.data.settings.EqualizerUiPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerCurveTest {

    @Test
    fun `all presets provide ten safe gain values`() {
        EqualizerPreset.entries.forEach { preset ->
            assertEquals(preset.name, PlaybackTuning.equalizerBandCount, preset.gains.size)
            assertTrue(preset.name, preset.gains.all { it in -12..12 })
        }
    }

    @Test
    fun `shared UI preset list exposes every built-in curve once`() {
        assertEquals(
            EqualizerPreset.entries.filterNot { it == EqualizerPreset.CUSTOM }.toSet(),
            EqualizerUiPresets.toSet(),
        )
        assertEquals(EqualizerUiPresets.size, EqualizerUiPresets.distinct().size)
    }

    @Test
    fun `flat preset remains flat at every hardware center frequency`() {
        EqualizerFrequenciesHz.forEach { frequency ->
            assertEquals(
                0,
                interpolatedEqualizerGain(frequency, EqualizerPreset.FLAT.gains),
            )
        }
    }

    @Test
    fun `interpolation preserves exact configured band centers`() {
        val gains = EqualizerPreset.ROCK.gains

        EqualizerFrequenciesHz.forEachIndexed { index, frequency ->
            assertEquals(gains[index], interpolatedEqualizerGain(frequency, gains))
        }
    }

    @Test
    fun `interpolation blends logarithmically between configured centers`() {
        val gains = listOf(0, 10, 0, 0, 0, 0, 0, 0, 0, 0)
        val geometricMidpoint = kotlin.math.sqrt(31.0 * 62.0)

        assertEquals(5, interpolatedEqualizerGain(geometricMidpoint, gains))
    }

    @Test
    fun `frequencies outside configured range use edge gains`() {
        val gains = EqualizerPreset.TREBLE_BOOST.gains

        assertEquals(gains.first(), interpolatedEqualizerGain(10.0, gains))
        assertEquals(gains.last(), interpolatedEqualizerGain(24_000.0, gains))
    }
}
