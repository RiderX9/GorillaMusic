package com.gorilla.music.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.ui.components.MiniPlayer
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.music.ui.theme.LocalTrueLiquidGlassEnabled
import com.gorilla.music.ui.theme.glassContentColor
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import kotlin.math.abs

/**
 * Echo's floating tab bar scroll behavior: scrolling down collapses the expanded
 * tab/accessory stack into a single inline row; scrolling up expands it again.
 */
class FloatingBarScrollConnection internal constructor(
    private val thresholdPx: Float,
    private val flingThresholdPx: Float,
) : NestedScrollConnection {
    var isInline by mutableStateOf(false)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Ignore synthetic fling and overscroll spring-back deltas here. Fast
        // swipes are resolved once in onPreFling instead of repeatedly toggling
        // the bar as a list settles at an edge.
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        val delta = available.y
        if ((accumulatedScroll > 0f && delta < 0f) || (accumulatedScroll < 0f && delta > 0f)) {
            accumulatedScroll = 0f
        }
        accumulatedScroll += delta
        if (accumulatedScroll <= -thresholdPx && !isInline) {
            isInline = true
            accumulatedScroll = 0f
        } else if (accumulatedScroll >= thresholdPx && isInline) {
            isInline = false
            accumulatedScroll = 0f
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val velocityY = available.y
        if (abs(velocityY) >= flingThresholdPx) {
            when {
                velocityY < 0f && !isInline -> isInline = true
                velocityY > 0f && isInline -> isInline = false
            }
        }
        accumulatedScroll = 0f
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        accumulatedScroll = 0f
        return Velocity.Zero
    }
}

@Composable
fun rememberFloatingBarScrollConnection(
    threshold: Dp = 50.dp,
    flingThreshold: Dp = 300.dp,
): FloatingBarScrollConnection {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val flingThresholdPx = with(LocalDensity.current) { flingThreshold.toPx() }
    return remember(thresholdPx, flingThresholdPx) {
        FloatingBarScrollConnection(thresholdPx, flingThresholdPx)
    }
}

/**
 * Compact landscape navigation modeled after Echo's NavigationRail. The player
 * accessory is hosted separately by GorillaRoot so the rail stays narrow.
 */
