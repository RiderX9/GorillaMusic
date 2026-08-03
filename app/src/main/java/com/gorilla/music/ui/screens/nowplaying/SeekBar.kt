package com.gorilla.music.ui.screens.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.music.ui.components.formatDuration

@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
    enabled: Boolean = true
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(0L) }
    
    val displayPositionMs = if (isScrubbing) scrubPositionMs else positionMs
    val dur = durationMs.coerceAtLeast(1L)
    val fraction = (displayPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .let { base ->
                    if (enabled) {
                        base.pointerInput(durationMs) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val currentPos = (offset.x / size.width).coerceIn(0f, 1f)
                                    onSeek((currentPos * dur).toLong())
                                }
                            )
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true
                                    val currentPos = (offset.x / size.width).coerceIn(0f, 1f)
                                    scrubPositionMs = (currentPos * dur).toLong()
                                },
                                onDragEnd = {
                                    isScrubbing = false
                                    onSeek(scrubPositionMs)
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentPos = (change.position.x / size.width).coerceIn(0f, 1f)
                                    scrubPositionMs = (currentPos * dur).toLong()
                                }
                            )
                        }
                    } else base
                },
        ) {
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val trackH = 4.dp.toPx()
                val cy = size.height / 2
                val radius = CornerRadius(trackH / 2, trackH / 2)
                
                // Background track
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(0f, cy - trackH / 2),
                    size = Size(size.width, trackH),
                    cornerRadius = radius,
                )
                
                val fillW = size.width * fraction
                
                // Soft accent glow under the filled portion.
                drawRoundRect(
                    color = accent.copy(alpha = 0.35f),
                    topLeft = Offset(0f, cy - trackH / 2 - 2.dp.toPx()),
                    size = Size(fillW, trackH + 4.dp.toPx()),
                    cornerRadius = CornerRadius(trackH, trackH),
                )
                
                // Filled portion
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, cy - trackH / 2),
                    size = Size(fillW, trackH),
                    cornerRadius = radius,
                )
                
                // Thumb — a filled accent circle over a soft accent halo.
                val thumbX = fillW.coerceIn(0f, size.width)
                drawCircle(
                    color = accent.copy(alpha = 0.30f),
                    radius = if (isScrubbing) 12.dp.toPx() else 10.dp.toPx(),
                    center = Offset(thumbX, cy),
                )
                drawCircle(
                    color = accent,
                    radius = if (isScrubbing) 8.dp.toPx() else 7.dp.toPx(),
                    center = Offset(thumbX, cy),
                )
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                text = formatDuration(displayPositionMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDuration(durationMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
