package com.gorilla.music.ui.screens.nowplaying

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gorilla.music.R
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.LiveMeshBackground
import com.gorilla.music.ui.components.BlurredArtworkBackground
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.layerBackdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalGlassEffectConfig
import com.gorilla.music.ui.theme.LocalTrueLiquidGlassEnabled
import com.gorilla.music.ui.theme.darken
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.utils.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Apple Music style Now Playing screen, ported from Echo Music's
 * BottomSheetPlayer (APPLE_MUSIC background + classic transport design)
 * and Queue.kt's collapsed Apple action bar.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.AppleMusicNowPlayingScreen(
    app: AppViewModel,
    onCollapse: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDragProgress: (Float) -> Unit = {},
    onDraggingChange: (Boolean) -> Unit = {},
) {
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val isFavorite by app.currentIsFavorite.collectAsStateWithLifecycle()
    val track = playback.current ?: return
    val bgSettings by app.settings.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val transitionArtworkModel = remember(track.id, track.albumId, track.artworkUri) {
        ImageRequest.Builder(context)
            .data(track.artworkUri ?: albumArtUri(track.albumId))
            // This painter is shared between the 60dp queue header and the
            // full-width player artwork. Decode for the larger destination so
            // opening a queued track cannot leave the player scaling a thumbnail.
            .size(1024, 1024)
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
    val transitionArtworkPainter = rememberAsyncImagePainter(transitionArtworkModel)
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showAudioOutput by remember { mutableStateOf(false) }
    val playerBackdrop = rememberLayerBackdrop()
    // Lyrics live inside the player: the artwork crossfades to an inline
    // lyrics view instead of opening a separate screen (Echo's
    // showInlineLyrics toggle in Player.kt).
    var showInlineLyrics by rememberSaveable { mutableStateOf(false) }
    // Fullscreen lyrics: everything below the seekbar slides down and away,
    // leaving thumbnail + title + slider (Echo's isFullScreen, Player.kt:813).
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    // Back exits the queue first, then fullscreen lyrics, then inline lyrics.
    BackHandler(enabled = showAudioOutput || showSleepTimer || showQueue || isFullScreen || showInlineLyrics) {
        when {
            showAudioOutput -> showAudioOutput = false
            showSleepTimer -> showSleepTimer = false
            showQueue -> showQueue = false
            isFullScreen -> isFullScreen = false
            else -> showInlineLyrics = false
        }
    }
    // Leaving lyrics always drops fullscreen.
    LaunchedEffect(showInlineLyrics) {
        if (!showInlineLyrics) isFullScreen = false
    }

    // Swipe-to-minimize matching the Gorilla player: the sheet follows the
    // finger 1:1 (tween(0) while dragging), then springs back or collapses.
    // A fast downward flick collapses via velocity even below the distance
    // threshold (Echo BottomSheet.performFling).
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var playerHeightPx by remember { mutableIntStateOf(0) }
    // On close we slide the sheet DOWNWARD off the bottom (Echo animates
    // translationY toward the collapsed bound), timed to the host's 260ms
    // fadeOut so they run together. Resetting to 0 instead would snap the
    // sheet back UP to the top before the fade — the "bounce up" bug.
    var collapsing by remember { mutableStateOf(false) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = when {
            isDragging -> tween(0) // follow finger 1:1
            collapsing -> tween(260, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            // Non-bouncy spring back for a short/cancelled drag.
            else -> spring(dampingRatio = 1f, stiffness = 280f)
        },
        label = "appleDragAnim",
    )
    // Report drag progress (0..1) to the host so the nav bar / mini player can
    // rise as the player falls. Mapped over the collapse span (not full screen)
    // so the chrome rises quickly on a short swipe — Echo parity.
    LaunchedEffect(animatedOffsetY, playerHeightPx) {
        val span = if (playerHeightPx > 0) playerHeightPx * PLAYER_COLLAPSE_SPAN else 1f
        onDragProgress((animatedOffsetY / span).coerceIn(0f, 1f))
    }
    // Report finger-down/up so the host's settle spring owns the release motion.
    LaunchedEffect(isDragging) { onDraggingChange(isDragging) }

    // Outer container is PINNED full-screen (never translated). Echo's
    // BottomSheet splits into a `background` slot drawn fillMaxSize + alpha-only
    // and a `content` slot that carries translationY; only the content slides.
    // Doing the same here makes the mesh/blur fill the whole screen and
    // dissolve by alpha as the sheet falls, instead of riding down as a
    // rectangle with its top edge exposed.
    // Accord track skip direction tracking (Next vs Previous)
    var previousIndex by remember { mutableIntStateOf(playback.currentIndex) }
    var isSkipNext by remember { mutableStateOf(true) }

    LaunchedEffect(playback.currentIndex) {
        if (playback.currentIndex != previousIndex) {
            isSkipNext = playback.currentIndex >= previousIndex
            previousIndex = playback.currentIndex
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (showSleepTimer) Modifier.layerBackdrop(playerBackdrop) else Modifier)
            .onSizeChanged { playerHeightPx = it.height }
            .then(
                if (showQueue || showSleepTimer) {
                    Modifier
                } else {
                    Modifier.pointerInput(track.id) {
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
                            onVerticalDrag = { change, amount ->
                                if (amount > 0f || dragOffsetY > 0f) {
                                    change.consume()
                                    velocityTracker.addPointerInputChange(change)
                                    dragOffsetY = (dragOffsetY + amount).coerceAtLeast(0f)
                                }
                            },
                        )
                    }
                }
            ),
    ) {
        val dynColors = LocalDynamicColors.current
        val dragFraction = if (playerHeightPx > 0)
            (animatedOffsetY / playerHeightPx).coerceIn(0f, 1f) else 0f
        val bgAlpha = 1f - ((dragFraction - 0.5f) * 2f).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bgAlpha }
                .background(Color.Black)
        ) {
            when (bgSettings?.artBackground) {
                ArtBackgroundStyle.SOLID_COLOR, null ->
                    Box(Modifier.fillMaxSize().background(dynColors.artBackground.darken(0.3f)))
                ArtBackgroundStyle.GRADIENT ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            dynColors.artPrimary.darken(0.25f),
                                            dynColors.artSecondary.darken(0.55f),
                                            Color.Black,
                                        ),
                                        center = Offset(size.width * 0.5f, size.height * 0.25f),
                                        radius = size.height * 0.95f,
                                    )
                                )
                            }
                    )
                ArtBackgroundStyle.BLURRED_ART ->
                    BlurredArtworkBackground(
                        artwork = track.artworkUri ?: albumArtUri(track.albumId),
                    )
                ArtBackgroundStyle.LIVE_MESH ->
                    LiveMeshBackground(artwork = track.artworkUri ?: albumArtUri(track.albumId))
            }
        }
        val inputShieldInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = inputShieldInteraction,
                    indication = null,
                    onClick = {},
                )
                .clearAndSetSemantics {},
        )
        val playerContentModifier = Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState("player_bounds"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                )
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedOffsetY
                }
                .statusBarsPadding()
                .navigationBarsPadding()

        if (isLandscape) {
            Row(
                modifier = playerContentModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = showQueue,
                    transitionSpec = {
                        val accordEasing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f)
                        fadeIn(tween(350, easing = accordEasing)) togetherWith
                            fadeOut(tween(350, easing = accordEasing))
                    },
                    label = "landscapeQueueTransition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .fillMaxWidth(),
                ) { queueVisible ->
                    val landscapeQueueTransitionScope = this
                    if (queueVisible) {
                        AppleQueueScreen(
                            app = app,
                            playback = playback,
                            track = track,
                            isFavorite = isFavorite,
                            animatedVisibilityScope = landscapeQueueTransitionScope,
                            transitionArtworkPainter = transitionArtworkPainter,
                            onDismiss = { showQueue = false },
                            compact = true,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppleNowPlayingHeader(track = track, isSkipNext = isSkipNext)
                            AnimatedContent(
                                targetState = showInlineLyrics,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "LandscapeLyrics",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) { showLyrics ->
                                if (showLyrics) {
                                    AppleInlineLyrics(app = app, track = track)
                                } else {
                                    AppleArtworkCarousel(
                                        app = app,
                                        playback = playback,
                                        animatedVisibilityScope = landscapeQueueTransitionScope,
                                        transitionArtworkPainter = transitionArtworkPainter,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppleMetadata(
                        track = track,
                        isFavorite = isFavorite,
                        showThumbnail = showInlineLyrics,
                        isSkipNext = isSkipNext,
                        onMore = { app.openTrackMenu(track, playback.queue) },
                        onFavorite = app::toggleFavoriteCurrent,
                        onFullscreen = { isFullScreen = !isFullScreen },
                    )
                    Spacer(Modifier.height(8.dp))
                    AppleProgressSlider(
                        value = playback.positionMs,
                        valueRangeEnd = playback.durationMs.coerceAtLeast(track.durationMs),
                        onValueChangeFinished = app.playback::seekTo,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        isLossless = track.isLossless,
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedVisibility(
                        visible = !isFullScreen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) +
                            slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppleTransport(
                                isPlaying = playback.isPlaying,
                                onPrevious = { haptic(); app.playback.previous() },
                                onPlayPause = { haptic(); app.playback.togglePlayPause() },
                                onNext = { haptic(); app.playback.next() },
                            )
                            AppleVolumeSlider(modifier = Modifier.padding(horizontal = 32.dp))
                            AppleBottomActions(
                                app = app,
                                onQueue = {
                                    if (!showQueue) {
                                        showInlineLyrics = false
                                        isFullScreen = false
                                    }
                                    showQueue = !showQueue
                                },
                                onOutput = { showAudioOutput = true },
                                onTimer = { showSleepTimer = true },
                                onLyrics = {
                                    showQueue = false
                                    showInlineLyrics = !showInlineLyrics
                                },
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = playerContentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            AnimatedContent(
                targetState = showQueue,
                transitionSpec = {
                    val accordEasing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f)
                    (fadeIn(tween(350, easing = accordEasing)) togetherWith
                        fadeOut(tween(350, easing = accordEasing))).apply {
                        targetContentZIndex = 1f
                    }
                },
                label = "appleQueueTransition",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { queueVisible ->
                val queueTransitionScope = this
                if (queueVisible) {
                    AppleQueueScreen(
                        app = app,
                        playback = playback,
                        track = track,
                        isFavorite = isFavorite,
                        animatedVisibilityScope = queueTransitionScope,
                        transitionArtworkPainter = transitionArtworkPainter,
                        onDismiss = { showQueue = false },
                    )
                } else {
                    Column(Modifier.fillMaxSize()) {
                        AppleNowPlayingHeader(track = track, isSkipNext = isSkipNext)
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "Lyrics",
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) { showLyrics ->
                            if (showLyrics) {
                                AppleInlineLyrics(app = app, track = track)
                            } else {
                                AppleArtworkCarousel(
                                    app = app,
                                    playback = playback,
                                    animatedVisibilityScope = queueTransitionScope,
                                    transitionArtworkPainter = transitionArtworkPainter,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        AppleMetadata(
                            track,
                            isFavorite,
                            showThumbnail = showInlineLyrics,
                            isSkipNext = isSkipNext,
                            onMore = { app.openTrackMenu(track, playback.queue) },
                            onFavorite = app::toggleFavoriteCurrent,
                            onFullscreen = { isFullScreen = !isFullScreen },
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            AppleProgressSlider(
                value = playback.positionMs,
                valueRangeEnd = playback.durationMs.coerceAtLeast(track.durationMs),
                onValueChangeFinished = app.playback::seekTo,
                modifier = Modifier.padding(horizontal = 32.dp),
                isLossless = track.isLossless,
            )
            Spacer(Modifier.height(12.dp))
            // In fullscreen lyrics everything below the seekbar slides down
            // and shrinks away (Echo Player.kt:2281-2284).
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppleTransport(
                        isPlaying = playback.isPlaying,
                        onPrevious = { haptic(); app.playback.previous() },
                        onPlayPause = { haptic(); app.playback.togglePlayPause() },
                        onNext = { haptic(); app.playback.next() },
                    )
                    Spacer(Modifier.height(8.dp))
                    AppleVolumeSlider(modifier = Modifier.padding(horizontal = 32.dp))
                    AppleBottomActions(
                        app = app,
                        onQueue = {
                            showInlineLyrics = false
                            isFullScreen = false
                            showQueue = !showQueue
                        },
                        onOutput = { showAudioOutput = true },
                        onTimer = { showSleepTimer = true },
                        onLyrics = {
                            showQueue = false
                            showInlineLyrics = !showInlineLyrics
                        },
                    )
                }
            }
            }
        }
    }

    if (showAudioOutput) {
        AudioDevicesSheet(
            app = app,
            onDismiss = { showAudioOutput = false },
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            app = app,
            onDismiss = { showSleepTimer = false },
        )
    }
}

/** Blurred Apple Music background, crossfaded when the track changes. */
@Composable
private fun AppleArtworkBackground(
    track: Track,
    queueVisible: Boolean,
) {
    val artwork = track.artworkUri ?: albumArtUri(track.albumId)
    val blurRadius by animateDpAsState(
        targetValue = if (queueVisible) 112.dp else 150.dp,
        animationSpec = tween(
            durationMillis = 350,
            easing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f),
        ),
        label = "appleQueueBlur",
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = artwork,
            transitionSpec = { fadeIn(tween(1200)) togetherWith fadeOut(tween(1200)) },
            label = "appleMusicBackground",
        ) { source ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(source)
                        .size(128, 128)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        // Let the large blur bleed beyond the screen instead of
                        // exposing a sharp rectangle while the player collapses.
                        .fillMaxSize()
                        .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.4f),
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Inline lyrics that replace the artwork carousel while toggled, exactly like
 * Echo's InlineLyricsView (Player.kt:2879+): word-karaoke lyrics render inside
 * the player over the blurred background — no separate screen.
 */
@Composable
private fun AppleInlineLyrics(
    app: AppViewModel,
    track: Track,
    vm: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory),
) {
    val lyricsState by vm.lyricsState.collectAsStateWithLifecycle()
    LaunchedEffect(track.id) { vm.loadLyrics(track) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = lyricsState) {
            LyricsState.Loading -> CircularProgressIndicator(color = Color.White)
            is LyricsState.Found -> {
                val entries = remember(state.lyrics) {
                    if (state.isSynced) LyricsParser.parseLyrics(state.lyrics) else emptyList()
                }
                if (entries.isEmpty()) {
                    ApplePlainLyrics(state.lyrics, modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    AppleLyrics(
                        entries = entries,
                        positionProvider = app.playback::currentPositionMs,
                        onSeek = app.playback::seekTo,
                    )
                }
            }
            LyricsState.NotFound, LyricsState.Offline -> Text(
                if (lyricsState == LyricsState.Offline) "Lyrics unavailable" else "No lyrics found",
                color = Color.White.copy(.65f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AppleNowPlayingHeader(track: Track, isSkipNext: Boolean = true) {
    // Accord-style Header with directional album title slide on track change
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
        ) {
            Text("Now Playing", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            AnimatedContent(
                targetState = track.id to track.displayAlbum,
                transitionSpec = {
                    val accordEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                    val offset = 100
                    if (isSkipNext) {
                        (slideInHorizontally(initialOffsetX = { offset }, animationSpec = tween(380, easing = accordEasing)) + fadeIn(tween(380))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -offset }, animationSpec = tween(380, easing = accordEasing)) + fadeOut(tween(380)))
                    } else {
                        (slideInHorizontally(initialOffsetX = { -offset }, animationSpec = tween(380, easing = accordEasing)) + fadeIn(tween(380))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { offset }, animationSpec = tween(380, easing = accordEasing)) + fadeOut(tween(380)))
                    }
                },
                label = "NowPlayingAnimation",
            ) { (_, text) ->
                if (text.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Swipeable artwork carousel: previous/current/next covers in a snapping
 * LazyRow; off-center items shrink to 0.85x and fade to 0.3 alpha, settling
 * on a neighbour skips the track, double-tap seeks ±5s by screen thirds
 * (Echo Thumbnail.kt).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.AppleArtworkCarousel(
    app: AppViewModel,
    playback: com.gorilla.music.playback.PlaybackState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    transitionArtworkPainter: AsyncImagePainter,
    modifier: Modifier = Modifier,
) {
    val track = playback.current ?: return

    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showSeekEffect) {
        if (showSeekEffect) {
            delay(1000)
            showSeekEffect = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .pointerInput(track.id) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val now = System.currentTimeMillis()
                        skipMultiplier = if (now - lastTapTime < 1000) skipMultiplier + 1 else 1
                        lastTapTime = now
                        val skipAmount = 5000 * skipMultiplier

                        val position = app.playback.currentPositionMs()
                        val duration = playback.durationMs
                        when {
                            offset.x < size.width * 0.33f -> {
                                app.playback.seekTo((position - skipAmount).coerceAtLeast(0))
                                seekDirection = "-${skipAmount / 1000}s"
                                showSeekEffect = true
                            }
                            offset.x > size.width * 0.66f -> {
                                app.playback.seekTo((position + skipAmount).coerceAtMost(duration))
                                seekDirection = "+${skipAmount / 1000}s"
                                showSeekEffect = true
                            }
                            else -> app.playback.togglePlayPause()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val artworkSize = minOf(maxWidth, maxHeight)
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            fallbackTitle = track.title,
            fallbackSubtitle = track.displayArtist,
            showLoadingPlaceholder = false,
            painterOverride = transitionArtworkPainter,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .size(artworkSize)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState("apple_queue_art_${track.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    boundsTransform = { _, _ ->
                        tween(
                            durationMillis = 350,
                            easing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f),
                        )
                    },
                    zIndexInOverlay = 2f,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp)),
                ),
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AppleMetadata(
    track: Track,
    isFavorite: Boolean,
    showThumbnail: Boolean,
    isSkipNext: Boolean = true,
    onMore: () -> Unit,
    onFavorite: () -> Unit,
    onFullscreen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = showThumbnail,
            label = "ThumbnailAnimation",
        ) { show ->
            if (show) {
                Row {
                    AlbumArt(
                        albumId = track.albumId,
                        artworkUri = track.artworkUri,
                        fallbackTitle = track.title,
                        fallbackSubtitle = track.displayArtist,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                }
            } else {
                Spacer(Modifier.width(0.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    (fadeIn(tween(250, delayMillis = 50)) + slideInVertically { it / 3 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                },
                label = "appleMetadataTransition",
            ) { _ ->
                Column {
                    Text(
                        track.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.displayArtist,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                            .padding(end = 12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(targetState = showThumbnail, label = "DownloadButton") { showLyrics ->
            if (showLyrics) {
                AppleCircleButton(icon = R.drawable.fullscreen, description = "Fullscreen lyrics", onClick = onFullscreen)
            } else {
                AppleCircleButton(icon = R.drawable.more_vert, description = "Track options", onClick = onMore)
            }
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(targetState = showThumbnail, label = "LikeButton") { showLyrics ->
            if (showLyrics) {
                AppleCircleButton(icon = R.drawable.more_horiz, description = "Track options", onClick = onMore)
            } else {
                AppleCircleButton(
                    icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                    description = "Favorite",
                    onClick = onFavorite,
                )
            }
        }
    }
}

/** 40dp circular translucent-filled icon button (Echo's classic Apple buttons). */
@Composable
internal fun AppleCircleButton(
    @DrawableRes icon: Int,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Thumbless progress slider whose track springs from 10dp to 16dp while
 * touched (Echo's SliderStyle.SLIM used by the Apple design).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleProgressSlider(
    value: Long,
    valueRangeEnd: Long,
    onValueChangeFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLossless: Boolean = false,
) {
    val duration = valueRangeEnd.coerceAtLeast(1L)
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isDragged || isPressed
    val trackHeight by animateDpAsState(
        targetValue = if (isActive) 16.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "trackHeight",
    )

    Column(modifier.fillMaxWidth()) {
        Slider(
            value = (sliderPosition ?: value).toFloat().coerceIn(0f, duration.toFloat()),
            valueRange = 0f..duration.toFloat(),
            onValueChange = { sliderPosition = it.toLong() },
            onValueChangeFinished = {
                sliderPosition?.let(onValueChangeFinished)
                sliderPosition = null
            },
            interactionSource = interactionSource,
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    trackHeight = trackHeight,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                    ),
                )
            },
            modifier = Modifier.height(21.dp),
        )
        Spacer(Modifier.height(3.dp))
        // Accord uses a 21dp seek widget followed by this overlaid time/quality
        // frame with a 3dp gap (full_player.xml duration_frame).
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(sliderPosition ?: value), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(formatTime(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            LosslessPill(
                visible = isLossless,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * "Lossless" quality chip shown between the elapsed and remaining time.
 * Ported from Accord's `quality_card` (full_player.xml): 5dp corners, the
 * tertiary+secondary white overlays flattened to one fill, and the glyph and
 * label in the primary overlay colour (#BFFFFFFF).
 *
 * The fade lives here rather than at the call site so that `AnimatedVisibility`
 * resolves to the plain overload — the caller sits in a [Box] nested in a
 * [Column], where the `ColumnScope` extension would otherwise win.
 */
@Composable
private fun LosslessPill(visible: Boolean, modifier: Modifier = Modifier) {
    val fade = tween<Float>(350, easing = CubicBezierEasing(0.4f, 0.2f, 0f, 1f))
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = fade),
        exit = fadeOut(animationSpec = fade),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.19f))
                .clickable(onClick = {})
                .padding(start = 7.5.dp, end = 7.5.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.apple_lossless_seeklogo),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(width = 15.dp, height = 9.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Lossless",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * Apple transport row: fast-rewind / big play-pause / fast-forward using
 * Echo's custom drawables (48dp skips, 72dp play glyph in a 100dp target).
 * The play-pause clip radius springs 36→24dp when playback starts (Echo's
 * playPauseRoundness).
 */
@Composable
internal fun AppleTransport(isPlaying: Boolean, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit) {
    val playPauseInteraction = remember { MutableInteractionSource() }
    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            ResizableIconButton(
                icon = R.drawable.apple_skip_previous,
                color = Color.White,
                modifier = Modifier.size(48.dp).align(Alignment.Center),
                onClick = onPrevious,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(playPauseRoundness))
                .pressScale(playPauseInteraction)
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = null,
                    onClick = onPlayPause,
                ),
        ) {
            Image(
                painter = painterResource(if (isPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                contentDescription = if (isPlaying) "Pause" else "Play",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            ResizableIconButton(
                icon = R.drawable.apple_skip_next,
                color = Color.White,
                modifier = Modifier.size(48.dp).align(Alignment.Center),
                onClick = onNext,
            )
        }
    }
}

/** Rippleless resizable drawable button (Echo ui/component/IconButton.kt). */
@Composable
internal fun ResizableIconButton(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .pressScale(interactionSource)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
    )
}

/**
 * System volume row: mute/up icons that scale 1.15x while dragging, with a
 * springy PlayerSliderTrack (Echo Player.kt volume section). Volume changes
 * from hardware keys arrive via VOLUME_CHANGED_ACTION and animate over 150ms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppleVolumeSlider(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maximum = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Live system volume (Echo Player.kt:515-528).
    val systemVolume by produceState(
        initialValue = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maximum.toFloat(),
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    value = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maximum.toFloat()
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        awaitDispose { context.unregisterReceiver(receiver) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isDragged || isPressed

    // While dragging show the raw drag value; otherwise ease towards the
    // system value so hardware-key changes glide (Echo Player.kt:2573-2591).
    var dragVolume by remember { mutableFloatStateOf(systemVolume) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(systemVolume) {
        if (!isActive) dragVolume = systemVolume
    }
    val animatedSystemVolume by animateFloatAsState(
        targetValue = systemVolume,
        animationSpec = tween(150, easing = LinearOutSlowInEasing),
        label = "animatedSystemVolume",
    )
    val volume = if (isActive) dragVolume else animatedSystemVolume

    val trackHeight by animateDpAsState(
        targetValue = if (isActive) 16.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "volumeTrackHeight",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "volumeIconScale",
    )

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.volume_mute),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(scaleX = iconScale, scaleY = iconScale),
        )
        Spacer(Modifier.width(12.dp))
        Slider(
            value = volume,
            onValueChange = { newVolume ->
                dragVolume = newVolume
                scope.launch(Dispatchers.Default) {
                    val newStep = (newVolume * maximum).roundToInt()
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, newStep, 0)
                }
            },
            modifier = Modifier.weight(1f),
            interactionSource = interactionSource,
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    trackHeight = trackHeight,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
            },
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(scaleX = iconScale, scaleY = iconScale),
        )
    }
}

/**
 * Bottom action row: queue button, connected speaker/sleep-timer pill and
 * the lyrics ("me" quote) button (Echo Queue.kt collapsed Apple bar). The
 * pill is hand-rolled — M3 expressive ToggleButton isn't in this Compose
 * version — using asymmetric corner shapes to mimic the connected group.
 */
@Composable
private fun AppleBottomActions(
    app: AppViewModel,
    onQueue: () -> Unit,
    onOutput: () -> Unit,
    onTimer: () -> Unit,
    onLyrics: () -> Unit,
) {
    val context = LocalContext.current
    val queueInteraction = remember { MutableInteractionSource() }
    val lyricsInteraction = remember { MutableInteractionSource() }
    val timerEnd by app.playback.sleepTimerEndMs.collectAsStateWithLifecycle()
    val sleepTimerEnabled = timerEnd != null

    // Live countdown while the timer runs.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepTimerEnabled) {
        while (sleepTimerEnabled) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Speaker icon swaps to headset while a bluetooth output is connected.
    var isBluetoothConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        while (true) {
            isBluetoothConnected = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            delay(2000)
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onQueue,
            interactionSource = queueInteraction,
            modifier = Modifier
                .wrapContentWidth()
                .pressScale(queueInteraction),
        ) {
            Icon(
                painter = painterResource(R.drawable.apple_queue),
                contentDescription = "Queue",
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.width(120.dp),
        ) {
            ApplePillSegment(
                shape = RoundedCornerShape(topStart = 21.dp, bottomStart = 21.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                checked = false,
                onClick = onOutput,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(
                        if (isBluetoothConnected) R.drawable.headset_applemusic else R.drawable.speaker_apple
                    ),
                    contentDescription = "Audio output",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            ApplePillSegment(
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 21.dp, bottomEnd = 21.dp),
                checked = sleepTimerEnabled,
                onClick = onTimer,
                modifier = Modifier.weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        painter = painterResource(R.drawable.sleep_timer),
                        contentDescription = "Sleep timer",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    timerEnd?.let { end ->
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatTime((end - now).coerceAtLeast(0L)),
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onLyrics,
            interactionSource = lyricsInteraction,
            modifier = Modifier
                .wrapContentWidth()
                .pressScale(lyricsInteraction),
        ) {
            Icon(
                painter = painterResource(R.drawable.apple_music_me),
                contentDescription = "Lyrics",
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ApplePillSegment(
    shape: RoundedCornerShape,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (checked) 0.4f else 0.2f))
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}


internal fun formatTime(ms: Long): String = "%d:%02d".format(ms / 60_000L, (ms / 1_000L) % 60L)
