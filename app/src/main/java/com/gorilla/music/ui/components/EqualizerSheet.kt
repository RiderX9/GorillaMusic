package com.gorilla.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.data.settings.EqualizerBandLabels
import com.gorilla.music.data.settings.EqualizerPreset
import com.gorilla.music.data.settings.EqualizerUiPresets
import com.gorilla.music.data.settings.shortLabel
import com.gorilla.music.playback.AudioEffectAvailability
import com.gorilla.music.playback.AudioEffectStatus
import com.gorilla.music.ui.screens.settings.GlassSwitch
import com.gorilla.music.ui.screens.settings.SettingsViewModel
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.rememberHaptic
import kotlin.math.roundToInt

/**
 * Standalone equalizer sheet, hosted at root next to TrackInfoSheet and EditTagsSheet so it
 * slides up over the nav bar and mini player. Reads and writes the same [SettingsViewModel]
 * state as Settings → Playback, so both surfaces stay in sync and changes reach the audio
 * engine immediately.
 */
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val effectStatus by vm.audioEffectStatus.collectAsStateWithLifecycle()
    val appColors = LocalAppColors.current

    ModalSheetScaffold(
        onDismiss = onDismiss,
        heightFraction = null,
        surfaceColor = appColors.bgSurface,
        plainSurface = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            EqualizerSheetContent(
                settings = settings,
                effectStatus = effectStatus,
                enabled = settings.equalizerEnabled,
                onEnabledChange = vm::setEqualizerEnabled,
                onPreset = vm::setEqualizerPreset,
                onBandGain = vm::setEqualizerBandGain,
                onPreamp = vm::setEqualizerPreampDb,
                onBassBoost = vm::setBassBoostStrength,
                onVirtualizer = vm::setVirtualizerStrength,
                onLoudness = vm::setLoudnessNormalization,
                onReset = vm::resetEqualizerBands,
                onDone = onDismiss,
            )
        }
    }
}

@Composable
private fun EqualizerSheetContent(
    settings: AppSettings,
    effectStatus: AudioEffectStatus,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPreset: (EqualizerPreset) -> Unit,
    onBandGain: (Int, Int) -> Unit,
    onPreamp: (Int) -> Unit,
    onBassBoost: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onLoudness: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val cardShape = RoundedCornerShape(20.dp)

    Column(Modifier.fillMaxWidth()) {
        // ---- Header: title, live preset badge, master toggle ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Equalizer",
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(10.dp))
            PresetBadge(label = settings.equalizerPreset.label, accent = accent, active = enabled)
            Spacer(Modifier.width(6.dp))
            EffectStatusBadge(effectStatus.equalizer)
            Spacer(Modifier.weight(1f))
            GlassSwitch(enabled, onEnabledChange)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderGlass.copy(alpha = 0.4f))
        )

        // Everything below the master switch dims and stops responding when EQ is off.
        Column(Modifier.alpha(if (enabled) 1f else 0.4f)) {
            // ---- 10-band sliders ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(cardShape)
                    .background(Color.Black.copy(alpha = 0.22f))
                    .border(1.dp, colors.borderGlass.copy(alpha = 0.3f), cardShape)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    settings.equalizerBandGains.forEachIndexed { index, gain ->
                        BandSlider(
                            label = EqualizerBandLabels.getOrElse(index) { "${index + 1}" },
                            gain = gain,
                            enabled = enabled,
                            accent = accent,
                            trackColor = colors.textSecondary,
                            labelColor = colors.textSecondary,
                            onChange = { onBandGain(index, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ---- Preset pills ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EqualizerUiPresets.forEach { preset ->
                    PresetPill(
                        label = preset.shortLabel,
                        selected = settings.equalizerPreset == preset,
                        accent = accent,
                        onClick = {
                            if (enabled) {
                                haptic()
                                onPreset(preset)
                            }
                        },
                    )
                }
            }

            // ---- Enhancements ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(cardShape)
                    .background(colors.bgGlass)
                    .border(1.dp, colors.borderGlass.copy(alpha = 0.45f), cardShape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EnhancementRow(
                    label = "Bass Boost",
                    valueText = "${settings.bassBoostStrength}%",
                    fraction = settings.bassBoostStrength / 100f,
                    enabled = enabled && effectStatus.bassBoost == AudioEffectAvailability.ACTIVE,
                    supportText = enhancementSupportText(
                        availability = effectStatus.bassBoost,
                        strengthSupported = effectStatus.bassBoostStrengthSupported,
                    ),
                    accent = accent,
                    onFraction = { onBassBoost((it * 100).roundToInt()) },
                )
                EnhancementRow(
                    label = "Virtualizer",
                    valueText = "${settings.virtualizerStrength}%",
                    fraction = settings.virtualizerStrength / 100f,
                    enabled = enabled && effectStatus.virtualizer == AudioEffectAvailability.ACTIVE,
                    supportText = enhancementSupportText(
                        availability = effectStatus.virtualizer,
                        strengthSupported = effectStatus.virtualizerStrengthSupported,
                    ),
                    accent = accent,
                    onFraction = { onVirtualizer((it * 100).roundToInt()) },
                )
                EnhancementRow(
                    label = "Poweramp",
                    valueText = formatDb(settings.equalizerPreampDb),
                    fraction = (settings.equalizerPreampDb + 12) / 24f,
                    enabled = enabled,
                    accent = accent,
                    onFraction = { onPreamp((it * 24f - 12f).roundToInt()) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Volume ceiling",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (settings.loudnessNormalization) "Output limited to 85%"
                            else "Full output level",
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    GlassSwitch(settings.loudnessNormalization) { if (enabled) onLoudness(it) }
                }
            }
        }

        // ---- Footer ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterPill(
                label = "Reset to flat",
                contentColor = accent,
                background = accent.copy(alpha = 0.14f),
                border = accent.copy(alpha = 0.35f),
                onClick = { haptic(); onReset() },
            )
            Spacer(Modifier.weight(1f))
            FooterPill(
                label = "Done",
                contentColor = colors.textPrimary,
                background = colors.bgGlass,
                border = colors.borderGlass.copy(alpha = 0.55f),
                onClick = { haptic(); onDone() },
            )
        }
    }
}

