package com.gorilla.music.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.music.R
import com.gorilla.music.data.model.LyricsEntry
import com.gorilla.music.utils.LyricsParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Apple Music style synced lyrics renderer with word-by-word karaoke fill.
 * Ported from Echo Music's ui/component/Lyrics.kt list scaffolding (echomusic_1
 * animation style) and ui/component/EchoMusicLyrics.kt line renderer, stripped
 * of Hilt, romanization/translation and selection mode.
 */
private const val LYRICS_PREVIEW_TIME_MS = 2000L

// Echo's defaults: LyricsTextSizeKey 24f, LyricsLineSpacingKey 1.3f.
private const val LYRICS_TEXT_SIZE = 24f
private const val LYRICS_LINE_SPACING = 1.3f
private const val INACTIVE_WORD_ALPHA = 0.45f

internal fun syncedWordProgress(
    isActiveLine: Boolean,
    lineRelativeTimeMs: Long,
    wordStartMs: Long,
    wordEndMs: Long,
): Float {
    if (!isActiveLine) return 0f

    val durationMs = (wordEndMs - wordStartMs).coerceAtLeast(1L)
    return when {
        lineRelativeTimeMs <= wordStartMs -> 0f
        lineRelativeTimeMs >= wordEndMs -> 1f
        else -> (lineRelativeTimeMs - wordStartMs).toFloat() / durationMs
    }
}

/** Top+bottom transparency fade (Echo ui/utils/FadingEdge.kt). */
private fun Modifier.fadingEdge(vertical: Dp) = graphicsLayer(alpha = 0.99f)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = vertical.toPx(),
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - vertical.toPx(),
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun AppleLyrics(
    entries: List<LyricsEntry>,
    positionProvider: () -> Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    sliderPositionProvider: () -> Long? = { null },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    accent: Color = Color.White,
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    var previousLineIndex by remember { mutableIntStateOf(-1) }
    var isSeeking by remember { mutableStateOf(false) }
    var lastPreviewTime by remember { mutableLongStateOf(0L) }
    var isAnimating by remember { mutableStateOf(false) }
    var initialScrollDone by remember { mutableStateOf(false) }
    var shouldScrollToFirstLine by remember { mutableStateOf(true) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    // Per-frame position loop (Echo Lyrics.kt:644-658).
    LaunchedEffect(entries) {
        while (isActive) {
            withFrameMillis { }
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: positionProvider()
            currentPlaybackPosition = position
            currentLineIndex = LyricsParser.findCurrentLineIndex(entries, position)
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LYRICS_PREVIEW_TIME_MS)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
        if (isAnimating) return
        isAnimating = true
        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            if (itemInfo != null) {
                val viewportHeight =
                    lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - center
                if (abs(offset) > 10) {
                    lazyListState.animateScrollBy(
                        value = offset.toFloat(),
                        animationSpec = tween(durationMillis = duration),
                    )
                }
            } else {
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }

    // Auto-scroll follows the active line; pauses while the user browses
    // (Echo Lyrics.kt:695-726).
    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (isAutoScrollEnabled) {
            if ((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
                shouldScrollToFirstLine = false
                performSmoothPageScroll(currentLineIndex.coerceAtLeast(0), 800)
                initialScrollDone = true
            } else if (currentLineIndex != -1) {
                if (isSeeking) {
                    performSmoothPageScroll(currentLineIndex.coerceAtLeast(0), 500)
                } else if (lastPreviewTime == 0L && currentLineIndex != previousLineIndex) {
                    performSmoothPageScroll(currentLineIndex, 1500)
                }
            }
        }
        if (currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
    ) {
        val listHeight = maxHeight
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = listHeight / 3, bottom = listHeight / 2),
            modifier = Modifier
                .fadingEdge(vertical = 64.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                isAutoScrollEnabled = false
                                lastPreviewTime = System.currentTimeMillis()
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            isAutoScrollEnabled = false
                            lastPreviewTime = System.currentTimeMillis()
                            return super.onPostFling(consumed, available)
                        }
                    }
                }),
        ) {
            itemsIndexed(
                items = entries,
                key = { index, item -> "$index-${item.time}" },
            ) { index, item ->
                val currentLineTime = entries.getOrNull(currentLineIndex)?.time ?: -1L
                val isActiveByIndex = index == currentLineIndex
                val isActiveByTime = item.time == currentLineTime && currentLineIndex >= 0

                AppleLyricsLine(
                    entry = item,
                    nextEntryTime = entries.getOrNull(index + 1)?.time,
                    effectivePlaybackPosition = currentPlaybackPosition,
                    isActive = isActiveByIndex || isActiveByTime,
                    distanceFromCurrent = abs(index - currentLineIndex),
                    textColor = accent,
                    isAutoScrollActive = isAutoScrollEnabled,
                    onClick = {
                        onSeek(item.time)
                        scope.launch {
                            lazyListState.scrollToItem(index = index)
                            val itemInfo =
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                            if (itemInfo != null) {
                                val viewportHeight =
                                    lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                val itemCenter = itemInfo.offset + itemInfo.size / 2
                                val offset = itemCenter - center
                                if (abs(offset) > 10) {
                                    lazyListState.animateScrollBy(
                                        value = offset.toFloat(),
                                        animationSpec = tween(durationMillis = 1500),
                                    )
                                }
                            }
                        }
                        lastPreviewTime = 0L
                    },
                )
            }
        }

        // Resume auto-scroll pill (Echo Lyrics.kt:1877-1899).
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
            AnimatedVisibility(
                visible = !isAutoScrollEnabled,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                FilledTonalButton(onClick = {
                    scope.launch { performSmoothPageScroll(currentLineIndex, 1500) }
                    isAutoScrollEnabled = true
                }) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Auto scroll")
                }
            }
        }
    }
}

