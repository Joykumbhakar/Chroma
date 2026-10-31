package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

/**
 * Standard Samsung One UI 7 Collapsing Header Scaffold.
 * Matches the exact proportions, physics, and typography of the Settings screen.
 */
@Composable
fun OneUiCollapsingHeaderLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backIcon: ImageVector = Lucide.ArrowLeft,
    backContentDescription: String = "Back",
    expandedHeightDp: Dp = 190.dp,
    headerLeadingContent: (@Composable RowScope.(compactTitleAlpha: Float) -> Unit)? = null,
    headerActions: (@Composable RowScope.(compactTitleAlpha: Float) -> Unit)? = null,
    content: @Composable BoxScope.(
        collapseProgress: Float
    ) -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // One UI Theme Synchronization
    val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }
    val gradientStyle = remember {
        val savedName = prefs.getString("one_ui_gradient_style", OneUiGradientStyle.GALAXY_AI.name)
        try {
            OneUiGradientStyle.valueOf(savedName ?: "GALAXY_AI")
        } catch (_: Exception) {
            OneUiGradientStyle.GALAXY_AI
        }
    }

    val screenBgColor = if (!isDark) Color(0xFFF8FAFC) else Color(0xFF000000)
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.07f)
    val dividerColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val textDark = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
    val activeAccent = if (gradientStyle == OneUiGradientStyle.SOLID) Color(0xFF007AFF) else gradientStyle.primaryColor

    // One UI Collapsing Engine
    val maxCollapsePx = remember(density, expandedHeightDp) { with(density) { expandedHeightDp.toPx() } }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }

    val collapseProgress = remember(collapseOffsetPx, maxCollapsePx) {
        if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
    }

    val largeTitleAlpha = (1f - (collapseProgress / 0.45f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.35f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val gradientAlpha = if (gradientStyle == OneUiGradientStyle.SOLID) 0f else (1f - (collapseProgress / 0.85f)).coerceIn(0f, 1f)

    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < 0f && collapseOffsetPx < maxCollapsePx) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaY = available.y
                if (deltaY > 0f && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                if (progress > 0.02f && progress < 0.98f) {
                    val targetPx = when {
                        available.y < -350f -> maxCollapsePx
                        available.y > 350f -> 0f
                        progress >= 0.5f -> maxCollapsePx
                        else -> 0f
                    }
                    animate(
                        initialValue = collapseOffsetPx,
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { value, _ ->
                        collapseOffsetPx = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val currentExpandedHeightDp = with(density) {
        (maxCollapsePx - collapseOffsetPx).coerceAtLeast(0f).toDp()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Exact Chroma Studio System Gradient Backdrop
        ChromaOneUiHeaderBackdrop(
            expandedHeightDp = expandedHeightDp,
            collapseProgress = collapseProgress,
            gradientAlpha = gradientAlpha,
            isDark = isDark,
            screenBgColor = screenBgColor
        )

        Column(modifier = Modifier.fillMaxSize()) {
            val toolbarBgAlpha = (collapseProgress * 0.94f).coerceIn(0f, 0.94f)

            // Top Status Bar Spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            )

            // 2. Expanded Viewing Area (Collapses smoothly on scroll)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentExpandedHeightDp),
                contentAlignment = Alignment.Center
            ) {
                if (largeTitleAlpha > 0.01f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                alpha = largeTitleAlpha
                                translationY = -collapseProgress * with(density) { 20.dp.toPx() }
                                val scale = 1f - (collapseProgress * 0.05f)
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        Text(
                            text = title,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp,
                            color = textDark,
                            textAlign = TextAlign.Center
                        )

                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            )
                        }
                    }
                }
            }

            // 3. Sticky Compact One UI Toolbar (Pinned at 56dp height)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Back button or Custom Leading + Compact Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (onBack != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        SoundManager.playReaction()
                                        onBack()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = backIcon,
                                    contentDescription = backContentDescription,
                                    tint = textDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (headerLeadingContent != null) {
                            headerLeadingContent(compactTitleAlpha)
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = compactTitleAlpha
                                translationY = (1f - compactTitleAlpha) * with(density) { 4.dp.toPx() }
                            }
                        )
                    }

                    // Right Side: Action Items
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (headerActions != null) {
                            headerActions(compactTitleAlpha)
                        }
                    }
                }

                if (collapseProgress > 0.85f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(dividerColor)
                    )
                }
            }

            // 4. Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content(collapseProgress)
            }
        }
    }
}

