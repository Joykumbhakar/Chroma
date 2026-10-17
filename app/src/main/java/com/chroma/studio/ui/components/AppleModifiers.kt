package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Users
import kotlin.math.abs

fun Modifier.spatialCard(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 8.dp,
    borderAlpha: Float = 0.6f
) = composed {
    this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.06f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xF2FFFFFF), // rgba(255,255,255,0.95) for crisp contrast
                    Color(0xD9FFFFFF)  // rgba(255,255,255,0.85)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = borderAlpha),
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
fun AuroraBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F3F8)) // soft space-bg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = 1.4f
                    scaleY = 1.4f
                }
                .blur(70.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .drawWithCache {
                    onDrawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x220A84FF), Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.2f),
                                radius = size.width * 0.6f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x18FF2D55), Color.Transparent),
                                center = Offset(size.width * 0.8f, size.height * 0.35f),
                                radius = size.width * 0.5f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1C5E5CE6), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.8f),
                                radius = size.width * 0.55f
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x1830D158), Color.Transparent),
                                center = Offset(size.width * 0.75f, size.height * 0.75f),
                                radius = size.width * 0.45f
                            )
                        )
                    }
                }
        )
    }
}

val IosAvatarGradients = listOf(
    listOf(Color(0xFFE2E5E9), Color(0xFF9BA2AE)), // Classic Mac
    listOf(Color(0xFF7B7B81), Color(0xFF505054)), // Space Gray
    listOf(Color(0xFF242D3D), Color(0xFF11161F)), // Midnight
    listOf(Color(0xFF1E3A8A), Color(0xFF172554)), // Corporate Navy
    listOf(Color(0xFF475569), Color(0xFF1E293B)), // Slate Blue
    listOf(Color(0xFF166534), Color(0xFF14532D)), // Executive Forest
    listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)), // Burgundy Board
    listOf(Color(0xFF581C87), Color(0xFF3B0764)), // Deep Purple
    listOf(Color(0xFF0F766E), Color(0xFF042F2E)), // Teal Pro
    listOf(Color(0xFF3F3F46), Color(0xFF18181B)), // Charcoal
    listOf(Color(0xFF9A3412), Color(0xFF431407)), // Bronze Exec
    listOf(Color(0xFF64748B), Color(0xFF334155)), // Steel Grey
    listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)), // Royal Blue
    listOf(Color(0xFF059669), Color(0xFF064E3B)), // Emerald Focus
    listOf(Color(0xFFBE123C), Color(0xFF881337)), // Crimson Velvet
    listOf(Color(0xFF7E22CE), Color(0xFF4C1D95)), // Amethyst Dark
    listOf(Color(0xFF451A03), Color(0xFF291002)), // Dark Mocha
    listOf(Color(0xFF0369A1), Color(0xFF082F49)), // Ocean Depth
    listOf(Color(0xFF312E81), Color(0xFF1E1B4B)), // Sapphire Night
    listOf(Color(0xFFB45309), Color(0xFF78350F))  // Gold Standard
)

fun getInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "?"
    val parts = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

fun getAvatarGradient(key: String): List<Color> {
    val idx = abs(key.hashCode()) % IosAvatarGradients.size
    return IosAvatarGradients[idx]
}