@Composable
fun GlassNavigationRail(
    current: Destination,
    onSelect: (Destination) -> Unit,
    dynamicThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val liquidGlass = LocalTrueLiquidGlassEnabled.current
    val glassColor = glassContentColor()

    LiquidGlassSurface(
        depth = GlassDepth.HIGH,
        shape = RoundedCornerShape(28.dp),
        surfaceColor = appColors.bgSurface.copy(alpha = if (liquidGlass) 0.40f else 1f),
        border = true,
        shadow = appColors.isDark,
        backdrop = LocalLiquidGlassContentBackdrop.current,
        modifier = modifier.width(64.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Destination.bottomBar.forEach { destination ->
                val selected = current == destination
                val dynamicSelectedColor = if (appColors.isDark) Color.White else Color.Black
                val tint = when {
                    liquidGlass && selected -> glassColor
                    liquidGlass -> glassColor.copy(alpha = 0.65f)
                    dynamicThemeEnabled && selected -> dynamicSelectedColor
                    selected -> accent
                    else -> appColors.textSecondary
                }
                val animatedTint by animateColorAsState(
                    targetValue = tint,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "${destination.route}RailColor",
                )
                val selectionColor by animateColorAsState(
                    targetValue = when {
                        !selected -> Color.Transparent
                        dynamicThemeEnabled && !liquidGlass ->
                            dynamicSelectedColor.copy(alpha = if (appColors.isDark) 0.18f else 0.10f)
                        else -> tint.copy(alpha = 0.14f)
                    },
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "${destination.route}RailSelection",
                )

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(selectionColor)
                        .instantClickable(pressedScale = 0.92f) {
                            onSelect(destination)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = animatedTint,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
        }
    }
}

/**
 * Echo Music's iOS-style bottom bar adapted to Gorilla's destinations. Search is
 * the standalone circular tab. When a track is loaded, the mini-player is an
 * expanded pill above the tabs and becomes the middle accessory in inline mode.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassNavigationBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    scrollConnection: FloatingBarScrollConnection,
    backgroundStyle: ArtBackgroundStyle,
    dynamicThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var lastContentDestination by remember { mutableStateOf(Destination.Home) }
    LaunchedEffect(current) {
        if (current != Destination.Search) {
            lastContentDestination = current
        }
    }

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = scrollConnection.isInline,
            transitionSpec = {
                (
                    fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        )
                ).togetherWith(
                    fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                        ),
                )
                    .using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ ->
                                tween(360, easing = FastOutSlowInEasing)
                            },
                        ),
                    )
            },
            contentAlignment = Alignment.BottomCenter,
            label = "echoFloatingBar",
            modifier = Modifier.animateContentSize(
                animationSpec = tween(360, easing = FastOutSlowInEasing),
            ),
        ) { inline ->
            if (inline) {
                InlineBar(
                    current = current,
                    inlineDestination = lastContentDestination,
                    onSelect = onSelect,
                    track = track,
                    isPlaying = isPlaying,
                    progress = progress,
                    onOpenPlayer = onOpenPlayer,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    backgroundStyle = backgroundStyle,
                    dynamicThemeEnabled = dynamicThemeEnabled,
                    onExpand = scrollConnection::expand,
                    animatedVisibilityScope = this@AnimatedContent,
                )
            } else {
                ExpandedBar(
                    current = current,
                    onSelect = onSelect,
                    track = track,
                    isPlaying = isPlaying,
                    progress = progress,
                    onOpenPlayer = onOpenPlayer,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    backgroundStyle = backgroundStyle,
                    dynamicThemeEnabled = dynamicThemeEnabled,
                    animatedVisibilityScope = this@AnimatedContent,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ExpandedBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    backgroundStyle: ArtBackgroundStyle,
    dynamicThemeEnabled: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
    ) {
        if (track != null) {
            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                progress = progress,
                onExpand = onOpenPlayer,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                backgroundStyle = backgroundStyle,
                dynamicThemeEnabled = dynamicThemeEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 340.dp)
                    .sharedElement(
                        state = rememberSharedContentState("playerAccessory"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            TabGroup(
                current = current,
                onSelect = onSelect,
                dynamicThemeEnabled = dynamicThemeEnabled,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            StandaloneSearch(
                selected = current == Destination.Search,
                onClick = { onSelect(Destination.Search) },
                dynamicThemeEnabled = dynamicThemeEnabled,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InlineBar(
    current: Destination,
    inlineDestination: Destination,
    onSelect: (Destination) -> Unit,
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    backgroundStyle: ArtBackgroundStyle,
    dynamicThemeEnabled: Boolean,
    onExpand: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        InlineSelectedTab(
            destination = inlineDestination,
            onClick = {
                onExpand()
                onSelect(inlineDestination)
            },
            dynamicThemeEnabled = dynamicThemeEnabled,
            animatedVisibilityScope = animatedVisibilityScope,
        )
        if (track != null) {
            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                progress = progress,
                onExpand = onOpenPlayer,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                backgroundStyle = backgroundStyle,
                dynamicThemeEnabled = dynamicThemeEnabled,
                compact = true,
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        state = rememberSharedContentState("playerAccessory"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
            )
        } else {
            Box(Modifier.weight(1f))
        }
        StandaloneSearch(
            selected = current == Destination.Search,
            onClick = { onSelect(Destination.Search) },
            dynamicThemeEnabled = dynamicThemeEnabled,
            modifier = Modifier.size(52.dp),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.TabGroup(
    current: Destination,
    onSelect: (Destination) -> Unit,
    dynamicThemeEnabled: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val tabs = Destination.bottomBar.filter { it != Destination.Search }
    GlassContainer(
        modifier = Modifier.sharedElement(
            state = rememberSharedContentState("tabGroup"),
            animatedVisibilityScope = animatedVisibilityScope,
            zIndexInOverlay = 1f,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(4.dp),
        ) {
            tabs.forEach { destination ->
                ExpandedTab(
                    destination = destination,
                    selected = current == destination,
                    onClick = { onSelect(destination) },
                    dynamicThemeEnabled = dynamicThemeEnabled,
                    sharedTransitionScope = this@TabGroup,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedTab(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    dynamicThemeEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val liquidGlass = LocalTrueLiquidGlassEnabled.current
    val glassColor = glassContentColor()
    val dynamicSelectedColor = if (appColors.isDark) Color.White else Color.Black
    val contentColor = when {
        liquidGlass && selected -> glassColor
        liquidGlass -> glassColor.copy(alpha = 0.65f)
        dynamicThemeEnabled && selected -> dynamicSelectedColor
        selected -> accent
        else -> appColors.textSecondary
    }
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "${destination.route}TabColor",
    )
    val selectionColor by animateColorAsState(
        targetValue = when {
            !selected -> Color.Transparent
            dynamicThemeEnabled && !liquidGlass ->
                dynamicSelectedColor.copy(alpha = if (appColors.isDark) 0.18f else 0.10f)
            else -> contentColor.copy(alpha = 0.14f)
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "${destination.route}TabSelection",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = tween(200, easing = LinearOutSlowInEasing),
        label = "${destination.route}TabScale",
    )
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .instantClickable(pressedScale = 0.92f) {
                if (!selected) haptic()
                onClick()
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = selectionColor,
                shape = RoundedCornerShape(24.dp),
            )
            .width(54.dp)
            .padding(vertical = 6.dp),
    ) {
        with(sharedTransitionScope) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = animatedContentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .then(
                        if (selected) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(
                                    "tab#${destination.route}-icon"
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                zIndexInOverlay = 1f,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Text(
            text = destination.label,
            color = animatedContentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.InlineSelectedTab(
    destination: Destination,
    onClick: () -> Unit,
    dynamicThemeEnabled: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    GlassContainer(
        modifier = Modifier
            .size(52.dp)
            .sharedElement(
                state = rememberSharedContentState("tabGroup"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f,
            ),
    ) {
        val interaction = remember { MutableInteractionSource() }
        val appColors = LocalAppColors.current
        val tint = if (LocalTrueLiquidGlassEnabled.current) {
            glassContentColor()
        } else if (dynamicThemeEnabled) {
            if (appColors.isDark) Color.White else Color.Black
        } else {
            LocalDynamicColors.current.accent
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .instantClickable(pressedScale = 0.92f) {
                    onClick()
                },
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = tint,
                modifier = Modifier.sharedElement(
                    state = rememberSharedContentState(
                        "tab#${destination.route}-icon"
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    zIndexInOverlay = 1f,
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.StandaloneSearch(
    selected: Boolean,
    onClick: () -> Unit,
    dynamicThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val liquidGlass = LocalTrueLiquidGlassEnabled.current
    val appColors = LocalAppColors.current
    val dynamicSelectedColor = if (appColors.isDark) Color.White else Color.Black
    val tint = when {
        liquidGlass -> glassContentColor()
        dynamicThemeEnabled && selected -> dynamicSelectedColor
        selected -> LocalDynamicColors.current.accent
        else -> appColors.textSecondary
    }
    GlassContainer(
        shape = CircleShape,
        modifier = modifier.sharedElement(
            state = rememberSharedContentState("standaloneSearch"),
            animatedVisibilityScope = animatedVisibilityScope,
            zIndexInOverlay = 1f,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .instantClickable(pressedScale = 0.92f) {
                    if (!selected) haptic()
                    onClick()
                }
                .background(
                    color = when {
                        !selected -> Color.Transparent
                        dynamicThemeEnabled && !liquidGlass ->
                            dynamicSelectedColor.copy(alpha = if (appColors.isDark) 0.18f else 0.10f)
                        else -> tint.copy(alpha = 0.14f)
                    },
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = Destination.Search.icon,
                contentDescription = Destination.Search.label,
                tint = tint,
            )
        }
    }
}

@Composable
private fun GlassContainer(
    modifier: Modifier,
    shape: androidx.compose.foundation.shape.RoundedCornerShape = RoundedCornerShape(percent = 50),
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val liquidGlass = LocalTrueLiquidGlassEnabled.current
    LiquidGlassSurface(
        depth = GlassDepth.HIGH,
        shape = shape,
        surfaceColor = appColors.bgSurface.copy(alpha = if (liquidGlass) 0.40f else 1f),
        border = true,
        shadow = appColors.isDark,
        backdrop = LocalLiquidGlassContentBackdrop.current,
        modifier = modifier,
        content = content,
    )
}
