package com.gorilla.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.data.settings.PlayerStyle
import com.gorilla.music.ui.theme.BlurIntensity

@Composable
fun AppearanceTab(settings: AppSettings, vm: SettingsViewModel) {
    Column {
        ThemeSettingsPanel(settings = settings, vm = vm)

        SettingRow(
            title = "Liquid Glass",
            subtitle = if (settings.liquidGlassEnabled) "True liquid glass effects enabled" else "Use the standard glass UI",
        ) {
            GlassSwitch(settings.liquidGlassEnabled, vm::setLiquidGlassEnabled)
        }

        if (settings.liquidGlassEnabled) {
            SettingBlock(title = "Liquid Glass intensity", subtitle = "Choose the liquid glass strength") {
                SegmentedControl(
                    options = listOf(
                        BlurIntensity.LOW to "Low",
                        BlurIntensity.MEDIUM to "Default",
                        BlurIntensity.HIGH to "High",
                    ),
                    selected = settings.liquidGlassIntensity,
                    onSelect = vm::setLiquidGlassIntensity,
                )
            }
        }

        SettingBlock(
            title = "Now Playing style",
            subtitle = "Choose the full-screen player layout",
        ) {
            SegmentedControl(
                options = listOf(
                    PlayerStyle.GORILLA to "Gorilla",
                    PlayerStyle.APPLE_MUSIC to "Apple",
                ),
                selected = settings.playerStyle,
                onSelect = vm::setPlayerStyle,
            )
        }

        SettingBlock(title = "Now Playing background") {
            SegmentedControl(
                options = listOf(
                    ArtBackgroundStyle.BLURRED_ART to "Blurred",
                    ArtBackgroundStyle.SOLID_COLOR to "Solid",
                    ArtBackgroundStyle.GRADIENT to "Gradient",
                    ArtBackgroundStyle.LIVE_MESH to "Live Mesh",
                ),
                selected = settings.artBackground,
                onSelect = vm::setArtBackground,
            )
        }

    }
}
