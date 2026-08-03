package com.gorilla.music.ui.screens.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic

/** Shown until the user grants the audio read permission. */
@Composable
fun PermissionGate(onRequest: () -> Unit) {
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = accent,
            )
            Text(
                "GorillaMusic needs access to your music",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                "Grant audio access to scan your library. Everything plays offline — nothing leaves your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LiquidGlassSurface(
                depth = GlassDepth.MID,
                shape = CapsuleShape,
                modifier = Modifier
                    .pressScale(interaction)
                    .clickable(interaction, indication = null) { haptic(); onRequest() },
            ) {
                Text(
                    "Grant access",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                )
            }
        }
    }
}
