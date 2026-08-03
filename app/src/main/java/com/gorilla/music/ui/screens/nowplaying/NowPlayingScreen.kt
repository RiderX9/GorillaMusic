package com.gorilla.music.ui.screens.nowplaying

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.model.TrackTechInfo
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.data.settings.PlayerStyle
import com.gorilla.music.data.settings.RepeatMode
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.BlurredArtworkBackground
import com.gorilla.music.ui.components.LightweightGlassPanel
import com.gorilla.music.ui.components.LiveMeshBackground
import com.gorilla.music.ui.components.formatBitrate
import com.gorilla.music.ui.components.formatSampleRate
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.playback.PlaybackState
import com.gorilla.music.ui.theme.DesignTokens
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.SpringSpecs
import com.gorilla.music.ui.theme.darken
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.renderEffectBlur
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi

// Fraction of the player height the sheet must travel for the chrome to reach
// full visibility. Echo maps chrome progress over its (expanded - collapsed)
// span, which is the screen height minus the collapsed chrome (~64dp mini
// player + nav inset) — nearly the whole screen. 0.87 ≈ that span on a
// typical phone: the bar rises in step with the sheet for the entire drag.
internal const val PLAYER_COLLAPSE_SPAN = 0.87f

/**
 * Full-screen Now Playing. Palette-driven blurred album-art background (or solid /
 * gradient per Settings), glass control panel with all transports wired to Media3, an
 * animated drag-to-seek bar, swipe-down to collapse, swipe-up for the Track Info sheet.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.NowPlayingScreen(
    app: AppViewModel,
    onCollapse: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    // 0f = fully expanded, 1f = dragged fully off the bottom. The host uses this
    // to raise the nav bar / mini player as the player is swiped down (Echo's
    // collapsed content rises while the sheet falls).
    onDragProgress: (Float) -> Unit = {},
    // True while the finger is actively dragging the player. The host snaps the
    // chrome to the finger while true and runs the settle spring when false.
    onDraggingChange: (Boolean) -> Unit = {},
    infoVm: TrackInfoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = TrackInfoViewModel.Factory),
) {
    val settings by app.settings.collectAsStateWithLifecycle()
    when (settings?.playerStyle ?: PlayerStyle.GORILLA) {
        PlayerStyle.GORILLA -> GorillaNowPlayingScreen(
            app = app,
            onCollapse = onCollapse,
            animatedVisibilityScope = animatedVisibilityScope,
            onDragProgress = onDragProgress,
            onDraggingChange = onDraggingChange,
            infoVm = infoVm,
        )
        PlayerStyle.APPLE_MUSIC -> AppleMusicNowPlayingScreen(
            app = app,
            onCollapse = onCollapse,
            animatedVisibilityScope = animatedVisibilityScope,
            onDragProgress = onDragProgress,
            onDraggingChange = onDraggingChange,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.GorillaNowPlayingScreen(
    app: AppViewModel,
    onCollapse: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDragProgress: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    infoVm: TrackInfoViewModel,
) {
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val isFavorite by app.currentIsFavorite.collectAsStateWithLifecycle()
    val isClosing = animatedVisibilityScope.transition.targetState != androidx.compose.animation.EnterExitState.Visible
    val controlsAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isClosing) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "controlsAlpha"
    )
    val colors = LocalDynamicColors.current
    val haptic = rememberHaptic()
    val track = playback.current ?: return
    val loadedSettings = settings ?: return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Live technical metadata for the always-visible quality pill — reloads per track.
    val techInfo by infoVm.info.collectAsStateWithLifecycle()
    LaunchedEffect(track.id) { infoVm.load(track) }

    // Gentle "breathing" of the album art that re-triggers on play/pause changes.
    val artScale by animateFloatAsState(
        targetValue = if (playback.isPlaying) 1f else 0.97f,
        animationSpec = SpringSpecs.Smooth,
        label = "artBreathe",
    )

    var dragOffsetY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var playerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    // On close we slide the sheet DOWNWARD off the bottom (Echo animates
    // translationY toward the collapsed bound), timed to the host's 260ms
    // fadeOut so they run together. Resetting to 0 instead would snap the
    // sheet back UP to the top before the fade — the "bounce up" bug.
    var collapsing by remember { androidx.compose.runtime.mutableStateOf(false) }

    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = when {
            isDragging -> tween(0) // follow finger 1:1
            collapsing -> tween(260, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            // Non-bouncy spring back for a short/cancelled drag (Echo's default
            // SpringSpec is DampingRatioNoBouncy).
            else -> androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 280f)
        },
        label = "dragAnim"
    )
    // Report drag progress (0..1) to the host so the nav bar / mini player can
    // rise as the player falls. Echo maps progress over the collapse DISTANCE,
    // not the full screen — so the chrome reaches full visibility well before
    // the sheet is all the way off-screen and a gentle swipe registers quickly.
    // We approximate Echo's (expanded - collapsed) span with a fraction of the
    // player height; the chrome is fully up once the sheet has travelled that far.
    LaunchedEffect(animatedOffsetY, playerHeightPx) {
        val span = if (playerHeightPx > 0) playerHeightPx * PLAYER_COLLAPSE_SPAN else 1f
        onDragProgress((animatedOffsetY / span).coerceIn(0f, 1f))
    }
    // Tell the host whether the finger is currently on the sheet. While true the
    // host snaps the chrome to the finger; on release the host's settle spring
    // takes over — one owner, no AnimatedVisibility pop.
    LaunchedEffect(isDragging) { onDraggingChange(isDragging) }

    // Track skip direction (Next vs Previous) matching Accord
    var previousIndex by remember { androidx.compose.runtime.mutableIntStateOf(playback.currentIndex) }
    var isSkipNext by remember { androidx.compose.runtime.mutableStateOf(true) }

    LaunchedEffect(playback.currentIndex) {
        if (playback.currentIndex != previousIndex) {
            isSkipNext = playback.currentIndex >= previousIndex
            previousIndex = playback.currentIndex
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { playerHeightPx = it.height }
            .let { base ->
                if (!isClosing) {
                    base.pointerInput(track.id) {
                        val velocityTracker = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                val velocity = velocityTracker.calculateVelocity().y
                                velocityTracker.resetTracking()
                                if (dragOffsetY > size.height * 0.05f || velocity > 250f) {
                                    collapsing = true
                                    dragOffsetY = size.height.toFloat()
                                    onCollapse()
                                } else {
                                    dragOffsetY = 0f
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                velocityTracker.resetTracking()
                                dragOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPointerInputChange(change)
                                if (dragOffsetY == 0f && dragAmount < -20f) {
                                    app.openTrackInfo(track)
                                } else if (dragOffsetY > 0f || dragAmount > 0f) {
                                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                }
                            },
                        )
                    }
                } else base
            },
    ) {
        val dragFraction = if (playerHeightPx > 0) {
            (animatedOffsetY / playerHeightPx).coerceIn(0f, 1f)
        } else {
            0f
        }
        val backgroundAlpha = 1f - ((dragFraction - 0.5f) * 2f).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backgroundAlpha }
                .background(Color.Black)
        ) {
            when (loadedSettings.artBackground) {
                ArtBackgroundStyle.SOLID_COLOR -> {
                    Box(Modifier.fillMaxSize().background(colors.artBackground.darken(0.3f)))
                }
                ArtBackgroundStyle.GRADIENT -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colors.artPrimary.darken(0.25f),
                                            colors.artSecondary.darken(0.55f),
                                            Color.Black,
                                        ),
                                        center = Offset(size.width * 0.5f, size.height * 0.25f),
                                        radius = size.height * 0.95f,
                                    )
                                )
                            }
                    )
                }
                ArtBackgroundStyle.BLURRED_ART -> {
                    BlurredArtworkBackground(
                        artwork = track.artworkUri ?: albumArtUri(track.albumId),
                    )
                }
                ArtBackgroundStyle.LIVE_MESH -> {
                    LiveMeshBackground(artwork = track.artworkUri ?: albumArtUri(track.albumId))
                }
            }
        }

        val playerContentModifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "player_bounds"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    boundsTransform = { _, _ ->
                        androidx.compose.animation.core.spring(
                            dampingRatio = 0.86f,
                            stiffness = 350f
                        )
                    }
                )
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedOffsetY
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)

        if (isLandscape) {
            Row(
                modifier = playerContentModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 8.dp)
                            .graphicsLayer { alpha = controlsAlpha }
                            .width(48.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.2f), CapsuleShape)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val artworkSize = minOf(maxWidth - 24.dp, maxHeight - 8.dp)
                        Box(
                            Modifier
                                .size(artworkSize)
                                .graphicsLayer {
                                    alpha = controlsAlpha
                                    scaleX = artScale
                                    scaleY = artScale
                                }
                                .shadow(
                                    16.dp,
                                    RoundedCornerShape(20.dp),
                                    clip = false,
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black,
                                )
                                .clip(RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AlbumArt(
                                albumId = track.albumId,
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(20.dp),
                                artworkUri = track.artworkUri,
                                fallbackTitle = track.title,
                                fallbackSubtitle = track.displayArtist,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = track.id,
                                transitionSpec = {
                                    (fadeIn(tween(250, delayMillis = 50)) + slideInVertically { it / 3 }) togetherWith
                                        (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                                },
                                label = "landscapeTitleArtistInPlace",
                            ) {
                                Column {
                                    Text(
                                        text = track.title,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = track.displayArtist,
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        MoreOptionsGlassButton(
                            enabled = !isClosing,
                            onClick = { haptic(); app.openTrackMenu(track, playback.queue) },
                        )
                        Spacer(Modifier.width(8.dp))
                        FavoriteGlassButton(
                            isFavorite = isFavorite,
                            enabled = !isClosing,
                            onClick = { haptic(); app.toggleFavoriteCurrent() },
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    AnimatedContent(
                        targetState = track.id,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "landscapeQualityFade",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .graphicsLayer { alpha = controlsAlpha },
                    ) {
                        QualityPill(techInfo = techInfo)
                    }
                    Spacer(Modifier.height(10.dp))
                    SeekBar(
                        positionMs = playback.positionMs,
                        durationMs = playback.durationMs.coerceAtLeast(track.durationMs),
                        onSeek = { app.playback.seekTo(it) },
                        accent = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha },
                        enabled = !isClosing,
                    )
                    Spacer(Modifier.height(10.dp))
                    MainControlsCard(
                        playback = playback,
                        app = app,
                        haptic = haptic,
                        accent = colors.accent,
                        enabled = !isClosing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha },
                    )
                    Spacer(Modifier.height(8.dp))
                    BottomActionBar(
                        track = track,
                        app = app,
                        haptic = haptic,
                        enabled = !isClosing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = controlsAlpha },
                    )
                }
            }
        } else {
            Column(
                modifier = playerContentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Box(
                Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { alpha = controlsAlpha }
                    .width(48.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), CapsuleShape)
            )

            // 1. Album art with Accord in-place crossfade and play/pause scale
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha }
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .shadow(16.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                AlbumArt(
                    albumId = track.albumId,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp),
                    artworkUri = track.artworkUri,
                    fallbackTitle = track.title,
                    fallbackSubtitle = track.displayArtist,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 2. Track info row with Accord in-place vertical fade
            Row(
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = track.id,
                        transitionSpec = {
                            (fadeIn(tween(250, delayMillis = 50)) + slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                        },
                        label = "titleArtistInPlace",
                    ) { _ ->
                        Column {
                            Text(
                                text = track.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.displayArtist,
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                MoreOptionsGlassButton(
                    enabled = !isClosing,
                    onClick = { haptic(); app.openTrackMenu(track, playback.queue) }
                )
                Spacer(Modifier.width(8.dp))
                FavoriteGlassButton(
                    isFavorite = isFavorite,
                    enabled = !isClosing,
                    onClick = { haptic(); app.toggleFavoriteCurrent() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. Music quality pill — centered, between track info and progress bar
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "qualityFade",
                modifier = Modifier.align(Alignment.CenterHorizontally).graphicsLayer { alpha = controlsAlpha },
            ) { _ ->
                QualityPill(
                    techInfo = techInfo
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Progress bar + timestamps — seek bar, time left, duration right
            SeekBar(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs.coerceAtLeast(track.durationMs),
                onSeek = { app.playback.seekTo(it) },
                accent = colors.accent,
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }.padding(horizontal = 16.dp),
                enabled = !isClosing
            )

            Spacer(Modifier.height(16.dp))

            // 5. Main controls card — frosted glass rounded rectangle (border-radius 24dp)
            // Shuffle | Previous | Play/Pause | Next | Repeat
            MainControlsCard(
                playback = playback,
                app = app,
                haptic = haptic,
                accent = colors.accent,
                enabled = !isClosing,
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 6. Bottom action bar — separate frosted glass pill, ~12dp below controls card
            // Lyrics | Info side-by-side with 1px vertical divider
            BottomActionBar(
                track = track,
                app = app,
                haptic = haptic,
                enabled = !isClosing,
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FavoriteGlassButton(
    isFavorite: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }

    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val isFirstLaunch = remember { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(isFavorite) {
        if (isFirstLaunch.value) {
            isFirstLaunch.value = false
            return@LaunchedEffect
        }
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 75,
                easing = androidx.compose.animation.core.LinearEasing
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 75,
                easing = androidx.compose.animation.core.LinearEasing
            )
        )
    }
    
    val accentColor = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val glassContentColor = if (!appColors.isDark) {
        appColors.textPrimary
    } else {
        Color.White
    }

    LightweightGlassPanel(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction)
            .let { if (enabled) it.clickable(interaction, indication = null, onClick = onClick) else it }
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) accentColor else glassContentColor.copy(alpha = 0.72f),
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun MoreOptionsGlassButton(
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    
    val appColors = LocalAppColors.current
    val glassContentColor = if (!appColors.isDark) {
        appColors.textPrimary
    } else {
        Color.White
    }

    LightweightGlassPanel(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction)
            .let { if (enabled) it.clickable(interaction, indication = null, onClick = onClick) else it }
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Track options",
            tint = glassContentColor.copy(alpha = 0.72f),
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun QualityPill(
    techInfo: TrackTechInfo?,
    modifier: Modifier = Modifier,
) {
    val label = remember(techInfo) {
        val segments = mutableListOf<String>()
        techInfo?.let { info ->
            if (info.sampleRateHz > 0) segments += formatSampleRate(info.sampleRateHz)
            if (info.bitrateKbps > 0) segments += formatBitrate(info.bitrateKbps)
            val fmt = info.format.ifBlank { info.codec }
            if (fmt.isNotBlank()) segments += fmt.uppercase()
        }
        segments.joinToString(" · ")
    }
    if (label.isBlank()) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = DesignTokens.SpringSnappy,
        label = "pillScale"
    )
    val appColors = LocalAppColors.current
    val glassContentColor = if (!appColors.isDark) {
        appColors.textPrimary.copy(alpha = 0.70f)
    } else {
        Color.White.copy(alpha = 0.70f)
    }

    LightweightGlassPanel(
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
                shape = RoundedCornerShape(percent = 50)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
    ) {
        Text(
            text = label,
            fontSize = 11.sp, // 11sp
            fontFamily = FontFamily.Monospace, // tabular / monospaced figures
            fontWeight = FontWeight.Normal,
            color = glassContentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MainControlsCard(
    playback: PlaybackState,
    app: AppViewModel,
    haptic: () -> Unit,
    accent: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    LightweightGlassPanel(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            ControlButton(
                icon = Icons.Rounded.Shuffle,
                desc = "Shuffle",
                targetSize = 44.dp,
                iconSize = 24.dp,
                hasActiveState = true,
                active = playback.shuffle,
                accentColor = accent,
                enabled = enabled,
                onClick = { haptic(); app.playback.toggleShuffle() }
            )
            // Previous
            ControlButton(
                icon = Icons.Rounded.SkipPrevious,
                desc = "Previous",
                targetSize = 52.dp,
                iconSize = 28.dp,
                accentColor = accent,
                hasCircleBackground = true,
                enabled = enabled,
                onClick = { haptic(); app.playback.previous() }
            )
            // Play/Pause circle
            PlayPauseCircle(
                isPlaying = playback.isPlaying,
                accentColor = accent,
                enabled = enabled,
                onClick = { haptic(); app.playback.togglePlayPause() }
            )
            // Next
            ControlButton(
                icon = Icons.Rounded.SkipNext,
                desc = "Next",
                targetSize = 52.dp,
                iconSize = 28.dp,
                accentColor = accent,
                hasCircleBackground = true,
                enabled = enabled,
                onClick = { haptic(); app.playback.next() }
            )
            // Repeat
            ControlButton(
                icon = if (playback.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                desc = "Repeat",
                targetSize = 44.dp,
                iconSize = 24.dp,
                hasActiveState = true,
                active = playback.repeat != RepeatMode.OFF,
                accentColor = accent,
                enabled = enabled,
                onClick = { haptic(); app.playback.cycleRepeat() }
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    desc: String,
    targetSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    hasActiveState: Boolean = false,
    active: Boolean = false,
    accentColor: Color = Color.White,
    hasCircleBackground: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val appColors = LocalAppColors.current
    val inactiveColor = if (!appColors.isDark) {
        appColors.textPrimary
    } else {
        Color.White
    }
    
    // Inactive/default: theme foreground. Active: Accent color.
    val baseColor = if (hasActiveState && active) accentColor else inactiveColor
    val baseOpacity = if (hasActiveState && !active) 0.45f else 0.85f
    val targetOpacity = if (isPressed) baseOpacity * 0.7f else baseOpacity

    val content = @Composable {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = baseColor.copy(alpha = targetOpacity),
            modifier = Modifier.size(iconSize)
        )
    }

    if (hasCircleBackground) {
        LightweightGlassPanel(
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier
                .size(targetSize)
                .pressScale(interaction)
                .let { if (enabled) it.clickable(interaction, indication = null, onClick = onClick) else it }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(targetSize)
                .pressScale(interaction)
                .let { if (enabled) it.clickable(interaction, indication = null, onClick = onClick) else it },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun PlayPauseCircle(
    isPlaying: Boolean,
    accentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = if (isPressed) 0.70f else 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .pressScale(interaction)
            .let { if (enabled) it.clickable(interaction, indication = null, onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun BottomActionBar(
    track: Track,
    app: AppViewModel,
    haptic: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val appColors = LocalAppColors.current
    val contentColor = if (!appColors.isDark) {
        appColors.textPrimary.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    val dividerColor = if (!appColors.isDark) {
        Color.Black.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }

    LightweightGlassPanel(
        shape = CapsuleShape,
        modifier = modifier
            .height(56.dp) // height ~56dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lyrics Column/Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { if (enabled) it.clickable { haptic(); app.openLyrics(track) } else it },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics, // lines icon
                        contentDescription = "Lyrics",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Lyrics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }

            // 1px rgba(255,255,255,0.15) vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.4f)
                    .background(dividerColor)
            )

            // Info Column/Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .let { if (enabled) it.clickable { haptic(); app.openTrackInfo(track) } else it },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info, // ℹ icon
                        contentDescription = "Info",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Info",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}
