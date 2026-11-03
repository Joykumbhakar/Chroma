package com.chroma.studio.chat.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Organic Disney-style "Squash & Stretch" Squeeze In / Squeeze Out Container
 * for the Voice Recording Bubble Card above the Chat Input Bar.
 *
 * Squeeze In:
 * - Shoots upward from the input bar (scaleX: 0.35, scaleY: 1.45, offsetY: 90f)
 * - Impacts at target height with a horizontal squash (scaleX: 1.18, scaleY: 0.86)
 * - Spring rebounds fluidly to rest (scaleX: 1.0, scaleY: 1.0)
 *
 * Squeeze Out:
 * - Anticipation pre-squeeze squash (scaleX: 1.15, scaleY: 0.82)
 * - Squeezes thin & tall as it gets sucked back into the input bar (scaleX: 0.20, scaleY: 1.45, offsetY: 90f)
 * - Cleanly dissolves into the mic button origin
 */
@Composable
fun SqueezeVoiceBubbleWrapper(
    visible: Boolean,
    modifier: Modifier = Modifier,
    transformOrigin: TransformOrigin = TransformOrigin(0.5f, 1f),
    content: @Composable () -> Unit
) {
    val scaleX = remember { Animatable(if (visible) 1f else 0.35f) }
    val scaleY = remember { Animatable(if (visible) 1f else 1.45f) }
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    val offsetY = remember { Animatable(if (visible) 0f else 90f) }

    var isContentActive by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            isContentActive = true
            val startX = if (alpha.value > 0.15f) scaleX.value else 0.35f
            val startY = if (alpha.value > 0.15f) scaleY.value else 1.45f
            val startOff = if (alpha.value > 0.15f) offsetY.value else 90f

            scaleX.snapTo(startX)
            scaleY.snapTo(startY)
            offsetY.snapTo(startOff)

            // Squeeze In Animation: shoots up, squashes on landing, then springs into place
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                // Initial horizontal squash expansion on landing
                scaleX.animateTo(
                    targetValue = 1.18f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                // Spring rebound to resting shape
                scaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                // Initial vertical compression on landing
                scaleY.animateTo(
                    targetValue = 0.86f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                // Spring rebound to resting shape
                scaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else {
            // Squeeze Out Animation: anticipation squash, then stretch and suck down into input bar
            launch {
                // 1. Anticipation squash
                scaleX.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                )
                // 2. Squeeze thin
                scaleX.animateTo(
                    targetValue = 0.20f,
                    animationSpec = tween(durationMillis = 190, easing = FastOutLinearInEasing)
                )
            }
            launch {
                // 1. Anticipation compress
                scaleY.animateTo(
                    targetValue = 0.82f,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                )
                // 2. Squeeze stretch tall
                scaleY.animateTo(
                    targetValue = 1.45f,
                    animationSpec = tween(durationMillis = 190, easing = FastOutLinearInEasing)
                )
            }
            launch {
                delay(80)
                offsetY.animateTo(
                    targetValue = 90f,
                    animationSpec = tween(durationMillis = 190, easing = FastOutLinearInEasing)
                )
            }
            launch {
                delay(90)
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 170, easing = LinearEasing)
                )
                isContentActive = false
            }
        }
    }

    if (isContentActive || alpha.value > 0.001f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.transformOrigin = transformOrigin
                    this.scaleX = scaleX.value
                    this.scaleY = scaleY.value
                    this.translationY = offsetY.value
                    this.alpha = alpha.value
                }
        ) {
            content()
        }
    }
}
