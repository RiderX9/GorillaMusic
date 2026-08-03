package com.gorilla.music.ui

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.gorilla.music.ui.components.EqualizerSheet
import com.gorilla.music.ui.components.MiniPlayer
import com.gorilla.music.ui.components.TrackContextMenuHost
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.ui.nav.Destination
import com.gorilla.music.ui.nav.GlassNavigationBar
import com.gorilla.music.ui.nav.GlassNavigationRail
import com.gorilla.music.ui.nav.GorillaNavHost
import com.gorilla.music.ui.nav.rememberFloatingBarScrollConnection
import com.gorilla.music.ui.screens.nowplaying.LyricsSheet
import com.gorilla.music.ui.screens.nowplaying.NowPlayingScreen
import com.gorilla.music.ui.screens.nowplaying.TrackInfoSheet
import com.gorilla.music.ui.screens.nowplaying.EditTagsSheet
import com.gorilla.music.ui.screens.permission.PermissionGate
import com.gorilla.music.ui.theme.GorillaBackgroundHost
import com.gorilla.music.ui.theme.DesignTokens
import com.gorilla.music.ui.theme.SpringSpecs
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.music.ui.theme.renderToHardwareTextureAndroid
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.LayerBackdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.layerBackdrop
import com.gorilla.music.ui.liquidglass.backdrop.backdrops.rememberLayerBackdrop

