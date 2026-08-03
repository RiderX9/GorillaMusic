package com.gorilla.music.ui.screens.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gorilla.music.R
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.utils.LyricsParser

/**
 * Echo's Apple lyrics mode: full-screen word-karaoke lyrics over a blurred
 * artwork background with the compact now-playing header. Adapted to
 * Gorilla's local tracks and lyric providers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(app: AppViewModel, track: Track, onDismiss: () -> Unit, vm: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory)) {
    val lyricsState by vm.lyricsState.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val activeTrack = playback.current ?: track
    var lyricsOnly by remember { mutableStateOf(false) }
    LaunchedEffect(activeTrack.id) { vm.loadLyrics(activeTrack) }
    BackHandler {
        if (lyricsOnly) lyricsOnly = false else onDismiss()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LyricsBackground(activeTrack)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val state = lyricsState) {
                    LyricsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    is LyricsState.Found -> {
                        val entries = remember(state.lyrics) {
                            if (state.isSynced) LyricsParser.parseLyrics(state.lyrics) else emptyList()
                        }
                        if (entries.isEmpty()) {
                            ApplePlainLyrics(state.lyrics)
                        } else {
                            AppleLyrics(
                                entries = entries,
                                positionProvider = app.playback::currentPositionMs,
                                onSeek = app.playback::seekTo,
                            )
                        }
                    }
                    LyricsState.NotFound, LyricsState.Offline -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (lyricsState == LyricsState.Offline) "Lyrics unavailable" else "No lyrics found",
                            color = Color.White.copy(.65f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (!lyricsOnly) {
                LyricsMiniPlayer(
                    app = app,
                    track = activeTrack,
                    onFullscreen = { lyricsOnly = true },
                )
            }
        }
        if (lyricsOnly) {
            AppleCircleButton(
                icon = R.drawable.fullscreen,
                description = "Exit fullscreen",
                onClick = { lyricsOnly = false },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 14.dp)
                    .align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun LyricsBackground(track: Track) {
    val source = track.artworkUri ?: albumArtUri(track.albumId)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(source).size(128, 128).allowHardware(false).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.2f; scaleY = 1.2f }.blur(150.dp),
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.28f), Color.Black.copy(.72f)))))
}

/**
 * Compact now-playing controls under the lyrics: 56dp artwork, title/artist,
 * circular fullscreen + more buttons, springy slider, Apple transport and
 * volume rows (Echo's lyric-mode header + classic controls).
 */
@Composable
private fun LyricsMiniPlayer(app: AppViewModel, track: Track, onFullscreen: () -> Unit) {
    val playback by app.playbackState.collectAsStateWithLifecycle()
    // Echo uses PlayerHorizontalPadding (32dp); the sheet column already
    // provides 24dp, so pad the remaining 8dp here.
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                fallbackTitle = track.title,
                fallbackSubtitle = track.displayArtist,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(56.dp).aspectRatio(1f),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.displayArtist, color = Color.White.copy(.68f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            AppleCircleButton(R.drawable.fullscreen, "Fullscreen", onFullscreen)
            Spacer(Modifier.width(12.dp))
            AppleCircleButton(R.drawable.more_horiz, "Track options", { app.openTrackMenu(track, playback.queue) })
        }
        Spacer(Modifier.height(12.dp))
        AppleProgressSlider(
            value = playback.positionMs,
            valueRangeEnd = playback.durationMs.coerceAtLeast(track.durationMs),
            onValueChangeFinished = app.playback::seekTo,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                ResizableIconButton(
                    icon = R.drawable.apple_skip_previous,
                    color = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    onClick = app.playback::previous,
                )
            }
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                ResizableIconButton(
                    icon = if (playback.isPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic,
                    color = Color.White,
                    modifier = Modifier.size(72.dp),
                    onClick = app.playback::togglePlayPause,
                )
            }
            Box(Modifier.weight(1f)) {
                ResizableIconButton(
                    icon = R.drawable.apple_skip_next,
                    color = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                    onClick = app.playback::next,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AppleVolumeSlider()
    }
}
