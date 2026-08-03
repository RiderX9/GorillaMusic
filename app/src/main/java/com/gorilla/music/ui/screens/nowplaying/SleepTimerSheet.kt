package com.gorilla.music.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.ModalSheetScaffold
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.rememberHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Theme-aware sleep timer bottom sheet styled after the sleep timer spec.
 * Uses LocalAppColors and LocalDynamicColors for full theme parity with
 * TrackInfoSheet and TrackContextSheet across Light, Dark, and AMOLED modes,
 * complete with touch press-scaling and accent bloom animations.
 */
@Composable
fun SleepTimerSheet(
    app: AppViewModel,
    onDismiss: () -> Unit,
) {
    val haptic = rememberHaptic()
    val timerEnd by app.playback.sleepTimerEndMs.collectAsStateWithLifecycle()

    val initialMinutes = remember {
        timerEnd
            ?.let { ((it - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000f).roundToInt() }
            ?.coerceIn(5, 120)
            ?: 30
    }
    var selectedMinutes by remember { mutableFloatStateOf(initialMinutes.toFloat()) }
    var endOfSong by remember { mutableStateOf(false) }

    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    // Inner element background and border tokens matching app sheet standards
    val cardBg = appColors.bgGlass
    val cardBorder = appColors.borderGlass.copy(alpha = 0.5f)

    ModalSheetScaffold(
        onDismiss = onDismiss,
        heightFraction = null,
        surfaceColor = appColors.bgSurface,
        plainSurface = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            // 1. Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Sleep Timer",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appColors.textPrimary,
                            letterSpacing = (-0.2).sp,
                        )
                        Text(
                            text = "Stop playback automatically",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .instantClickable(pressedScale = 0.92f) {
                            onDismiss()
                        }
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // 2. Primary Time Readout Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Top subtle accent glow line
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(140.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    accent.copy(alpha = if (appColors.isDark) 0.6f else 0.45f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (endOfSong) {
                        Text(
                            text = "End of Track",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = appColors.textPrimary,
                            letterSpacing = (-0.5).sp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "${selectedMinutes.roundToInt()}",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = appColors.textPrimary,
                                letterSpacing = (-1).sp,
                            )
                            Text(
                                text = "MIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val targetTimeFormatted = remember(selectedMinutes, endOfSong, timerEnd) {
                        if (endOfSong) {
                            "Playback stops when current track finishes"
                        } else {
                            val targetMs = System.currentTimeMillis() + (selectedMinutes.roundToInt() * 60_000L)
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(targetMs))
                            "Playback turns off at $timeStr"
                        }
                    }

                    Text(
                        text = targetTimeFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 3. Precise Time Scrubber Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Slider(
                        value = selectedMinutes,
                        onValueChange = {
                            selectedMinutes = (it / 5f).roundToInt().times(5).toFloat()
                            if (endOfSong) endOfSong = false
                        },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = if (appColors.isDark) Color.White else accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = appColors.textPrimary.copy(alpha = 0.12f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("5 MIN", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Text("60 MIN", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Text("120 MIN", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. Preset Duration Pills
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                    .padding(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val presets = listOf(15, 30, 45, 60)
                    presets.forEach { mins ->
                        val isSelected = !endOfSong && selectedMinutes.roundToInt() == mins
                        val pillShape = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .instantClickable(pressedScale = 0.93f) {
                                    selectedMinutes = mins.toFloat()
                                    endOfSong = false
                                }
                                .clip(pillShape)
                                .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = pillShape,
                                )
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) accent else appColors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 5. End of Current Track Toggle Card
            val toggleCardShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .instantClickable(pressedScale = 0.97f) {
                        endOfSong = !endOfSong
                    }
                    .clip(toggleCardShape)
                    .background(cardBg)
                    .border(
                        width = 1.dp,
                        color = if (endOfSong) accent.copy(alpha = 0.4f) else cardBorder,
                        shape = toggleCardShape,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (endOfSong) accent.copy(alpha = 0.15f) else appColors.textPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = null,
                                tint = if (endOfSong) accent else appColors.textPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "End of Current Track",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary,
                            )
                            Text(
                                text = "Finish active song before stopping",
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                            )
                        }
                    }

                    Switch(
                        checked = endOfSong,
                        onCheckedChange = {
                            endOfSong = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent,
                            uncheckedThumbColor = appColors.textSecondary,
                            uncheckedTrackColor = appColors.textPrimary.copy(alpha = 0.15f),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6. Action Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val isTimerActive = timerEnd != null
                val actionShape = RoundedCornerShape(20.dp)

                // Reset / Turn Off button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .instantClickable(pressedScale = 0.94f) {
                            app.playback.cancelSleepTimer()
                            selectedMinutes = 30f
                            endOfSong = false
                        }
                        .clip(actionShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, actionShape)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isTimerActive) "Turn Off" else "Reset",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appColors.textPrimary,
                    )
                }

                // Set Timer button
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .accentBloom(accent, active = true, shape = actionShape)
                        .instantClickable(pressedScale = 0.94f) {
                            if (endOfSong) {
                                app.playback.setSleepTimerAtEndOfCurrentTrack()
                            } else {
                                app.playback.setSleepTimer(selectedMinutes.roundToInt())
                            }
                            onDismiss()
                        }
                        .clip(actionShape)
                        .background(accent)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isTimerActive) "Update Timer" else "Set Timer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
