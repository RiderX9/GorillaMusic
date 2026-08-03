package com.gorilla.music.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.music.BuildConfig
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.LibraryStatus
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic

private data class License(val name: String, val license: String)

private val licenses = listOf(
    License("Jetpack Compose", "Apache-2.0"),
    License("AndroidX Media3 (ExoPlayer)", "Apache-2.0"),
    License("AndroidX Room", "Apache-2.0"),
    License("AndroidX DataStore", "Apache-2.0"),
    License("AndroidX Palette", "Apache-2.0"),
    License("Coil", "Apache-2.0"),
    License("Accompanist", "Apache-2.0"),
    License("Kotlin Coroutines", "Apache-2.0"),
    License("JAudioTagger (ALAC, FLAC, M4A, MP3, OGG, WAV)", "LGPL-3.0-or-later"),
)

@Composable
fun AboutTab(app: AppViewModel) {
    val context = LocalContext.current
    val status by app.libraryStatus.collectAsStateWithLifecycle()
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val tracks by app.tracks.collectAsStateWithLifecycle()
    val cardShape = RoundedCornerShape(22.dp)

    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        // App identity card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(cardShape)
                .background(appColors.bgSurface.copy(alpha = 0.94f))
                .border(1.dp, appColors.borderGlass, cardShape)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = "GorillaMusic",
                    color = appColors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                    color = appColors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "${tracks.size} tracks indexed on device",
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        // Link 1
        LinkRow("GitHub repository", "github.com/RiderX9/GorillaMusic", Icons.Rounded.Code) {
            open("https://github.com/RiderX9/GorillaMusic")
        }

        // Link 2
        LinkRow("Developer", "Open developer page", Icons.Rounded.OpenInNew) {
            open("https://github.com/RiderX9")
        }

        // Scan library button
        ActionRow(
            title = if (status == LibraryStatus.SCANNING) "Scanning..." else "Scan library again",
            subtitle = "Re-read audio files from storage",
            icon = Icons.Rounded.Refresh,
            enabled = status != LibraryStatus.SCANNING,
            onClick = { app.rescan() },
        )

        if (status == LibraryStatus.SCANNING) {
            LinearProgressIndicator(
                color = accent,
                trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(4.dp)
                    .clip(CircleShape),
            )
        }

        // Licenses card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(cardShape)
                .background(appColors.bgSurface.copy(alpha = 0.94f))
                .border(1.dp, appColors.borderGlass, cardShape)
                .padding(18.dp),
        ) {
            Column {
                Text(
                    text = "Open source licenses",
                    color = appColors.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                licenses.forEach { lic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lic.name,
                            color = appColors.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = lic.license,
                            color = appColors.textSecondary,
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ActionRow(title, subtitle, icon, enabled = true, onClick = onClick)
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, cardShape)
            .clickable(interaction, indication = null, enabled = enabled) {
                haptic()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = appColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = appColors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