@Composable
fun IosAvatarView(
    name: String,
    avatarUrl: String,
    size: Dp = 52.dp,
    isGroup: Boolean = false,
    isOnline: Boolean = false,
    gender: String? = null,
    modifier: Modifier = Modifier
) {
    val gradient = remember(name, avatarUrl) { getAvatarGradient(name.ifBlank { avatarUrl }) }
    val presetAvatarRes = remember(avatarUrl) { getAvatarPresetDrawableRes(avatarUrl) }
    val defaultAvatarRes = remember(gender) { getDefaultAvatarDrawableRes(gender) }
    val resolvedDrawable = presetAvatarRes ?: if (avatarUrl.isBlank()) defaultAvatarRes else null

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Avatar Base (Image, Preset Vector, Gender Default, or Monogram)
        if (resolvedDrawable != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.15f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = resolvedDrawable),
                    contentDescription = name.ifBlank { "Avatar" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentScale = ContentScale.Crop
            )
        } else if (isGroup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Users,
                    contentDescription = "Group",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        } else {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                val canvasSize = this.size
                // Background Gradient
                drawRect(brush = Brush.verticalGradient(gradient))
                
                // Silhouette Gradient (White to subtle gray)
                val silhouetteBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFD5D9E0))
                )

                val headRadius = canvasSize.height * 0.185f
                val headCenter = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.322f)
                val bodyRadius = canvasSize.height * 0.488f
                val bodyCenter = Offset(canvasSize.width * 0.5f, canvasSize.height * 1.035f)

                // Draw Drop Shadows using Native Canvas
                drawIntoCanvas { canvas ->
                    val paintAmbient = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.10f)
                    }
                    val frameworkPaintAmbient = paintAmbient.asFrameworkPaint()
                    // stdDeviation=24 in 512px viewbox -> 24/512 = 0.0468
                    val blurAmbient = (canvasSize.minDimension * 0.0468f).coerceAtLeast(1f)
                    frameworkPaintAmbient.maskFilter = android.graphics.BlurMaskFilter(blurAmbient, android.graphics.BlurMaskFilter.Blur.NORMAL)

                    val paintDirect = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.12f)
                    }
                    val frameworkPaintDirect = paintDirect.asFrameworkPaint()
                    // stdDeviation=6 in 512px viewbox -> 6/512 = 0.0117
                    val blurDirect = (canvasSize.minDimension * 0.0117f).coerceAtLeast(1f)
                    frameworkPaintDirect.maskFilter = android.graphics.BlurMaskFilter(blurDirect, android.graphics.BlurMaskFilter.Blur.NORMAL)

                    // Ambient shadow (dy=16 -> 16/512 = 0.03125)
                    val dyAmbient = canvasSize.height * 0.03125f
                    canvas.nativeCanvas.drawCircle(headCenter.x, headCenter.y + dyAmbient, headRadius, frameworkPaintAmbient)
                    canvas.nativeCanvas.drawCircle(bodyCenter.x, bodyCenter.y + dyAmbient, bodyRadius, frameworkPaintAmbient)

                    // Direct contact shadow (dy=4 -> 4/512 = 0.0078)
                    val dyDirect = canvasSize.height * 0.0078f
                    canvas.nativeCanvas.drawCircle(headCenter.x, headCenter.y + dyDirect, headRadius, frameworkPaintDirect)
                    canvas.nativeCanvas.drawCircle(bodyCenter.x, bodyCenter.y + dyDirect, bodyRadius, frameworkPaintDirect)
                }

                // Head
                drawCircle(brush = silhouetteBrush, radius = headRadius, center = headCenter)
                // Body
                drawCircle(brush = silhouetteBrush, radius = bodyRadius, center = bodyCenter)
                
                // Inner Border
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.2f))
                    ),
                    radius = canvasSize.minDimension / 2f - 1.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Online Status Dot
        if (!isGroup && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759))
            )
        }
    }
}

/**
 * Unified Apple Status / Story Avatar Ring brushes and colors.
 * - Active / Unseen: Apple Blue Gradient Circle
 * - Seen / Viewed: Neutral Apple System Gray Circle
 */
val AppleStatusBlueGradient = listOf(
    Color(0xFF007AFF), // Apple System Blue
    Color(0xFF00C6FF)  // Simple Sky Blue
)

val AppleStatusSeenGrayDark = listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E))
val AppleStatusSeenGrayLight = listOf(Color(0xFFD1D1D6), Color(0xFFC7C7CC))

fun getAppleStatusRingBrush(
    hasStatus: Boolean,
    isSeen: Boolean,
    isDark: Boolean
): Brush {
    return when {
        !hasStatus -> Brush.linearGradient(
            if (isDark) listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
            else listOf(Color(0xFFE5E5EA), Color(0xFFF2F2F7))
        )
        !isSeen -> Brush.linearGradient(
            listOf(Color(0xFF007AFF), Color(0xFF00C6FF))
        )
        else -> Brush.linearGradient(
            if (isDark) AppleStatusSeenGrayDark
            else AppleStatusSeenGrayLight
        )
    }
}

/**
 * Standard reusable Apple Status / Story Avatar with Blue Gradient (Unseen) and Gray (Seen) ring.
 */
