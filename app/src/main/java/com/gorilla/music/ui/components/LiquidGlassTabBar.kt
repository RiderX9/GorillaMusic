package com.gorilla.music.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalLiquidGlassBackdrop
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic

@Composable
fun LiquidGlassTabBar(
    labels: List<String>,
    selectedIndex: Int,
    selectionPosition: Float = selectedIndex.toFloat(),
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (labels.isEmpty()) return

    val backgroundBackdrop = LocalLiquidGlassBackdrop.current
    val safeSelectedIndex = selectedIndex.coerceIn(labels.indices)
    val safeSelectionPosition = selectionPosition.coerceIn(0f, labels.lastIndex.toFloat())
    val selectedIndexState by rememberUpdatedState(safeSelectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val haptic = rememberHaptic()
    val density = LocalDensity.current
    var dragIndex by remember { mutableIntStateOf(safeSelectedIndex) }
    var dragOffsetPx by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(safeSelectedIndex) {
        dragIndex = safeSelectedIndex
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (compact) {
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                },
            ),
    ) {
        val horizontalPadding = if (compact) 5.dp else 4.dp
        val verticalPadding = if (compact) 5.dp else 4.dp
        val tabHeight = if (compact) 36.dp else 44.dp
        val barHeight = tabHeight + verticalPadding * 2
        val contentWidth = maxWidth - horizontalPadding * 2
        val tabWidth = contentWidth / labels.size
        val indicatorWidth = tabWidth
        val indicatorHeight = tabHeight
        val selectedIndicatorOffset = horizontalPadding + tabWidth * safeSelectionPosition
        val indicatorOffset = if (dragOffsetPx.isNaN()) {
            selectedIndicatorOffset
        } else {
            with(density) { dragOffsetPx.toDp() }
        }

        fun selectAt(xPx: Float, commit: Boolean) {
            val leftPx = with(density) { horizontalPadding.toPx() }
            val tabWidthPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
            val maxIndicatorPx = leftPx + tabWidthPx * labels.lastIndex
            dragOffsetPx = (xPx - tabWidthPx / 2f).coerceIn(leftPx, maxIndicatorPx)
            val index = ((xPx - leftPx) / tabWidthPx)
                .toInt()
                .coerceIn(0, labels.lastIndex)
            if (index != dragIndex) {
                dragIndex = index
                haptic()
                onSelectState(index)
            }
            if (commit) onSelectState(index)
        }

        LiquidGlassSurface(
            depth = GlassDepth.LOW,
            shape = CapsuleShape,
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.026f,
            backdrop = backgroundBackdrop,
            shadow = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(tabWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragIndex = selectedIndexState
                            selectAt(offset.x, commit = false)
                        },
                        onHorizontalDrag = { change, _ ->
                            selectAt(change.position.x, commit = false)
                            change.consume()
                        },
                        onDragEnd = {
                            onSelectState(dragIndex)
                            dragOffsetPx = Float.NaN
                        },
                        onDragCancel = { dragOffsetPx = Float.NaN },
                    )
                }
        ) {
            LiquidGlassSurface(
                depth = GlassDepth.LOW,
                shape = RoundedCornerShape(percent = 50),
                tint = LocalDynamicColors.current.accent,
                surfaceColor = LocalDynamicColors.current.accent.copy(alpha = 0.18f),
                tintAlphaOverride = 0.14f,
                backdrop = backgroundBackdrop,
                shadow = false,
                modifier = Modifier
                    .offset(x = indicatorOffset, y = verticalPadding)
                    .width(indicatorWidth)
                    .height(indicatorHeight)
                    .background(
                        color = LocalDynamicColors.current.accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(percent = 50),
                    )
            ) {}

            Row(
                Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.forEachIndexed { index, label ->
                    TopTabLabel(
                        label = label,
                        selected = index == safeSelectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.width(tabWidth),
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollableLiquidGlassTabBar(
    labels: List<String>,
    selectedIndex: Int,
    selectionPosition: Float = selectedIndex.toFloat(),
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    compactVisibleTabCount: Int? = null,
    compactBottomPadding: Dp = 18.dp,
    compactFontSize: TextUnit = 12.5.sp,
    compactTabSpacing: Dp = 4.dp,
    compactHorizontalPadding: Dp = 5.dp,
    compactLabelHorizontalPadding: Dp = 4.dp,
    compactFitTabsToWidth: Boolean = false,
) {
    if (labels.isEmpty()) return

    val backgroundBackdrop = LocalLiquidGlassBackdrop.current
    val safeSelectedIndex = selectedIndex.coerceIn(labels.indices)
    val safeSelectionPosition = selectionPosition.coerceIn(0f, labels.lastIndex.toFloat())
    val selectedIndexState by rememberUpdatedState(safeSelectedIndex)
    val onSelectState by rememberUpdatedState(onSelect)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val compactTextStyle = LocalTextStyle.current.copy(fontSize = compactFontSize)
    val haptic = rememberHaptic()
    var dragIndex by remember { mutableIntStateOf(safeSelectedIndex) }
    var dragOffsetPx by remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(safeSelectedIndex) {
        dragIndex = safeSelectedIndex
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (compact) {
                    Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = compactBottomPadding,
                    )
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                },
            ),
    ) {
        val maxLabelLength = labels.maxOfOrNull { it.length } ?: 10
        val defaultTabWidth = maxOf(92.dp, (maxLabelLength * 8.5).dp)
        val tabSpacing = if (compact) compactTabSpacing else 0.dp
        val horizontalPadding = if (compact) compactHorizontalPadding else 4.dp
        val fixedCompactTabWidth = compactVisibleTabCount
            ?.takeIf { compact && it > 0 }
            ?.let { visibleCount ->
                (
                    maxWidth -
                        horizontalPadding * 2 -
                        tabSpacing * (visibleCount - 1)
                    ) / visibleCount
            }
        val measuredCompactWidths = if (compact) {
            labels.map { label ->
                val measuredWidth = textMeasurer.measure(
                    text = AnnotatedString(label),
                    style = compactTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                ).size.width
                with(density) { measuredWidth.toDp() }
            }
        } else {
            emptyList()
        }
        val fittedCompactWidths = if (compact && compactFitTabsToWidth) {
            val availableWidth =
                maxWidth -
                    horizontalPadding * 2 -
                    tabSpacing * (labels.size - 1).coerceAtLeast(0)
            val measuredTotal = measuredCompactWidths.fold(0.dp) { total, width -> total + width }
            val extraPerTab = if (measuredTotal < availableWidth) {
                (availableWidth - measuredTotal) / labels.size
            } else {
                0.dp
            }
            measuredCompactWidths.map { it + extraPerTab }
        } else {
            emptyList()
        }
        val tabWidths = labels.mapIndexed { index, _ ->
            when {
                fixedCompactTabWidth != null -> fixedCompactTabWidth
                fittedCompactWidths.isNotEmpty() -> fittedCompactWidths[index]
                compact -> measuredCompactWidths[index] + 32.dp
                else -> defaultTabWidth
            }
        }
        val verticalPadding = if (compact) 5.dp else 4.dp
        val tabHeight = if (compact) 36.dp else 44.dp
        val barHeight = tabHeight + verticalPadding * 2
        val viewportWidth = maxWidth
        val tabStarts = buildList {
            var nextStart = horizontalPadding
            tabWidths.forEach { width ->
                add(nextStart)
                nextStart += width + tabSpacing
            }
        }
        val tabsWidth = tabWidths.fold(0.dp) { total, width -> total + width } +
            tabSpacing * (labels.size - 1).coerceAtLeast(0)
        val contentWidth = maxOf(
            viewportWidth,
            tabsWidth + horizontalPadding * 2,
        )

        LaunchedEffect(safeSelectedIndex, scrollState.maxValue, viewportWidth) {
            val viewportWidthPx = with(density) { viewportWidth.toPx() }
            val selectedStartPx = with(density) { tabStarts[safeSelectedIndex].toPx() }
            val selectedWidthPx = with(density) { tabWidths[safeSelectedIndex].toPx() }
            val target = (
                selectedStartPx +
                    selectedWidthPx / 2f -
                    viewportWidthPx / 2f
                )
                .toInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
        }

        val lowerIndex = safeSelectionPosition.toInt().coerceIn(labels.indices)
        val upperIndex = (lowerIndex + 1).coerceAtMost(labels.lastIndex)
        val positionFraction = safeSelectionPosition - lowerIndex
        val selectedIndicatorOffset =
            tabStarts[lowerIndex] +
                (tabStarts[upperIndex] - tabStarts[lowerIndex]) * positionFraction
        val selectedIndicatorWidth =
            tabWidths[lowerIndex] +
                (tabWidths[upperIndex] - tabWidths[lowerIndex]) * positionFraction
        val indicatorOffset = if (dragOffsetPx.isNaN()) {
            selectedIndicatorOffset
        } else {
            with(density) { dragOffsetPx.toDp() }
        }
        val indicatorWidth = if (dragOffsetPx.isNaN()) {
            selectedIndicatorWidth
        } else {
            tabWidths[dragIndex]
        }

        fun selectAt(xPx: Float, commit: Boolean) {
            val centersPx = labels.indices.map { index ->
                with(density) {
                    tabStarts[index].toPx() + tabWidths[index].toPx() / 2f
                }
            }
            val index = centersPx.indices.minBy { index ->
                kotlin.math.abs(xPx - centersPx[index])
            }
            val indicatorWidthPx = with(density) { tabWidths[index].toPx() }
            val minOffsetPx = with(density) { tabStarts.first().toPx() }
            val maxOffsetPx = with(density) { tabStarts.last().toPx() }
            dragOffsetPx = (xPx - indicatorWidthPx / 2f).coerceIn(minOffsetPx, maxOffsetPx)
            if (index != dragIndex) {
                dragIndex = index
                haptic()
                onSelectState(index)
            }
            if (commit) onSelectState(index)
        }

        LiquidGlassSurface(
            depth = GlassDepth.LOW,
            shape = CapsuleShape,
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.026f,
            backdrop = backgroundBackdrop,
            shadow = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(tabWidths) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragIndex = selectedIndexState
                            selectAt(offset.x + scrollState.value, commit = false)
                        },
                        onHorizontalDrag = { change, _ ->
                            selectAt(change.position.x + scrollState.value, commit = false)
                            change.consume()
                        },
                        onDragEnd = {
                            onSelectState(dragIndex)
                            dragOffsetPx = Float.NaN
                        },
                        onDragCancel = { dragOffsetPx = Float.NaN },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(contentWidth)
                    .height(barHeight),
            ) {
                LiquidGlassSurface(
                    depth = GlassDepth.LOW,
                    shape = RoundedCornerShape(percent = 50),
                    tint = LocalDynamicColors.current.accent,
                    surfaceColor = LocalDynamicColors.current.accent.copy(alpha = 0.18f),
                    tintAlphaOverride = 0.14f,
                    backdrop = backgroundBackdrop,
                    shadow = false,
                    modifier = Modifier
                        .offset(
                            x = indicatorOffset,
                            y = verticalPadding,
                        )
                        .width(indicatorWidth)
                        .height(tabHeight)
                        .background(
                            color = LocalDynamicColors.current.accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(percent = 50),
                        ),
                ) {}

                Row(
                    modifier = Modifier
                        .width(contentWidth)
                        .zIndex(1f)
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tabSpacing),
                ) {
                    labels.forEachIndexed { index, label ->
                        TopTabLabel(
                            label = label,
                            selected = index == safeSelectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.width(tabWidths[index]),
                            compact = compact,
                            compactFontSize = compactFontSize,
                            horizontalTextPadding = compactLabelHorizontalPadding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTabLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    compactFontSize: TextUnit = 12.5.sp,
    horizontalTextPadding: Dp = 4.dp,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()

    Box(
        modifier = modifier
            .instantClickable(pressedScale = 0.94f) {
                if (!selected) haptic()
                onClick()
            }
            .height(if (compact) 36.dp else 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (compact) {
                LocalTextStyle.current.copy(
                    fontSize = compactFontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                )
            } else {
                MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                )
            },
            fontSize = if (compact) compactFontSize else 13.sp,
            color = if (selected) accent else appColors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalTextPadding),
        )
    }
}
