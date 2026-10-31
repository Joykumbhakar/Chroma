package com.chroma.studio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chroma.studio.ui.components.GhostFibers
import com.chroma.studio.ui.components.GhostFibersConfig
import com.chroma.studio.ui.components.OscellionSplashBrandView
import kotlinx.coroutines.delay

/**
 * Animated Splash Screen powered by the GhostFibers React Bits port.
 * Features 3D flowing cyber ribbons, chromatic depth-of-field particles,
 * and the cold-boot milled metallic Oscellion emblem.
 */
@Composable
fun OscellionSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2200L
) {
    LaunchedEffect(Unit) {
        delay(durationMillis)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF010414)),
        contentAlignment = Alignment.Center
    ) {
        // GhostFibers animated 3D ribbon background
        GhostFibers(
            modifier = Modifier.fillMaxSize(),
            config = GhostFibersConfig(
                glowColor = Color(0xFF001FFF),
                lineColor = Color(0xFF0070FF),
                secondaryColor = Color(0xFF0038FF),
                layers = 3,
                twist = 1.2f,
                threads = 72,
                depthOfField = 0.85f,
                particles = 0.5f,
                sparkle = 0.5f,
                glow = 0.8f,
                brightness = 1f,
                intro = true
            )
        )

        // Subtle radial vignette to ensure logo pops crisply
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xCC010414),
                            Color.Transparent
                        )
                    )
                )
        )

        // Pulsing metallic emblem and title
        OscellionSplashBrandView(size = 100.dp)
    }
}
