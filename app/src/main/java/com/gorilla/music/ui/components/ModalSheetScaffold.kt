package com.gorilla.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gorilla.music.ui.theme.DesignTokens
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.music.ui.theme.LocalTrueLiquidGlassEnabled
import com.gorilla.music.ui.theme.SheetShape
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalSheetScaffold(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    heightFraction: Float? = 0.62f,
    skipPartiallyExpanded: Boolean = true,
    shape: RoundedCornerShape = SheetShape,
    surfaceColor: Color = Color(0xFF1E1E2A).copy(alpha = 0.92f),
    tintAlphaOverride: Float? = 0.07f,
    dragHandleColor: Color = Color.White.copy(alpha = 0.5f),
    enableLens: Boolean = false,
    plainSurface: Boolean = false,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val trueLiquidGlassEnabled = LocalTrueLiquidGlassEnabled.current
    val contentBackdrop = backdrop ?: LocalLiquidGlassContentBackdrop.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = {
            // Allows the hidden state, so a drag-down gesture can actually dismiss it.
            true
        }
    )

    val dragHandleHeight = 24.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = if (heightFraction == null) {
            null
        } else {
            {
                Box(
                    Modifier
                        .zIndex(1f)
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(dragHandleColor, RoundedCornerShape(2.dp))
                )
            }
        },
        sheetMaxWidth = Dp.Unspecified,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (heightFraction == null) {
            val extraBottomPadding = 6.dp
            val surfaceModifier = modifier
                .zIndex(-1f)
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height - extraBottomPadding.roundToPx()) {
                        placeable.placeRelative(0, 0)
                    }
                }

            if (plainSurface) {
                Box(
                    modifier = surfaceModifier
                        .clip(shape)
                        .background(surfaceColor)
                ) {
                    CompactSheetContent(
                        appColors = appColors,
                        extraBottomPadding = extraBottomPadding,
                        content = content,
                    )
                }
                return@ModalBottomSheet
            }

            LiquidGlassSurface(
                depth = GlassDepth.MID,
                shape = shape,
                shadow = false,
                surfaceColor = surfaceColor,
                tintAlphaOverride = tintAlphaOverride,
                saturationOverride = if (trueLiquidGlassEnabled) 1.25f else null,
                enableLens = enableLens,
                backdrop = if (trueLiquidGlassEnabled) contentBackdrop else null,
                modifier = surfaceModifier,
            ) {
                CompactSheetContent(
                    appColors = appColors,
                    extraBottomPadding = extraBottomPadding,
                    content = content,
                )
            }
            return@ModalBottomSheet
        }

        LiquidGlassSurface(
            depth = GlassDepth.HIGH,
            shape = shape,
            surfaceColor = surfaceColor,
            tintAlphaOverride = tintAlphaOverride,
            saturationOverride = if (trueLiquidGlassEnabled) 1.25f else null,
            enableLens = enableLens,
            backdrop = if (trueLiquidGlassEnabled) contentBackdrop else null,
            modifier = modifier
                .zIndex(-1f)
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .layout { measurable, constraints ->
                    val offset = dragHandleHeight.roundToPx()
                    val placeable = measurable.measure(constraints.copy(maxHeight = constraints.maxHeight + offset))
                    layout(placeable.width, placeable.height - offset) {
                        placeable.placeRelative(0, -offset)
                    }
                }
        ) {
            Box(Modifier.padding(top = dragHandleHeight)) {
                content()
            }
        }
    }
}

@Composable
private fun CompactSheetContent(
    appColors: com.gorilla.music.ui.theme.AppColors,
    extraBottomPadding: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = extraBottomPadding)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .size(width = 32.dp, height = 4.dp)
                .background(
                    color = if (appColors.isDark) {
                        Color.White.copy(alpha = 0.25f)
                    } else {
                        Color.Black.copy(alpha = 0.28f)
                    },
                    shape = RoundedCornerShape(6.dp),
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            content()
        }
    }
}
