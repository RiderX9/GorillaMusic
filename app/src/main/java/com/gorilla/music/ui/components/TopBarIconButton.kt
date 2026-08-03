package com.gorilla.music.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable

@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent

    LiquidGlassSurface(
        depth = GlassDepth.MID,
        shape = CapsuleShape,
        modifier = modifier
            .instantClickable(pressedScale = 0.92f) {
                onClick()
            },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier
                .padding(if (small) 8.dp else 12.dp)
                .size(if (small) 20.dp else 24.dp),
        )
    }
}
