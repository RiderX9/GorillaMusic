package com.gorilla.music.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.music.data.settings.AccentChoice
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.rememberHaptic

@Composable
fun ThemeSettingsPanel(
    settings: AppSettings,
    vm: SettingsViewModel,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val colorLabel = if (settings.dynamicTheme) "Dynamic" else settings.accent.label

    SettingRow(
        title = "Theme",
        subtitle = "${settings.themeMode.displayLabel()} \u2022 $colorLabel",
    ) {
        Text(
            text = if (expanded) "Hide" else "Edit",
            color = LocalDynamicColors.current.accent,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    haptic()
                    expanded = !expanded
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(Modifier.padding(vertical = 2.dp)) {
            SettingBlock(
                title = "Theme mode",
                subtitle = "Choose how light and dark surfaces are displayed",
            ) {
                ThemeModeGrid(
                    selected = settings.themeMode,
                    onSelect = vm::setThemeMode,
                )
            }

            SettingRow(
                title = "Dynamic Theme",
                subtitle = "Match the interface to the current album artwork",
            ) {
                GlassSwitch(settings.dynamicTheme, vm::setDynamicTheme)
            }

            SettingBlock(
                title = "Accent color",
                subtitle = if (settings.dynamicTheme) {
                    "Used when Dynamic Theme is off"
                } else {
                    settings.accent.label
                },
            ) {
                ColorSwatchRow(
                    choices = AccentChoice.entries,
                    selectedChoice = settings.accent,
                    onPick = { choice ->
                        if (settings.dynamicTheme) {
                            vm.setDynamicTheme(false)
                        }
                        vm.setAccent(choice)
                    },
                    enabled = true,
                )
            }
        }
    }
}

private fun com.gorilla.music.ui.theme.ThemeMode.displayLabel(): String =
    when (this) {
        com.gorilla.music.ui.theme.ThemeMode.AUTO -> "Auto"
        com.gorilla.music.ui.theme.ThemeMode.LIGHT -> "Light"
        com.gorilla.music.ui.theme.ThemeMode.DARK -> "Dark"
        com.gorilla.music.ui.theme.ThemeMode.AMOLED -> "AMOLED"
    }