@Composable
fun AppleStatusAvatar(
    name: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    avatarSize: Dp = 54.dp,
    hasStatus: Boolean = false,
    isSeen: Boolean = false,
    isOnline: Boolean = false,
    showAddBadge: Boolean = false,
    gender: String? = null,
    isGroup: Boolean = false,
    isDark: Boolean = false,
    gapColor: Color = if (isDark) Color(0xFF121212) else Color.White,
    onAddBadgeClick: (() -> Unit)? = null
) {
    val ringBrush = remember(hasStatus, isSeen, isDark) {
        getAppleStatusRingBrush(hasStatus = hasStatus, isSeen = isSeen, isDark = isDark)
    }
    val ringStroke = when {
        hasStatus && !isSeen -> 2.5.dp
        hasStatus && isSeen -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size - 2.dp)
                .clip(CircleShape)
                .background(ringBrush)
                .padding(ringStroke)
                .clip(CircleShape)
                .background(gapColor)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            IosAvatarView(
                name = name,
                avatarUrl = avatarUrl,
                size = avatarSize,
                gender = gender,
                isGroup = isGroup
            )
        }

        // Add (+) badge at bottom-end
        if (showAddBadge) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .border(2.dp, gapColor, CircleShape)
                    .then(
                        if (onAddBadgeClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onAddBadgeClick() }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Add Story",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else if (isOnline) {
            // Online green indicator dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .background(gapColor, CircleShape)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759))
            )
        }
    }
}


@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int>? = null,
    icons: List<androidx.compose.ui.graphics.vector.ImageVector?>? = null
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .shadow(2.dp, shape, ambientColor = Color.Black.copy(0.04f), spotColor = Color.Black.copy(0.04f))
            .clip(shape)
            .background(Color(0x24767680)) // iOS standard translucent fill
            .border(0.75.dp, Color.White.copy(alpha = 0.4f), shape)
            .padding(2.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
                    label = "text_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .shadow(3.dp, RoundedCornerShape(7.dp), spotColor = Color.Black.copy(0.08f))
                                    .background(Color.White, RoundedCornerShape(7.dp))
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val icon = icons?.getOrNull(index)
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (index == 2) Color(0xFFFF2D55) else textColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = title,
                            color = textColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        val count = counts?.getOrNull(index) ?: 0
                        if (count > 0) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isSelected) Color(0xFF007AFF) else Color(0x308E8E93))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    color = if (isSelected) Color.White else Color(0xFF8E8E93),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSeamlessTabBar(
    icons: List<ImageVector>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color(0xFF007AFF),
    unselectedColor: Color = Color(0xFF8E8E93)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                drawLine(
                    color = Color(0xFF8E8E93).copy(alpha = 0.2f),
                    start = Offset(0f, size.height - strokeWidth / 2f),
                    end = Offset(size.width, size.height - strokeWidth / 2f),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        val tabCount = icons.size
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tab_indicator_anim"
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icons.forEachIndexed { index, icon ->
                val isSelected = selectedIndex == index
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else unselectedColor,
                    animationSpec = tween(180),
                    label = "tab_icon_color"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "tab_icon_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(21.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }

        // Smooth sliding active underline indicator
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / tabCount
            val indicatorWidth = tabWidth * 0.45f
            val indicatorOffset = (tabWidth * animatedIndex) + ((tabWidth - indicatorWidth) / 2f)

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(2.5.dp)
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(selectedColor)
            )
        }
    }
}

@Composable
fun IosIconSegmentedControl(
    icons: List<androidx.compose.ui.graphics.vector.ImageVector>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color(0xFF007AFF),
    unselectedColor: Color = Color(0xFF8E8E93)
) {
    ProfileSeamlessTabBar(
        icons = icons,
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected,
        modifier = modifier,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor
    )
}

@Composable
fun VoiceWaveform(isRecording: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val heights = List(5) { index ->
        val target = if (isRecording) (10..24).random().toFloat() else 4f
        val delay = index * 100
        infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400 + delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(24.dp)
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.value.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF30D158), Color(0xFF0A84FF))
                        )
                    )
            )
        }
    }
}