/**
 * A single karaoke lyric line: each word is its own Text whose fill sweeps a
 * horizontal gradient driven by a 150ms-eased progress, with a growing glow
 * shadow (Echo EchoMusicLyrics.kt echomusicLyricsLine, verbatim minus
 * romanization/translation/selection).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AppleLyricsLine(
    entry: LyricsEntry,
    nextEntryTime: Long?,
    effectivePlaybackPosition: Long,
    isActive: Boolean,
    distanceFromCurrent: Int,
    textColor: Color,
    isAutoScrollActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textSize = LYRICS_TEXT_SIZE
    val lineSpacing = LYRICS_LINE_SPACING

    val targetBlur = if (!isAutoScrollActive || isActive) {
        0f
    } else {
        when (distanceFromCurrent) {
            1 -> 0f
            2 -> 0f
            3 -> 2f
            4 -> 4f
            else -> 6f
        }
    }
    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 1000),
        label = "blur",
    )

    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }
    val activeDuration = remember(duration) {
        (duration * 0.95).toLong().coerceAtLeast(300L)
    }

    // Word windows relative to line start: real timings when the provider gave
    // them, synthesized proportionally to character counts otherwise.
    val wordData = remember(entry.text, entry.words, activeDuration) {
        val words = entry.words
        if (words != null && words.isNotEmpty()) {
            words.map { word ->
                val wordStart = ((word.startTime * 1000).toLong() - entry.time).coerceAtLeast(0L)
                val wordEnd = ((word.endTime * 1000).toLong() - entry.time).coerceAtLeast(wordStart + 50L)
                Triple(word.text, wordStart, wordEnd)
            }
        } else {
            val split = entry.text.split(" ").filter { it.isNotEmpty() }
            if (split.isEmpty()) {
                listOf(Triple(entry.text, 0L, activeDuration))
            } else {
                val totalChars = entry.text.length
                var accumulatedTime = 0L
                split.mapIndexed { index, word ->
                    val includeSpace = index < split.size - 1
                    val charCount = if (includeSpace) word.length + 1 else word.length
                    val wordStart = accumulatedTime
                    val wordDur = if (totalChars > 0) {
                        (activeDuration * charCount.toFloat() / totalChars).toLong()
                    } else activeDuration
                    accumulatedTime += wordDur
                    Triple(word, wordStart, wordStart + wordDur)
                }
            }
        }
    }

    val targetAlpha = when {
        isActive -> 1f
        distanceFromCurrent == 1 -> 0.65f
        distanceFromCurrent == 2 -> 0.45f
        else -> 0.35f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "lineScale",
    )

    val bgScale = if (entry.isBackground) 0.85f else 1f
    val itemModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            this.alpha = if (entry.isBackground) animatedAlpha * 0.8f else animatedAlpha
            this.scaleX = scale * bgScale
            this.scaleY = scale * bgScale
        }
        .clip(RoundedCornerShape(16.dp))
        .combinedClickable(enabled = true, onClick = onClick)
        .padding(horizontal = 24.dp, vertical = (8 * lineSpacing).dp)
        .blur(animatedBlur.dp)

    val agentAlignment = when {
        entry.isBackground -> Alignment.CenterHorizontally
        entry.agent == "v2" -> Alignment.End
        else -> Alignment.Start
    }
    val agentTextAlign = when {
        entry.isBackground -> TextAlign.Center
        entry.agent == "v2" -> TextAlign.Right
        else -> TextAlign.Left
    }

    Column(modifier = itemModifier, horizontalAlignment = agentAlignment) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (agentTextAlign) {
                TextAlign.Center -> Arrangement.Center
                TextAlign.Right -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalArrangement = Arrangement.spacedBy(
                with(LocalDensity.current) { (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp() }
            ),
        ) {
            wordData.forEachIndexed { index, (wordText, startRelative, endRelative) ->
                val lineRelTime = (effectivePlaybackPosition - entry.time).coerceAtLeast(0L)
                val progress = syncedWordProgress(
                    isActiveLine = isActive,
                    lineRelativeTimeMs = lineRelTime,
                    wordStartMs = startRelative,
                    wordEndMs = endRelative,
                )

                val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                val wordBrush = when {
                    progress <= 0f -> Brush.linearGradient(
                        listOf(
                            textColor.copy(alpha = INACTIVE_WORD_ALPHA),
                            textColor.copy(alpha = INACTIVE_WORD_ALPHA),
                        ),
                    )
                    progress >= 1f -> Brush.linearGradient(listOf(textColor, textColor))
                    else -> Brush.horizontalGradient(
                        0f to textColor,
                        progress to textColor,
                        progress to textColor.copy(alpha = INACTIVE_WORD_ALPHA),
                        1f to textColor.copy(alpha = INACTIVE_WORD_ALPHA),
                    )
                }

                Text(
                    text = wordText,
                    fontSize = textSize.sp,
                    style = TextStyle(
                        brush = wordBrush,
                        fontWeight = finalFontWeight,
                        lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        textAlign = agentTextAlign,
                        shadow = Shadow(
                            color = textColor.copy(alpha = 0.6f * progress),
                            offset = Offset.Zero,
                            blurRadius = (12f * progress).coerceAtLeast(0.1f),
                        ),
                    ),
                )
                if (index != wordData.lastIndex) {
                    val isSpaceHighlighted = isActive && lineRelTime >= endRelative
                    Text(
                        text = " ",
                        fontSize = textSize.sp,
                        color = textColor.copy(alpha = if (isSpaceHighlighted) 1f else INACTIVE_WORD_ALPHA),
                        lineHeight = (textSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        style = TextStyle(
                            shadow = if (isSpaceHighlighted) {
                                Shadow(
                                    color = textColor.copy(alpha = 0.3f),
                                    offset = Offset.Zero,
                                    blurRadius = 6f,
                                )
                            } else null,
                        ),
                    )
                }
            }
        }
    }
}

/** Plain (unsynced) lyrics fallback. */
@Composable
fun ApplePlainLyrics(text: String, modifier: Modifier = Modifier, accent: Color = Color.White) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 72.dp),
    ) {
        Text(text, color = accent, fontSize = 24.sp, lineHeight = 31.2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(160.dp))
    }
}
