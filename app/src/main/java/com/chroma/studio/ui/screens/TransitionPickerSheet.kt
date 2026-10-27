package com.chroma.studio.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlin.math.sin

@Deprecated("Use AppleVideoTransitionStudio from FinalCutProVideoEditor instead. This sheet is locked to 7 legacy transitions and will be removed.")
@Composable
fun TransitionPickerSheet(
    currentTransition: TransitionType,
    onTransitionSelected: (TransitionType) -> Unit,
    onDismiss: () -> Unit,
    outgoingBitmap: Bitmap? = null,
    incomingBitmap: Bitmap? = null
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
        // Header: Title & Close (Capsule/Pill free)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
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

        // Live Animated Transition Preview Card (Real Video Clip Frames / Cinematic HUD)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Outgoing Video Clip Scene (Real Frame or Cinematic Scene 1)
            CinematicClipScene(
                bitmap = outgoingBitmap,
                isIncoming = false,
                modifier = Modifier.fillMaxSize()
            )

            // Animated Transition Overlay Effect with Real Incoming Video Clip Scene
            when (previewType) {
                TransitionType.NONE -> {
                    // Split boundary line at current progress
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }
                TransitionType.CROSSFADE, TransitionType.DISSOLVE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(rawProgress)
                    ) {
                        CinematicClipScene(
                            bitmap = incomingBitmap,
                            isIncoming = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                TransitionType.SLIDE_LEFT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterEnd)
                    ) {
                        CinematicClipScene(
                            bitmap = incomingBitmap,
                            isIncoming = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                TransitionType.SLIDE_RIGHT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterStart)
                    ) {
                        CinematicClipScene(
                            bitmap = incomingBitmap,
                            isIncoming = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                TransitionType.ZOOM_IN -> {
                    val zoomAlpha = sin(rawProgress * Math.PI.toFloat()).coerceIn(0f, 1f)
                    val zoomScale = 0.75f + 0.25f * zoomAlpha
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(zoomAlpha)
                            .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
                    ) {
                        CinematicClipScene(
                            bitmap = incomingBitmap,
                            isIncoming = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                TransitionType.WIPE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = rawProgress)
                            .align(Alignment.CenterStart)
                    ) {
                        CinematicClipScene(
                            bitmap = incomingBitmap,
                            isIncoming = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.5.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Preview HUD Tag (Capsule/Pill free)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = previewType.label.uppercase(),
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

@Composable
private fun CinematicClipScene(
    bitmap: Bitmap?,
    isIncoming: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = if (!isIncoming) {
                        listOf(Color(0xFF1B1B2F), Color(0xFF3B1D54), Color(0xFF8C3A5C), Color(0xFFE07A5F))
                    } else {
                        listOf(Color(0xFF0A192F), Color(0xFF112240), Color(0xFF1F4068), Color(0xFF00B4D8))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = if (isIncoming) "Incoming Scene" else "Outgoing Scene",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val thirdsColor = Color.White.copy(alpha = 0.12f)
                drawLine(thirdsColor, Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
                drawLine(thirdsColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), 1f)
                drawLine(thirdsColor, Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
                drawLine(thirdsColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), 1f)
                val reticleColor = if (!isIncoming) Color.White.copy(alpha = 0.28f) else Color(0xFF38BDF8).copy(alpha = 0.35f)
                val cx = w / 2f
                val cy = h / 2f
                drawLine(reticleColor, Offset(cx - 10f, cy), Offset(cx + 10f, cy), 1.5f)
                drawLine(reticleColor, Offset(cx, cy - 10f), Offset(cx, cy + 10f), 1.5f)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (!isIncoming) Color(0xFFEF4444) else Color(0xFF10B981))
                )
                Text(
                    text = if (!isIncoming) "REC 4K" else "PLAY 4K",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (!isIncoming) "SCENE 01" else "SCENE 02",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (!isIncoming) "01:23:45:12" else "01:23:47:08",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
