package com.gorilla.music.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color

/**
 * Shared animation specs. Rewired from springs to well-optimized simple curved tween animations
 * to ensure 60fps smoothness and eliminate overshoot jank on lower-end devices.
 */
object SpringSpecs {

    /** Snappy press feedback for buttons / scale (small element: 150ms ease-out). */
    val Press: AnimationSpec<Float> = tween(
        durationMillis = 150,
        easing = LinearOutSlowInEasing
    )

    /** Standard UI element settle (alpha, small offsets: 200ms ease-out). */
    val Standard: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )

    /** Curved entrance for nav icons / selection (200ms ease-out). */
    val Bouncy: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )

    /** Sheet / large surface motion (200ms ease-out). */
    val Sheet: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )

    /** Gentle continuous values like blur radius (200ms ease-in-out). */
    val Smooth: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val OffsetSpring: AnimationSpec<IntOffset> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val DpSpring: AnimationSpec<Dp> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val ColorSpring: AnimationSpec<Color> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
}
