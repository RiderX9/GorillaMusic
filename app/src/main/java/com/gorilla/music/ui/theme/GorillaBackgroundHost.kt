package com.gorilla.music.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.layerBackdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.rememberLayerBackdrop

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalLiquidGlassContentBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Mounts the app background directly above the navigation graph and renders [content] on
 * top of it. The layer is still captured for liquid glass, but the animated ambient blob
 * renderer is intentionally not mounted.
 */
@Composable
fun GorillaBackgroundHost(
    nowPlayingVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    val backdrop = rememberLayerBackdrop()

    CompositionLocalProvider(LocalLiquidGlassBackdrop provides backdrop) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .layerBackdrop(backdrop)
            ) {
                Box(Modifier.matchParentSize().background(appColors.bgBase))
            }
            content()
        }
    }
}
