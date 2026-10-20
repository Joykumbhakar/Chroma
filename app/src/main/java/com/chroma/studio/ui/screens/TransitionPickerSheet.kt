package com.chroma.studio.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlin.math.sin

@Composable
fun TransitionPickerSheet(
    currentTransition: TransitionType,
    onTransitionSelected: (TransitionType) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var previewType by remember { mutableStateOf(currentTransition) }

    val infiniteTransition = rememberInfiniteTransition(label = "chromaTransitionPreview")
    val rawProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chromaTransitionProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Pill Handle
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFFD1D5DB))
        )

        Spacer(Modifier.height(12.dp))

        // Header: Title & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Clip Transition",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "Tap to preview transition effect",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Close",
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Live Animated Transition Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Outgoing Clip Mock (Clip A - Teal Gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F766E), Color(0xFF06B6D4))
                        )
                    )
            ) {
                Text(
                    text = "CLIP A",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
                )
            }

            // Animated Transition Overlay Effect
            when (previewType) {
                TransitionType.NONE -> {
                    // Split boundary line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
                TransitionType.CROSSFADE, TransitionType.DISSOLVE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                )
                            )
                            .alpha(rawProgress)
                    )
                }
                TransitionType.SLIDE_LEFT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterEnd)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                )
                            )
                    )
                }
                TransitionType.SLIDE_RIGHT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                )
                            )
                    )
                }
                TransitionType.ZOOM_IN -> {
                    val zoomAlpha = sin(rawProgress * Math.PI.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFEC4899).copy(alpha = zoomAlpha * 0.9f), Color(0xFF7C3AED).copy(alpha = zoomAlpha * 0.9f))
                                )
                            )
                    )
                }
                TransitionType.WIPE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterStart)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4338CA), Color(0xFF8B5CF6))
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Preview HUD Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "PREVIEW: ${previewType.label.uppercase()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Transition Selector Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
        ) {
            items(TransitionType.values()) { type ->
                val isSelected = type == previewType
                val icon = when (type) {
                    TransitionType.NONE -> Lucide.Minus
                    TransitionType.CROSSFADE -> Lucide.Shuffle
                    TransitionType.DISSOLVE -> Lucide.Sparkles
                    TransitionType.SLIDE_LEFT -> Lucide.ArrowLeft
                    TransitionType.SLIDE_RIGHT -> Lucide.ArrowRight
                    TransitionType.ZOOM_IN -> Lucide.Maximize2
                    TransitionType.WIPE -> Lucide.Layers
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { previewType = type }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.label,
                            tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = type.label,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1F2937),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action Button: Apply Transition
        Button(
            onClick = {
                onTransitionSelected(previewType)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text(
                text = "Apply Transition",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
