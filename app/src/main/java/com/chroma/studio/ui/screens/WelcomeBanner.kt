package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.ChromaPillVariant
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.OscellionAppIcon

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide

@Composable
fun WelcomeBanner(onDismiss: () -> Unit) {
    val colors = LocalOscellionColors.current

    // Micro-animations for the banner
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.2f),
                                Color(0xFF818CF8).copy(alpha = 0.05f)
                            )
                        )
                    )
                    .background(colors.glassBg)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(scale),
                        contentAlignment = Alignment.Center
                    ) {
                        OscellionAppIcon(size = 76.dp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Welcome to Oscellion",
                        color = colors.textMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Design stunning wallpapers and animations. Share your creations with the world and discover amazing artwork from our community.",
                        color = colors.textMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    ChromaPillButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        variant = com.chroma.studio.ui.components.ChromaPillVariant.Blue,
                        height = 50.dp
                    ) {
                        Text(
                            text = "Get Started",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