/**
 * Root composable: background wash, permission gate, NavHost for the five tabs, the
 * persistent mini player + glass nav bar, and the full-screen Now Playing overlay
 * that springs up from the mini player and collapses back down on swipe.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GorillaRoot(app: AppViewModel) {
    val permission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    LaunchedEffect(permission.status.isGranted) {
        app.onPermissionResult(permission.status.isGranted)
    }

    val playback by app.playbackState.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val sheetTrack by app.sheetTrack.collectAsStateWithLifecycle()
    val infoTrack by app.infoTrack.collectAsStateWithLifecycle()
    val lyricsTrack by app.lyricsTrack.collectAsStateWithLifecycle()
    val editTagsTrack by app.editTagsTrack.collectAsStateWithLifecycle()
    val equalizerOpen by app.equalizerOpen.collectAsStateWithLifecycle()

    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    val nowPlayingVisible = nowPlayingOpen && playback.hasTrack
    val appColors = LocalAppColors.current
    val view = LocalView.current

    // Edge-to-edge bars are transparent, so icon contrast must follow what the
    // app is drawing underneath them. Light screens use dark system icons; the
    // full-screen players remain dark and therefore keep light icons.
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val useDarkIcons = !appColors.isDark && !nowPlayingVisible
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    // --- Chrome motion (Echo parity) --------------------------------------
    // ONE continuous animatable drives the always-composed nav bar + mini
    // player, exactly like Echo's single BottomSheet animatable. No
    // AnimatedVisibility handoff means nothing to pop in/out on release.
    //   0f = chrome fully visible (player collapsed/closed)
    //   1f = chrome hidden below the screen (player fully expanded)
    val chromeHidden = remember { androidx.compose.animation.core.Animatable(0f) }
    // True only while the user's finger is actively dragging the player. While
    // true the chrome snaps to the finger; on release the settle spring owns it.
    var playerDragging by remember { mutableStateOf(false) }
    // Live finger progress from the player: 0 = expanded, 1 = swiped off bottom.
    var playerDragProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // Settle: when the player opens/closes and no drag is active, spring the
    // chrome to hidden (open) or visible (closed). This is the single owner of
    // tap-open, back-button, and post-fling motion.
    LaunchedEffect(nowPlayingVisible, playerDragging) {
        if (playerDragging) return@LaunchedEffect
        chromeHidden.animateTo(
            if (nowPlayingVisible) 1f else 0f,
            spring(dampingRatio = 0.9f, stiffness = 350f),
        )
    }
    // Follow the finger 1:1 while dragging. chromeHidden = 1 - progress:
    // expanded (progress 0) -> hidden (1); off-bottom (progress 1) -> visible (0).
    LaunchedEffect(playerDragProgress, playerDragging) {
        if (playerDragging) chromeHidden.snapTo(1f - playerDragProgress)
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        app.showSnackbar.collect { message ->
            snackbarHost.showSnackbar(message)
        }
    }
    val contentBackdrop = rememberLayerBackdrop()

    // Single shared animated background for the whole app. Full intensity under Now
    // Playing, a faint wash under every other screen — same colors, same clock.
    SharedTransitionLayout {
        GorillaBackgroundHost(nowPlayingVisible = nowPlayingVisible) {
            Box(Modifier.fillMaxSize()) {
            if (!permission.status.isGranted) {
                PermissionGate(onRequest = { permission.launchPermissionRequest() })
            } else {
                MainShell(
                    app = app,
                    nowPlayingOpen = nowPlayingOpen,
                    nowPlayingVisible = nowPlayingVisible,
                    contentBackdrop = contentBackdrop,
                    backgroundStyle = settings?.artBackground ?: ArtBackgroundStyle.LIVE_MESH,
                    dynamicThemeEnabled = settings?.dynamicTheme == true,
                    chromeHidden = chromeHidden.value,
                    onOpenNowPlaying = { nowPlayingOpen = true },
                    onCollapseNowPlaying = { nowPlayingOpen = false },
                    onDragProgress = { playerDragProgress = it },
                    onDraggingChange = { playerDragging = it },
                )
            }

            if (nowPlayingOpen) {
                BackHandler {
                    nowPlayingOpen = false
                }
            }

            // Now Playing is no longer a full-screen overlay stacked above the
            // chrome — it lives inside MainShell, drawn BELOW the nav bar + mini
            // player, so on collapse it slides behind them (Echo parity). See
            // MainShell for the sheet slot.

            // Global Sheets (Bug 1) — hosted at root so they cover the nav bar + mini player.
            TrackContextMenuHost(
                app = app,
                menuTrack = sheetTrack,
                onDismiss = { app.closeTrackMenu() },
                context = app.sheetContext,
            )

            AnimatedVisibility(
                visible = infoTrack != null,
                enter = slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = DesignTokens.SpringBouncy
                ),
            ) {
                infoTrack?.let { track ->
                    TrackInfoSheet(
                        track = track,
                        onDismiss = { app.closeTrackInfo() },
                        onEditTags = {
                            app.closeTrackInfo()
                            app.openEditTags(track)
                        },
                        onPlayNext = {
                            app.playNext(track)
                            app.closeTrackInfo()
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = equalizerOpen,
                enter = slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = DesignTokens.SpringBouncy
                ),
            ) {
                EqualizerSheet(onDismiss = { app.closeEqualizer() })
            }

            AnimatedVisibility(
                visible = lyricsTrack != null,
                enter = slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = DesignTokens.SpringBouncy
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                lyricsTrack?.let { track ->
                    LyricsSheet(app = app, track = track, onDismiss = { app.closeLyrics() })
                }
            }

            AnimatedVisibility(
                visible = editTagsTrack != null,
                enter = slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    ),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = DesignTokens.SpringBouncy
                ),
            ) {
                editTagsTrack?.let { track ->
                    EditTagsSheet(
                        track = track,
                        onDismiss = { app.closeEditTags() },
                        onSave = { title, artist, album, genre, year, lyrics ->
                            app.saveTags(track, title, artist, album, genre, year, lyrics)
                            app.closeEditTags()
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 180.dp),
            )
        }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.MainShell(
    app: AppViewModel,
    nowPlayingOpen: Boolean,
    nowPlayingVisible: Boolean,
    contentBackdrop: LayerBackdrop,
    backgroundStyle: ArtBackgroundStyle,
    dynamicThemeEnabled: Boolean,
    // 0f = chrome fully visible, 1f = chrome hidden below the screen. Host-owned
    // single source of truth (Echo's BottomSheet progress) — the bar reads it
    // directly, so there's no AnimatedVisibility handoff to pop on release.
    chromeHidden: Float,
    onOpenNowPlaying: () -> Unit,
    onCollapseNowPlaying: () -> Unit,
    onDragProgress: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStack?.destination?.route)

    val playback by app.playbackState.collectAsStateWithLifecycle()
    val floatingBarScrollConnection = rememberFloatingBarScrollConnection()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides contentBackdrop) {
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(floatingBarScrollConnection)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .layerBackdrop(contentBackdrop)
                    // The backdrop must contain a material field of its own.
                    // Capturing only the screen content leaves a black/empty
                    // source behind the translucent liquid-glass surfaces.
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // The backdrop records this subtree. Glass surfaces inside it must
                // sample the background backdrop instead of recording themselves.
                CompositionLocalProvider(LocalLiquidGlassContentBackdrop provides null) {
                    GorillaNavHost(
                        navController = navController,
                        app = app,
                        onOpenNowPlaying = onOpenNowPlaying,
                        contentPadding = if (isLandscape) {
                            PaddingValues(
                                bottom = if (playback.current != null) 80.dp else 16.dp,
                            )
                        } else {
                            // Expanded Echo chrome: 64dp player + 8dp gap + 64dp tabs.
                            PaddingValues(bottom = 176.dp)
                        },
                        modifier = if (isLandscape) {
                            Modifier.padding(start = 88.dp)
                        } else {
                            Modifier
                        },
                    )

                    // Now Playing sheet — recorded INTO contentBackdrop (drawn
                    // after the NavHost, so it sits ABOVE screen content) so the
                    // bar + mini player liquid glass actually sample the player
                    // surface behind them. The nav bar is drawn later, OUTSIDE
                    // this backdrop Box, so it stays on top and the sheet
                    // collapses behind it (Echo's `Box{ player; …; navBar }`).
                    // `provides null` keeps player-internal glass from trying to
                    // record the very backdrop it lives in.
                    if (nowPlayingOpen) {
                        BackHandler { onCollapseNowPlaying() }
                    }
                    AnimatedVisibility(
                        visible = nowPlayingVisible,
                        enter = fadeIn(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 260,
                                easing = androidx.compose.animation.core.LinearOutSlowInEasing
                            )
                        ),
                        exit = fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 260,
                                easing = androidx.compose.animation.core.FastOutLinearInEasing
                            )
                        ),
                        modifier = Modifier.renderToHardwareTextureAndroid()
                    ) {
                        NowPlayingScreen(
                            app = app,
                            onCollapse = onCollapseNowPlaying,
                            onDragProgress = onDragProgress,
                            onDraggingChange = onDraggingChange,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }
                }
            }

            // Echo parity: the nav bar + mini player are ALWAYS composed and
            // slid off the bottom by `chromeHidden` (0 = visible, 1 = hidden).
            // A single host animatable owns every transition — drag, fling,
            // tap-open, back — so there is no AnimatedVisibility enter/exit to
            // pop in front of the collapsing player. The bar rises/falls as one
            // continuous function of the player position, both under the finger
            // and during the release spring.
            val onSelectDestination: (Destination) -> Unit = { dest ->
                if (dest.route != current.route) {
                    if (dest == Destination.Home) {
                        val returnedHome = navController.popBackStack(
                            route = Destination.Home.route,
                            inclusive = false,
                        )
                        if (!returnedHome) {
                            navController.navigate(Destination.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(dest.route) {
                            popUpTo(Destination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }

            if (isLandscape) {
                var railWidthPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .onSizeChanged { railWidthPx = it.width }
                        .offset {
                            IntOffset(
                                x = -(chromeHidden.coerceIn(0f, 1f) * railWidthPx).toInt(),
                                y = 0,
                            )
                        }
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                ) {
                    GlassNavigationRail(
                        current = current,
                        onSelect = onSelectDestination,
                        dynamicThemeEnabled = dynamicThemeEnabled,
                    )
                }

                playback.current?.let { track ->
                    var miniPlayerHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .onSizeChanged { miniPlayerHeightPx = it.height }
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (chromeHidden.coerceIn(0f, 1f) * miniPlayerHeightPx).toInt(),
                                )
                            }
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        MiniPlayer(
                            track = track,
                            isPlaying = playback.isPlaying,
                            progress = playback.progress,
                            onExpand = onOpenNowPlaying,
                            onPlayPause = { app.playback.togglePlayPause() },
                            onNext = { app.playback.next() },
                            onPrevious = { app.playback.previous() },
                            backgroundStyle = backgroundStyle,
                            dynamicThemeEnabled = dynamicThemeEnabled,
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                var barHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        // Measure + move the OUTERMOST node so barHeightPx
                        // includes the navigation-bar inset and vertical padding.
                        // Placement-based offset is required here because liquid glass
                        // samples the backdrop using layout coordinates.
                        .onSizeChanged { barHeightPx = it.height }
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (chromeHidden.coerceIn(0f, 1f) * barHeightPx).toInt(),
                            )
                        }
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    GlassNavigationBar(
                        current = current,
                        onSelect = onSelectDestination,
                        track = playback.current,
                        isPlaying = playback.isPlaying,
                        progress = playback.progress,
                        onOpenPlayer = onOpenNowPlaying,
                        onPlayPause = { app.playback.togglePlayPause() },
                        onNext = { app.playback.next() },
                        onPrevious = { app.playback.previous() },
                        scrollConnection = floatingBarScrollConnection,
                        backgroundStyle = backgroundStyle,
                        dynamicThemeEnabled = dynamicThemeEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
