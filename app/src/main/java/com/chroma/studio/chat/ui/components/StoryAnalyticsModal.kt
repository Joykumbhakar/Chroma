package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors

/**
 * Flagship Professional Apple iOS Story Insights & Analytics Dashboard.
 * Features modular KPI cards, interactive Bezier curve activity charts,
 * viewer interaction tracking, and safe bottom navigation padding.
 */
@Composable
fun StoryAnalyticsModal(
    story: Story,
    onClose: () -> Unit,
    onDeleteStory: (String) -> Unit
) {
    val colors = LocalChatColors.current
    var selectedFilter by remember { mutableStateOf("all") } // "all", "reactions", "replies"
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Story?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark
                )
            },
            text = {
                Text(
                    text = "This story and all its analytics and insights will be permanently deleted.",
                    fontSize = 14.sp,
                    color = colors.textMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SoundManager.playReaction()
                        showDeleteConfirmDialog = false
                        onDeleteStory(story.id)
                        onClose()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = colors.iosBlue,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = if (colors.isLight) Color.White else Color(0xFF2C2C2E)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() }
            .statusBarsPadding()
            .padding(top = 28.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Bottom Sheet (Apple iOS Frosted Glass Sheet Surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(if (colors.isLight) Color(0xFFFFFFFF) else Color(0xFF1C1C1E))
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // ── Top Handle & Header ──────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.textMuted.copy(alpha = 0.30f))
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Story Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                            ) {
                                if (story.image != null) {
                                    AsyncImage(
                                        model = story.image,
                                        contentDescription = "Story preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (story.bgGradientColors != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(story.bgGradientColors))
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Story Insights",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textDark,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "Posted ${story.timeAgo} • Shared with contacts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Right Actions (Delete Story & Close)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30).copy(alpha = 0.10f))
                                    .clickable {
                                        SoundManager.playReaction()
                                        showDeleteConfirmDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideTrash(tint = Color(0xFFFF3B30), size = 15.dp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceSecondary)
                                    .clickable {
                                        SoundManager.playReaction()
                                        onClose()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCloseX(tint = colors.textMuted, size = 13.dp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── 1. Professional Modular KPI Metric Cards ──────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Views
                    ProfessionalKpiCard(
                        title = "Views",
                        value = story.viewCount.toString(),
                        badgeText = "94% retained",
                        badgeColor = Color(0xFF34C759),
                        icon = { LucideEye(tint = colors.iosBlue, size = 14.dp) },
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Reactions
                    ProfessionalKpiCard(
                        title = "Reactions",
                        value = story.likesCount.toString(),
                        badgeText = "28.4% rate",
                        badgeColor = Color(0xFF34C759),
                        icon = { LucideHeart(tint = Color(0xFFFF3B30), isFilled = true, size = 14.dp) },
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Replies
                    ProfessionalKpiCard(
                        title = "Replies",
                        value = "6",
                        badgeText = "Direct DMs",
                        badgeColor = colors.textMuted,
                        icon = { LucideMessageSquare(tint = Color(0xFFAF52DE), size = 14.dp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── 2. Interactive Hourly Activity Chart ──────────────────────
                StoryEngagementGraph(viewCount = story.viewCount)

                Spacer(Modifier.height(12.dp))

                // ── 3. Apple UISegmentedControl Tab Switcher ───────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(2.5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf(
                        "all" to "All Viewers (${story.viewers.size})",
                        "reactions" to "Reactions (${story.viewers.count { it.reactedEmoji != null }})",
                        "replies" to "Replies (2)"
                    )
                    tabs.forEach { (key, title) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) (if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                    else Color.Transparent
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(
                                            elevation = 1.5.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.08f),
                                            spotColor = Color.Black.copy(alpha = 0.08f)
                                        )
                                    } else Modifier
                                )
                                .border(
                                    width = if (isSelected) 0.5.dp else 0.dp,
                                    color = if (isSelected) colors.borderSubtle else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    SoundManager.playReaction()
                                    selectedFilter = key
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.textDark else colors.textMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── 4. Viewers Interaction List ────────────────────────────────
                val displayedViewers = when (selectedFilter) {
                    "reactions" -> story.viewers.filter { it.reactedEmoji != null }
                    else -> story.viewers
                }

                if (displayedViewers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No interactions in this category yet",
                            fontSize = 13.5.sp,
                            color = colors.textMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(displayedViewers) { index, viewer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {}
                                    .padding(vertical = 9.dp, horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Contact Avatar with Reaction Pip
                                    Box(modifier = Modifier.size(44.dp)) {
                                        AsyncImage(
                                            model = viewer.avatar,
                                            contentDescription = viewer.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(0.5.dp, colors.borderSubtle, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (viewer.reactedEmoji != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(19.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .background(if (colors.isLight) Color.White else Color(0xFF1C1C1E), CircleShape)
                                                    .border(1.dp, colors.borderSubtle, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(viewer.reactedEmoji, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = viewer.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${viewer.username} • ${viewer.timeAgo}",
                                            fontSize = 12.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                // Right Reaction Badge
                                if (viewer.reactedEmoji != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.iosBlueSubtle)
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${viewer.reactedEmoji} Reacted",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.iosBlue
                                        )
                                    }
                                }
                            }

                            if (index < displayedViewers.size - 1) {
                                HorizontalDivider(
                                    color = colors.separator,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── 5. Full Width Primary Apple Done Button ────────────────────
                Button(
                    onClick = {
                        SoundManager.playReaction()
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Clean Professional KPI Metric Card
 */
@Composable
private fun ProfessionalKpiCard(
    title: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceSecondary)
            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted
            )
        }

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textDark
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}

/**
 * Animated iOS Engagement & Hourly Viewership Graph.
 * Uses cubic Bezier curves with horizontal gridlines and glowing peak indicators.
 */
@Composable
fun StoryEngagementGraph(
    viewCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current

    var animationTrigger by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "graphAnimation"
    )

    LaunchedEffect(Unit) {
        animationTrigger = true
    }

    // Pulse animation for the peak indicator point
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (colors.isLight) Color(0xFFF9F9FB) else Color(0xFF252528))
            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Graph Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(colors.iosBlue, CircleShape)
                )
                Text(
                    text = "HOURLY VIEW ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF34C759).copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = "Peak: 48 views/hr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF34C759)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Canvas Bezier Graph with horizontal guidelines
        val dataPoints = remember {
            listOf(0.12f, 0.35f, 0.55f, 0.95f, 0.70f, 0.82f, 0.45f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / (dataPoints.size - 1)

                // Faint horizontal baseline guides (25%, 50%, 75%)
                val guideAlpha = 0.07f
                val strokeGuide = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
                drawLine(
                    color = colors.textDark.copy(alpha = guideAlpha),
                    start = Offset(0f, height * 0.25f),
                    end = Offset(width, height * 0.25f),
                    strokeWidth = strokeGuide.width,
                    pathEffect = strokeGuide.pathEffect
                )
                drawLine(
                    color = colors.textDark.copy(alpha = guideAlpha),
                    start = Offset(0f, height * 0.60f),
                    end = Offset(width, height * 0.60f),
                    strokeWidth = strokeGuide.width,
                    pathEffect = strokeGuide.pathEffect
                )

                val graphPoints = dataPoints.mapIndexed { idx, value ->
                    val animatedY = height - ((value * height * 0.82f) * animProgress)
                    Offset(idx * spacing, animatedY)
                }

                // Build smooth Cubic Bezier Path
                val strokePath = Path().apply {
                    if (graphPoints.isNotEmpty()) {
                        moveTo(graphPoints[0].x, graphPoints[0].y)
                        for (i in 0 until graphPoints.size - 1) {
                            val p0 = graphPoints[i]
                            val p1 = graphPoints[i + 1]
                            val controlX1 = (p0.x + p1.x) / 2
                            val controlY1 = p0.y
                            val controlX2 = (p0.x + p1.x) / 2
                            val controlY2 = p1.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }
                    }
                }

                // Build Gradient Fill Area Path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.iosBlue.copy(alpha = 0.25f * animProgress),
                            colors.iosBlue.copy(alpha = 0.01f * animProgress)
                        ),
                        startY = 0f,
                        endY = height
                    ),
                    style = Fill
                )

                // Draw Stroke Line
                drawPath(
                    path = strokePath,
                    color = colors.iosBlue,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Peak Indicator (at index 3)
                if (graphPoints.size > 3 && animProgress > 0.6f) {
                    val peakPoint = graphPoints[3]
                    // Outer pulse halo
                    drawCircle(
                        color = colors.iosBlue.copy(alpha = 0.20f),
                        radius = 9.dp.toPx() * pulseScale,
                        center = peakPoint
                    )
                    // Inner solid dot
                    drawCircle(
                        color = colors.iosBlue,
                        radius = 4.5.dp.toPx(),
                        center = peakPoint
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = peakPoint
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Time Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("12h ago", "8h ago", "4h ago", "2h ago", "1h ago", "Just now").forEach { timeLabel ->
                Text(
                    text = timeLabel,
                    fontSize = 10.sp,
                    color = colors.textMuted.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
