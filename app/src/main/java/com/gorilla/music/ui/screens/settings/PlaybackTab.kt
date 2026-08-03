package com.gorilla.music.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.data.settings.EqualizerBandLabels
import com.gorilla.music.data.settings.EqualizerPreset
import com.gorilla.music.data.settings.EqualizerUiPresets
import com.gorilla.music.data.settings.RepeatMode
import com.gorilla.music.data.settings.shortLabel
import com.gorilla.music.playback.AudioEffectAvailability
import com.gorilla.music.playback.AudioEffectStatus
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import kotlin.math.roundToInt

@Composable
fun PlaybackTab(settings: AppSettings, vm: SettingsViewModel) {
    val effectStatus by vm.audioEffectStatus.collectAsStateWithLifecycle()

    Column {
        SettingRow(
            title = "Equalizer",
            subtitle = if (settings.equalizerEnabled) "Custom tone shaping enabled" else "10-band EQ, presets, bass boost and spatializer",
        ) {
            GlassSwitch(settings.equalizerEnabled, vm::setEqualizerEnabled)
        }

        if (settings.equalizerEnabled) {
            EqualizerPanel(settings = settings, effectStatus = effectStatus, vm = vm)
        }

        SettingRow(
            title = "Shuffle by default",
            subtitle = "Start new queues shuffled",
        ) {
            GlassSwitch(settings.defaultShuffle, vm::setDefaultShuffle)
        }

        SettingBlock(
            title = "Crossfade",
            subtitle = "${settings.crossfadeSeconds}s - fade between tracks",
        ) {
            Column {
                GlassSlider(
                    value = settings.crossfadeSeconds.toFloat(),
                    valueRange = 0f..10f,
                    steps = 9,
                    onValueChange = { vm.setCrossfadeSeconds(it.toInt()) },
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("0s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("10s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingBlock(title = "Default repeat mode") {
            SegmentedControl(
                options = listOf(
                    RepeatMode.OFF to "Off",
                    RepeatMode.ALL to "All",
                    RepeatMode.ONE to "One",
                ),
                selected = settings.defaultRepeat,
                onSelect = vm::setDefaultRepeat,
            )
        }

        SettingRow(
            title = "Resume on open",
            subtitle = "Reload the last track and position",
        ) {
            GlassSwitch(settings.resumeOnOpen, vm::setResumeOnOpen)
        }

        SettingRow(
            title = "Lock screen artwork",
            subtitle = "Show album art on the lock screen",
        ) {
            GlassSwitch(settings.lockScreenArtwork, vm::setLockScreenArtwork)
        }
    }
}

@Composable
private fun EqualizerPanel(
    settings: AppSettings,
    effectStatus: AudioEffectStatus,
    vm: SettingsViewModel,
) {
    val bassBoostEnabled = effectStatus.bassBoost == AudioEffectAvailability.ACTIVE
    val virtualizerEnabled = effectStatus.virtualizer == AudioEffectAvailability.ACTIVE

    Column {
        SettingBlock(
            title = "Equalizer",
            subtitle = settings.equalizerPreset.label,
            headerControl = {
                AudioEffectStatusPill(effectStatus.equalizer)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(184.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    settings.equalizerBandGains.forEachIndexed { index, gain ->
                        EqualizerBandControl(
                            label = EqualizerBandLabels.getOrElse(index) { "${index + 1}" },
                            gain = gain,
                            onChange = { vm.setEqualizerBandGain(index, it) },
                        )
                    }
                }

                EqualizerUiPresets.chunked(3).forEach { presets ->
                    SegmentedControl(
                        options = presets.map { it to it.shortLabel },
                        selected = settings.equalizerPreset,
                        onSelect = vm::setEqualizerPreset,
                    )
                }
            }
        }

        SettingBlock(
            title = "Enhancements",
            subtitle = "Bass, volume ceiling and spatial width",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EqualizerSliderRow(
                    label = "Bass Boost",
                    value = settings.bassBoostStrength,
                    range = 0..100,
                    suffix = "%",
                    enabled = bassBoostEnabled,
                    statusText = enhancementStatusText(
                        availability = effectStatus.bassBoost,
                        strengthSupported = effectStatus.bassBoostStrengthSupported,
                    ),
                    onChange = vm::setBassBoostStrength,
                )
                EqualizerSliderRow(
                    label = "Virtualizer",
                    value = settings.virtualizerStrength,
                    range = 0..100,
                    suffix = "%",
                    enabled = virtualizerEnabled,
                    statusText = enhancementStatusText(
                        availability = effectStatus.virtualizer,
                        strengthSupported = effectStatus.virtualizerStrengthSupported,
                    ),
                    onChange = vm::setVirtualizerStrength,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Volume ceiling",
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalAppColors.current.textPrimary,
                        )
                        Text(
                            if (settings.loudnessNormalization) "Output limited to 85%" else "Full output level",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAppColors.current.textSecondary,
                        )
                    }
                    GlassSwitch(settings.loudnessNormalization, vm::setLoudnessNormalization)
                }
            }
        }

        SettingBlock(
            title = "Poweramp",
            subtitle = "${settings.equalizerPreampDb} dB across EQ bands",
        ) {
            GlassSlider(
                value = settings.equalizerPreampDb.toFloat(),
                valueRange = -12f..12f,
                steps = 23,
                onValueChange = { vm.setEqualizerPreampDb(it.roundToInt()) },
            )
        }

        SettingActionButton(label = "Reset to flat", onClick = vm::resetEqualizerBands)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AudioEffectStatusPill(availability: AudioEffectAvailability) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val (label, color) = when (availability) {
        AudioEffectAvailability.ACTIVE -> "Active" to accent
        AudioEffectAvailability.WAITING_FOR_PLAYBACK -> "Waiting" to appColors.textSecondary
        AudioEffectAvailability.UNAVAILABLE -> "Unavailable" to MaterialTheme.colorScheme.error
        AudioEffectAvailability.DISABLED -> "Off" to appColors.textSecondary
    }
    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun enhancementStatusText(
    availability: AudioEffectAvailability,
    strengthSupported: Boolean,
): String? = when (availability) {
    AudioEffectAvailability.WAITING_FOR_PLAYBACK -> "Waiting for playback"
    AudioEffectAvailability.UNAVAILABLE -> "Unavailable on this device or output"
    AudioEffectAvailability.ACTIVE -> if (strengthSupported) null else "Device-adjusted strength"
    AudioEffectAvailability.DISABLED -> null
}

@Composable
private fun EqualizerBandControl(
    label: String,
    gain: Int,
    onChange: (Int) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val fraction = ((gain + 12f) / 24f).coerceIn(0f, 1f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(28.dp),
    ) {
        Text(
            if (gain > 0) "+$gain" else "$gain",
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textSecondary,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(118.dp)
                .pointerInput(Unit) {
                    fun valueFromY(y: Float): Int {
                        val f = (1f - (y / size.height)).coerceIn(0f, 1f)
                        return (-12 + f * 24).roundToInt().coerceIn(-12, 12)
                    }
                    detectTapGestures { offset -> onChange(valueFromY(offset.y)) }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        val next = ((1f - (change.position.y / size.height)).coerceIn(0f, 1f) * 24 - 12)
                            .roundToInt()
                            .coerceIn(-12, 12)
                        onChange(next)
                        change.consume()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val trackX = size.width / 2f
                val top = 4.dp.toPx()
                val bottom = size.height - 4.dp.toPx()
                val thumbY = bottom - (bottom - top) * fraction
                drawLine(
                    color = appColors.textSecondary.copy(alpha = 0.28f),
                    start = Offset(trackX, top),
                    end = Offset(trackX, bottom),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = accent.copy(alpha = 0.82f),
                    start = Offset(trackX, bottom),
                    end = Offset(trackX, thumbY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = accent,
                    radius = 7.dp.toPx(),
                    center = Offset(trackX, thumbY),
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textSecondary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EqualizerSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    enabled: Boolean = true,
    statusText: String? = null,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.48f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(0.42f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = LocalAppColors.current.textPrimary,
                maxLines = 1,
            )
            Text(
                statusText ?: "$value$suffix",
                style = MaterialTheme.typography.labelMedium,
                color = LocalAppColors.current.textSecondary,
                maxLines = 2,
            )
        }
        GlassSlider(
            value = value.toFloat(),
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            enabled = enabled,
            onValueChange = { onChange(it.roundToInt().coerceIn(range.first, range.last)) },
            modifier = Modifier.weight(1f),
        )
    }
}