/**
 * Standard One UI Squircle Search Bar (identical to Settings screen search bar).
 */
@Composable
fun OneUiSquircleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onClear: () -> Unit = { onQueryChange("") }
) {
    val isDark = isSystemInDarkTheme()
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.07f)
    val textDark = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (!isDark) Color(0xFFE9E9EE) else Color(0xFF1E1E22))
            .border(1.dp, cardBorderColor, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        fontFamily = InterFontFamily,
                        color = textMuted
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontFamily = InterFontFamily,
                        color = textDark,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(if (!isDark) Color(0xFF007AFF) else Color(0xFF0A84FF)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (!isDark) Color.Black.copy(0.08f) else Color.White.copy(0.12f))
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


/**
 * Exact Chroma Studio System Gradient Backdrop for all One UI Collapsing Headers.
 * Matches CSS .chroma-bg and JSON spec:
 * - Base background: #F8FAFC (light) / #000000 (dark)
 * - Linear Gradient at 91.27742 deg: rgba(0, 71, 255, 0.34901962) 0% -> rgba(255, 255, 255, 0.0) 100%
 * - Blob 1 (x: 25%, y: 40%, feather: 100%)
 * - Blob 2 (x: 75%, y: 60%, feather: 100%)
 * - Smooth bottom feather into screen background.
 */
@Composable
fun ChromaOneUiHeaderBackdrop(
    expandedHeightDp: Dp,
    collapseProgress: Float,
    gradientAlpha: Float = 1f,
    isDark: Boolean = isSystemInDarkTheme(),
    screenBgColor: Color = if (!isDark) Color(0xFFF8FAFC) else Color(0xFF000000),
    modifier: Modifier = Modifier
) {
    if (gradientAlpha <= 0.005f) return
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(expandedHeightDp + 150.dp)
            .graphicsLayer {
                alpha = gradientAlpha
                translationY = -collapseProgress * with(density) { 42.dp.toPx() }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height

                    // 91.27742 deg linear gradient (slight ~1.28 deg tilt downward from left-to-right)
                    val angleRad = Math.toRadians(91.27742 - 90.0)
                    val endY = (w * kotlin.math.tan(angleRad)).toFloat().coerceAtLeast(0f)

                    val startColor = if (!isDark) Color(0, 71, 255, (0.34901962f * 255).toInt())
                                     else Color(0, 71, 255, (0.42f * 255).toInt())

                    val baseLinearBrush = Brush.linearGradient(
                        colors = listOf(
                            startColor,
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, endY)
                    )

                    // Vertical atmospheric wash (top to bottom)
                    val verticalWashBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0, 71, 255, if (!isDark) (0.16f * 255).toInt() else (0.24f * 255).toInt()),
                            Color.Transparent
                        )
                    )

                    // Blob 1: x = 25.0%, y = 40.0%, feather = 100%
                    val blob1Brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0, 71, 255, if (!isDark) (0.25f * 255).toInt() else (0.32f * 255).toInt()),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.25f, h * 0.40f),
                        radius = (w * 0.40f).coerceAtLeast(100f)
                    )

                    // Blob 2: x = 75.0%, y = 60.0%, feather = 100%
                    val blob2Brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0, 71, 255, if (!isDark) (0.18f * 255).toInt() else (0.26f * 255).toInt()),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.75f, h * 0.60f),
                        radius = (w * 0.40f).coerceAtLeast(100f)
                    )

                    // Bottom feather to seamlessly transition into screen background
                    val bottomFeatherBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            screenBgColor
                        ),
                        startY = (h - 85.dp.toPx()).coerceAtLeast(0f),
                        endY = h
                    )

                    onDrawBehind {
                        drawRect(baseLinearBrush)
                        drawRect(verticalWashBrush)
                        drawRect(blob1Brush)
                        drawRect(blob2Brush)
                        drawRect(bottomFeatherBrush)
                    }
                }
        )
    }
}
