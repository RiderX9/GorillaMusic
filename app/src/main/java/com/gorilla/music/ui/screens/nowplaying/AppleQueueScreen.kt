package com.gorilla.music.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gorilla.music.R
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.settings.RepeatMode
import com.gorilla.music.playback.PlaybackState
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.isActive

private data class AppleQueueEntry(
    val track: Track,
    val key: String,
)

private fun List<Track>.toAppleQueueEntries(): List<AppleQueueEntry> {
    val occurrences = HashMap<Long, Int>()
    return map { track ->
        val occurrence = occurrences.getOrDefault(track.id, 0)
        occurrences[track.id] = occurrence + 1
        AppleQueueEntry(track = track, key = "${track.id}:$occurrence")
    }
}

/** Accord standard DefaultItemAnimator cubic-bezier easing (250ms accelerate-decelerate). */
private val AccordItemAnimatorEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SharedTransitionScope.AppleQueueScreen(
    app: AppViewModel,
    playback: PlaybackState,
    track: Track,
    isFavorite: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    transitionArtworkPainter: AsyncImagePainter,
    onDismiss: () -> Unit,
    compact: Boolean = false,
) {
    val haptic = rememberHaptic()
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val visibleQueue = remember {
        mutableStateListOf<AppleQueueEntry>().apply {
            addAll(playback.queue.toAppleQueueEntries())
        }
    }
    var draggedEntryKey by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var draggedQueueIndex by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    // Accord ItemTouchHelper auto-scroll loop with container-level drag lock
    LaunchedEffect(draggedEntryKey) {
        if (draggedEntryKey != null) {
            var msInBounds = 0L
            var lastTime = System.currentTimeMillis()

            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime).coerceAtLeast(1L)
                lastTime = now

                val baseSpeed = autoScrollSpeed
                if (baseSpeed != 0f) {
                    msInBounds += dt
                    val timeRatio = (msInBounds / 1200f).coerceAtMost(1f)
                    val timeFactor = timeRatio * timeRatio
                    val effectiveSpeed = baseSpeed * timeFactor

                    val scrolled = listState.scrollBy(effectiveSpeed)
                    draggedOffsetY -= scrolled

                    val currentKey = draggedEntryKey ?: break
                    val draggedInfo = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == currentKey }
                    if (draggedInfo != null) {
                        val draggedCenter = draggedInfo.offset + draggedOffsetY + draggedInfo.size / 2f
                        val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { visible ->
                            visible.key != currentKey &&
                                draggedCenter >= visible.offset &&
                                draggedCenter <= visible.offset + visible.size
                        }
                        val from = draggedQueueIndex
                        if (targetInfo != null && from != null) {
                            val to = targetInfo.index
                            if (from != to && from in visibleQueue.indices && to in visibleQueue.indices) {
                                val slotDelta = (from - to) * draggedInfo.size
                                draggedOffsetY += slotDelta
                                visibleQueue.add(to, visibleQueue.removeAt(from))
                                draggedQueueIndex = to
                            }
                        }
                    }
                } else {
                    msInBounds = 0L
                }
                withFrameNanos { }
            }
        } else {
            autoScrollSpeed = 0f
        }
    }

    // Sync player-driven changes only while no drag is active.
    LaunchedEffect(playback.queue, draggedEntryKey) {
        if (draggedEntryKey == null && visibleQueue.map { it.track } != playback.queue) {
            visibleQueue.clear()
            visibleQueue.addAll(playback.queue.toAppleQueueEntries())
        }
    }

    // Keep the playing track visible when this screen opens or playback advances.
    LaunchedEffect(playback.current?.id) {
        val currentIndex = visibleQueue.indexOfFirst { it.track.id == playback.current?.id }
        if (currentIndex in visibleQueue.indices) {
            listState.scrollToItem(currentIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (compact) 16.dp else 32.dp,
                    top = if (compact) 8.dp else 28.dp,
                    end = if (compact) 12.dp else 28.dp,
                    bottom = if (compact) 8.dp else 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                fallbackTitle = track.title,
                fallbackSubtitle = track.displayArtist,
                showLoadingPlaceholder = false,
                painterOverride = transitionArtworkPainter,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
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
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(6.dp)),
                    )
                    .size(if (compact) 48.dp else 60.dp)
                    .instantClickable(pressedScale = 0.94f) {
                        haptic()
                        onDismiss()
                    },
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = track.displayArtist,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = if (compact) 14.sp else 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppleCircleButton(
                icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                description = "Favorite",
                onClick = {
                    haptic()
                    app.toggleFavoriteCurrent()
                },
            )
            Spacer(Modifier.width(10.dp))
            AppleCircleButton(
                icon = R.drawable.more_vert,
                description = "Track options",
                onClick = {
                    haptic()
                    app.openTrackMenu(track, playback.queue)
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QueueModeButton(
                active = playback.shuffle,
                onClick = {
                    haptic()
                    app.playback.toggleShuffle()
                },
                modifier = Modifier.weight(1f),
                compact = compact,
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = Color.White)
            }
            QueueModeButton(
                active = playback.repeat != RepeatMode.OFF,
                enabled = !playback.autoplay,
                onClick = {
                    haptic()
                    app.playback.cycleRepeat()
                },
                modifier = Modifier.weight(1f),
                compact = compact,
            ) {
                Icon(
                    imageVector = if (playback.repeat == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = when (playback.repeat) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat queue"
                        RepeatMode.ONE -> "Repeat current track"
                    },
                    tint = Color.White,
                )
            }
            QueueModeButton(
                active = playback.autoplay,
                enabled = playback.repeat != RepeatMode.ALL,
                onClick = {
                    haptic()
                    app.toggleAutoplay()
                },
                modifier = Modifier.weight(1f),
                compact = compact,
            ) {
                Icon(
                    Icons.Rounded.AllInclusive,
                    contentDescription = "Autoplay similar music",
                    tint = Color.White,
                )
            }
        }

        Text(
            text = "Next in Queue",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = if (compact) 16.dp else 32.dp,
                vertical = if (compact) 8.dp else 18.dp,
            ),
        )

        // Container-level gesture scope: guarantees zero gesture cancellation during scrolling/reordering
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                offset.y >= item.offset && offset.y <= item.offset + item.size
                            }
                            if (hit != null && hit.index in visibleQueue.indices) {
                                haptic()
                                val entry = visibleQueue[hit.index]
                                draggedEntryKey = entry.key
                                dragStartIndex = hit.index
                                draggedQueueIndex = hit.index
                                draggedOffsetY = 0f
                            }
                        },
                        onDragEnd = {
                            autoScrollSpeed = 0f
                            val from = dragStartIndex
                            val to = draggedQueueIndex
                            draggedEntryKey = null
                            dragStartIndex = null
                            draggedQueueIndex = null
                            draggedOffsetY = 0f
                            if (
                                from != null &&
                                to != null &&
                                from != to &&
                                from in playback.queue.indices &&
                                to in playback.queue.indices
                            ) {
                                app.playback.moveQueueItem(from, to)
                            }
                        },
                        onDragCancel = {
                            autoScrollSpeed = 0f
                            val from = dragStartIndex
                            val to = draggedQueueIndex
                            draggedEntryKey = null
                            dragStartIndex = null
                            draggedQueueIndex = null
                            draggedOffsetY = 0f
                            if (
                                from != null &&
                                to != null &&
                                from != to &&
                                from in playback.queue.indices &&
                                to in playback.queue.indices
                            ) {
                                app.playback.moveQueueItem(from, to)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val currentKey = draggedEntryKey ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            draggedOffsetY += dragAmount.y

                            val draggedInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentKey }
                                ?: return@detectDragGesturesAfterLongPress

                            val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
                            val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()

                            val currentTop = draggedInfo.offset + draggedOffsetY
                            val currentBottom = currentTop + draggedInfo.size

                            val maxDragScroll = with(density) { 20.dp.toPx() }
                            val edgeThreshold = with(density) { 60.dp.toPx() }

                            autoScrollSpeed = when {
                                currentTop < (viewportStart + edgeThreshold) -> {
                                    val outOfBounds = (viewportStart + edgeThreshold - currentTop).coerceAtLeast(0f)
                                    val ratio = (outOfBounds / edgeThreshold).coerceIn(0f, 1f)
                                    -(maxDragScroll * ratio * ratio).coerceAtLeast(-maxDragScroll)
                                }
                                currentBottom > (viewportEnd - edgeThreshold) -> {
                                    val outOfBounds = (currentBottom - (viewportEnd - edgeThreshold)).coerceAtLeast(0f)
                                    val ratio = (outOfBounds / edgeThreshold).coerceIn(0f, 1f)
                                    (maxDragScroll * ratio * ratio).coerceAtMost(maxDragScroll)
                                }
                                else -> 0f
                            }

                            val draggedCenter = currentTop + draggedInfo.size / 2f
                            val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { visible ->
                                visible.key != currentKey &&
                                    draggedCenter >= visible.offset &&
                                    draggedCenter <= visible.offset + visible.size
                            } ?: return@detectDragGesturesAfterLongPress

                            val from = draggedQueueIndex ?: return@detectDragGesturesAfterLongPress
                            val to = targetInfo.index
                            if (from != to && from in visibleQueue.indices && to in visibleQueue.indices) {
                                val slotDelta = (from - to) * draggedInfo.size
                                draggedOffsetY += slotDelta
                                visibleQueue.add(to, visibleQueue.removeAt(from))
                                draggedQueueIndex = to
                            }
                        },
                    )
                },
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = visibleQueue,
                    key = { _, entry -> entry.key },
                    contentType = { _, _ -> "appleQueueTrack" },
                ) { index, entry ->
                    val item = entry.track
                    val isDragging = draggedEntryKey == entry.key

                    val rowModifier = if (isDragging) {
                        Modifier.zIndex(10f)
                    } else if (draggedEntryKey != null) {
                        Modifier
                            .zIndex(0f)
                            .animateItem(
                                fadeInSpec = null,
                                placementSpec = tween(
                                    durationMillis = 250,
                                    easing = AccordItemAnimatorEasing,
                                ),
                                fadeOutSpec = null,
                            )
                    } else {
                        Modifier
                    }

                    AppleQueueRow(
                        item = item,
                        current = item.id == playback.current?.id,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) draggedOffsetY else 0f,
                        modifier = rowModifier,
                        compact = compact,
                        onPlay = {
                            haptic()
                            if (index in playback.queue.indices) {
                                app.playback.skipToQueueItem(index)
                            }
                        },
                        onRemove = {
                            haptic()
                            if (index in playback.queue.indices) {
                                app.playback.removeQueueItem(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueModeButton(
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(if (compact) 40.dp else 46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Color.White.copy(
                    alpha = when {
                        !enabled -> 0.10f
                        active -> 0.34f
                        else -> 0.18f
                    },
                ),
            )
            .pressScale(interactionSource)
            .toggleable(
                value = active,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AppleQueueRow(
    item: Track,
    current: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = AccordItemAnimatorEasing),
        label = "dragScale",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .background(
                if (isDragging) {
                    Color.White.copy(alpha = 0.14f)
                } else if (current) {
                    Color.White.copy(alpha = 0.07f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onPlay)
            .padding(
                horizontal = if (compact) 16.dp else 32.5.dp,
                vertical = if (compact) 3.dp else 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = item.albumId,
            artworkUri = item.artworkUri,
            fallbackTitle = item.title,
            fallbackSubtitle = item.displayArtist,
            shape = RoundedCornerShape(4.dp),
            allowHardware = true,
            crossfade = false,
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .border(0.7.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(if (compact) 12.dp else 18.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White.copy(alpha = if (current) 0.96f else 0.92f),
                fontSize = if (compact) 15.sp else 17.sp,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.displayArtist,
                color = Color.White.copy(alpha = 0.40f),
                fontSize = if (compact) 12.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .instantClickable(pressedScale = 0.90f) {
                    onRemove()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_queue_remove),
                contentDescription = "Remove from queue",
                tint = Color.White.copy(alpha = 0.34f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