private fun formatDb(db: Int): String = if (db > 0) "+${db}dB" else "${db}dB"

@Composable
private fun EffectStatusBadge(availability: AudioEffectAvailability) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val (label, tint) = when (availability) {
        AudioEffectAvailability.ACTIVE -> "ACTIVE" to accent
        AudioEffectAvailability.WAITING_FOR_PLAYBACK -> "WAITING" to colors.textSecondary
        AudioEffectAvailability.UNAVAILABLE -> "UNAVAILABLE" to Color(0xFFE05A5A)
        AudioEffectAvailability.DISABLED -> "OFF" to colors.textSecondary
    }
    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

private fun enhancementSupportText(
    availability: AudioEffectAvailability,
    strengthSupported: Boolean,
): String? = when (availability) {
    AudioEffectAvailability.WAITING_FOR_PLAYBACK -> "Waiting for playback"
    AudioEffectAvailability.UNAVAILABLE -> "Unavailable"
    AudioEffectAvailability.ACTIVE -> if (strengthSupported) null else "Device-adjusted"
    AudioEffectAvailability.DISABLED -> null
}

@Composable
private fun PresetBadge(label: String, accent: Color, active: Boolean) {
    val shape = RoundedCornerShape(100.dp)
    val tint = if (active) accent else LocalAppColors.current.textSecondary
    Box(
        modifier = Modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.15f))
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = "$label Profile".uppercase(),
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PresetPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .instantClickable(pressedScale = 0.94f, onClick = onClick)
            .background(if (selected) accent else colors.bgGlass)
            .border(
                1.dp,
                if (selected) accent else colors.borderGlass.copy(alpha = 0.5f),
                shape,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else colors.textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Rounded pill used for both footer actions so they read as a matched pair. */
@Composable
private fun FooterPill(
    label: String,
    contentColor: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CapsuleShape)
            .instantClickable(pressedScale = 0.94f, onClick = onClick)
            .background(background)
            .border(1.dp, border, CapsuleShape)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/** One vertical band. Tap or drag anywhere in the column to set the gain (-12..+12 dB). */
@Composable
private fun BandSlider(
    label: String,
    gain: Int,
    enabled: Boolean,
    accent: Color,
    trackColor: Color,
    labelColor: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = ((gain + 12f) / 24f).coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (gain > 0) "+$gain" else "$gain",
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .width(22.dp)
                .then(
                    if (enabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset -> onChange(gainFromY(offset.y, size.height)) }
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, _ ->
                                    onChange(gainFromY(change.position.y, size.height))
                                    change.consume()
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val trackX = size.width / 2f
                val top = 7.dp.toPx()
                val bottom = size.height - 7.dp.toPx()
                val thumbY = bottom - (bottom - top) * fraction
                drawLine(
                    color = trackColor.copy(alpha = 0.25f),
                    start = Offset(trackX, top),
                    end = Offset(trackX, bottom),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = accent,
                    start = Offset(trackX, bottom),
                    end = Offset(trackX, thumbY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(trackX, thumbY))
            }
        }
        Text(
            text = label,
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun gainFromY(y: Float, height: Int): Int {
    if (height <= 0) return 0
    val f = (1f - (y / height)).coerceIn(0f, 1f)
    return (-12 + f * 24).roundToInt().coerceIn(-12, 12)
}

/** Label + thin horizontal slider + value readout, matching the mockup's enhancement rows. */
@Composable
private fun EnhancementRow(
    label: String,
    valueText: String,
    fraction: Float,
    enabled: Boolean,
    supportText: String? = null,
    accent: Color,
    onFraction: (Float) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(90.dp)) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (supportText != null) {
                Text(
                    text = supportText,
                    color = colors.textSecondary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 10.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .alpha(if (enabled) 1f else 0.42f)
                .then(
                    if (enabled) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    onFraction((offset.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, _ ->
                                    onFraction((change.position.x / size.width).coerceIn(0f, 1f))
                                    change.consume()
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val left = 6.dp.toPx()
                val right = size.width - 6.dp.toPx()
                val thumbX = left + (right - left) * fraction.coerceIn(0f, 1f)
                drawLine(
                    color = colors.textSecondary.copy(alpha = 0.22f),
                    start = Offset(left, centerY),
                    end = Offset(right, centerY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                if (thumbX > left) {
                    drawLine(
                        color = accent,
                        start = Offset(left, centerY),
                        end = Offset(thumbX, centerY),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(color = Color.White, radius = 6.5.dp.toPx(), center = Offset(thumbX, centerY))
            }
        }
        Text(
            text = valueText,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp),
        )
    }
}
