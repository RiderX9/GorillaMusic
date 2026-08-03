package com.gorilla.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.gorilla.music.data.settings.AppSettings

@Composable
fun AudioTab(settings: AppSettings, vm: SettingsViewModel) {
    val context = LocalContext.current
    val output = remember { readAudioOutput(context) }

    Column {
        SettingRow(title = "Output device", subtitle = output.deviceName) {
            Text(output.sampleRate, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        SettingRow(
            title = "High-quality resampling",
            subtitle = "Higher CPU; smoother sample-rate conversion",
        ) {
            GlassSwitch(settings.resamplingHighQuality, vm::setResamplingHighQuality)
        }

        SettingRow(
            title = "Loudness normalization",
            subtitle = "Even out volume across tracks",
        ) {
            GlassSwitch(settings.loudnessNormalization, vm::setLoudnessNormalization)
        }

        SettingRow(
            title = "Gapless playback",
            subtitle = "Remove silence between consecutive tracks",
        ) {
            GlassSwitch(settings.gaplessPlayback, vm::setGaplessPlayback)
        }
    }
}
