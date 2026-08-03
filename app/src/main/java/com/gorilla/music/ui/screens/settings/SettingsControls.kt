package com.gorilla.music.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.gorilla.music.data.settings.AccentChoice
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.SpringSpecs
import com.gorilla.music.ui.theme.ThemeMode
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic

/** Labelled setting row wrapped in a card matching Settings.html. */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    control: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, cardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = title,
                    color = appColors.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = appColors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            control()
        }
    }
}

/** Stacked setting block with full-width control matching Settings.html. */
@Composable
fun SettingBlock(
    title: String,
    subtitle: String? = null,
    headerControl: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, cardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = appColors.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                if (headerControl != null) {
                    Spacer(Modifier.size(12.dp))
                    headerControl()
                }
            }
            Box(Modifier.padding(top = 14.dp)) {
                content()
            }
        }
    }
}

/** Full-width settings action using the same surface treatment as the other settings rows. */
@Composable
fun SettingActionButton(
    label: String,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .pressScale(interaction)
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, cardShape)
            .clickable(interaction, indication = null) {
                haptic()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Custom iOS-style on/off switch matching Settings.html. */
@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 2.dp,
        animationSpec = SpringSpecs.DpSpring,
        label = "switchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) accent else appColors.bgGlass,
        animationSpec = SpringSpecs.ColorSpring,
        label = "switchTrack",
    )

    Box(
        modifier = Modifier
            .accentBloom(accent, active = checked, shape = CircleShape)
            .size(width = 50.dp, height = 30.dp)
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                1.dp,
                if (checked) accent else appColors.borderGlass,
                CircleShape,
            )
            .clickable(interaction, indication = null) {
                haptic()
                onChange(!checked)
            }
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Segmented control single-line track matching Settings.html. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val trackShape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(appColors.bgGlass)
            .border(1.dp, appColors.borderGlass, trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            val interaction = remember { MutableInteractionSource() }
            val optionShape = RoundedCornerShape(18.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .accentBloom(accent, active = isSel, shape = optionShape)
                    .pressScale(interaction, pressedScale = 0.95f)
                    .clip(optionShape)
                    .background(if (isSel) accent else Color.Transparent)
                    .clickable(interaction, indication = null) {
                        if (!isSel) haptic()
                        onSelect(value)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSel) Color.White else appColors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Accent color swatch picker. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchRow(
    choices: List<AccentChoice>,
    selectedChoice: AccentChoice,
    onPick: (AccentChoice) -> Unit,
    enabled: Boolean = true,
) {
    val haptic = rememberHaptic()
    val borderGlass = LocalAppColors.current.borderGlass

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier else Modifier.alpha(0.4f)),
    ) {
        choices.chunked(5).forEach { rowChoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 5) {
                    if (i < rowChoices.size) {
                        val choice = rowChoices[i]
                        key(choice) {
                            val selected = enabled && choice == selectedChoice
                            val interaction = remember { MutableInteractionSource() }
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.08f else 1f,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                                label = "swatchScale",
                            )
                            val outlineWidth by animateDpAsState(
                                targetValue = if (selected) 3.dp else 1.5.dp,
                                animationSpec = SpringSpecs.DpSpring,
                                label = "swatchOutlineWidth",
                            )
                            val outlineColor by animateColorAsState(
                                targetValue = if (selected) Color.White else borderGlass,
                                animationSpec = SpringSpecs.ColorSpring,
                                label = "swatchOutlineColor",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(interaction, indication = null, enabled = enabled) {
                                        haptic()
                                        onPick(choice)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .pressScale(interaction)
                                        .clip(CircleShape)
                                        .background(choice.color)
                                        .border(width = outlineWidth, color = outlineColor, shape = CircleShape),
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ThemeModeItem(
    val mode: ThemeMode,
    val label: String,
    val icon: ImageVector,
)

/** Segmented control for theme mode (Auto / Light / Dark / AMOLED) matching Settings.html. */
@Composable
fun ThemeModeGrid(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val entries = listOf(
        ThemeModeItem(ThemeMode.AUTO, "Auto", Icons.Outlined.BrightnessAuto),
        ThemeModeItem(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        ThemeModeItem(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
        ThemeModeItem(ThemeMode.AMOLED, "AMOLED", Icons.Outlined.Contrast),
    )
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val trackShape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(appColors.bgGlass)
            .border(1.dp, appColors.borderGlass, trackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { item ->
            val isSel = item.mode == selected
            val interaction = remember { MutableInteractionSource() }
            val optionShape = RoundedCornerShape(18.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .accentBloom(accent, active = isSel, shape = optionShape)
                    .pressScale(interaction, pressedScale = 0.95f)
                    .clip(optionShape)
                    .background(if (isSel) accent else Color.Transparent)
                    .clickable(interaction, indication = null) {
                        if (!isSel) haptic()
                        onSelect(item.mode)
                    }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSel) Color.White else appColors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = item.label,
                        color = if (isSel) Color.White else appColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun GlassSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()

    androidx.compose.material3.Slider(
        value = value,
        enabled = enabled,
        onValueChange = {
            haptic()
            onValueChange(it)
        },
        valueRange = valueRange,
        steps = steps,
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = accent,
            inactiveTrackColor = appColors.bgGlass,
            disabledThumbColor = appColors.textSecondary,
            disabledActiveTrackColor = appColors.textSecondary.copy(alpha = 0.35f),
            disabledInactiveTrackColor = appColors.bgGlass,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
