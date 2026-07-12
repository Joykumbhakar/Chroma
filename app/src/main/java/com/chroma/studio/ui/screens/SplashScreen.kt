package com.chroma.studio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import com.chroma.studio.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(4000)
        isReady = true
        onAnimationFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val driftX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
            translate(left = size.width * driftX, top = 0f) {
                drawMix1(this)
                drawLayer2(this)
            }
        }
        val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_logo)
        var atEnd by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            // Trigger the animation start
            atEnd = true
        }

        Image(
            painter = rememberAnimatedVectorPainter(image, atEnd),
            contentDescription = "JD Logo",
            modifier = Modifier.size(160.dp)
        )
    }
}

private fun drawMix1(scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    // 50% radius
    val r = maxOf(w, h) * 0.7f

    val stops = listOf(
        Pair(Offset(0f, 0f), Color(255, 173, 40)),
        Pair(Offset(w * 0.5f, 0f), Color(255, 245, 236)),
        Pair(Offset(w, 0f), Color(178, 201, 220)),
        
        Pair(Offset(0f, h * 0.5f), Color(103, 59, 215)),
        Pair(Offset(w * 0.5f, h * 0.5f), Color(78, 224, 9)),
        Pair(Offset(w, h * 0.5f), Color(112, 117, 198)),
        
        Pair(Offset(0f, h), Color(114, 202, 51)),
        Pair(Offset(w * 0.5f, h), Color(252, 9, 45)),
        Pair(Offset(w, h), Color(36, 60, 250))
    )

    for ((center, color) in stops) {
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, color.copy(alpha = 0f)),
                center = center,
                radius = r
            ),
            center = center,
            radius = r
        )
    }
}

private fun drawLayer2(scope: DrawScope) {
    val w = scope.size.width
    val h = scope.size.height

    // ellipse 30% 26% at 99% 98%, rgba(79,70,229,1)
    val cx1 = w * 0.99f
    val cy1 = h * 0.98f
    val rx1 = w * 0.3f
    val ry1 = h * 0.26f
    
    // Fake an ellipse by scaling the canvas slightly during draw
    scope.scale(scaleX = 1f, scaleY = ry1 / rx1, pivot = Offset(cx1, cy1)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(79, 70, 229), Color(79, 70, 229, 0)),
                center = Offset(cx1, cy1),
                radius = rx1
            ),
            center = Offset(cx1, cy1),
            radius = rx1
        )
    }

    // ellipse 50% 50% at 93% 18%, rgba(0,200,232,1)
    val cx2 = w * 0.93f
    val cy2 = h * 0.18f
    val r2 = maxOf(w, h) * 0.5f
    
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0, 200, 232), Color(0, 200, 232, 0)),
            center = Offset(cx2, cy2),
            radius = r2
        ),
        center = Offset(cx2, cy2),
        radius = r2
    )
}
