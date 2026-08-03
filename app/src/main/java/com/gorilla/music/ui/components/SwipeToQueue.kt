package com.gorilla.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import com.gorilla.music.ui.theme.LocalAppColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.SpringSpecs
import kotlinx.coroutines.launch

/**
 * Wraps a row with a swipe-right-to-queue gesture: dragging past a threshold reveals
 * a queue icon, fires [onQueue] with a haptic, then springs back. Spring-driven, no
 * linear easing.
 */
@Composable
fun SwipeToQueue(
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val textPrimary = LocalAppColors.current.textPrimary
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 96.dp.toPx() }

    Box(modifier) {
        // Revealed action behind the row.
        Box(
            Modifier
                .matchParentSize()
                .padding(start = 28.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                Icons.Rounded.PlaylistPlay,
                contentDescription = "Add to queue",
                tint = textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        Box(
            Modifier
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > threshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onQueue()
                                }
                                offsetX.animateTo(0f, SpringSpecs.Standard)
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, SpringSpecs.Standard)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, threshold * 1.4f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
