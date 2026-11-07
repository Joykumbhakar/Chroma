package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.chroma.studio.ui.theme.LocalOscellionColors

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide

private val isAppInDark: Boolean
    @Composable
    get() = LocalOscellionColors.current.isDark

/**
 * Remember and sync Samsung One UI 7.0 Gradient Style from SharedPreferences.
 */
@Composable
fun rememberOneUiGradientStyle(): Pair<OneUiGradientStyle, () -> Unit> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }
    var currentStyle by remember {
        val savedName = prefs.getString("one_ui_gradient_style", OneUiGradientStyle.GALAXY_AI.name)
        val style = try {
            OneUiGradientStyle.valueOf(savedName ?: "GALAXY_AI")
        } catch (e: Exception) {
            OneUiGradientStyle.GALAXY_AI
        }
        mutableStateOf(style)
    }

    val cycleStyle = {
        val styles = OneUiGradientStyle.entries
        val nextIndex = (styles.indexOf(currentStyle) + 1) % styles.size
        val next = styles[nextIndex]
        currentStyle = next
        prefs.edit().putString("one_ui_gradient_style", next.name).apply()
        SoundManager.playReaction()
    }

    return Pair(currentStyle, cycleStyle)
}

/**
 * Samsung One UI 7.0 Viewing Area (Expanded Header Zone)
 * Matches exact One UI reference geometry, typography, and solid background.
 */
@Composable
fun OneUiViewingArea(
    title: String,
    subtitle: String? = null,
    gradientStyle: OneUiGradientStyle = OneUiGradientStyle.SOLID,
    onCycleGradient: () -> Unit = {},
    showBadge: Boolean = false,
    height: Dp = 240.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDark
    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F4F7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(screenBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showBadge) {
                Surface(
                    onClick = onCycleGradient,
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White else Color.Black)
                        )
                        Text(
                            text = "ONE UI • ${title.uppercase()}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                            letterSpacing = 0.6.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = title,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                letterSpacing = (-0.6).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = InterFontFamily
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

/**
 * Samsung One UI 7.0 Sticky Compact Toolbar
 */
@Composable
fun OneUiStickyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    gradientStyle: OneUiGradientStyle = OneUiGradientStyle.GALAXY_AI,
    onCycleGradient: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDark
    val barBg = if (isDark) Color(0xFF000000).copy(alpha = 0.92f) else Color(0xFFF2F4F7).copy(alpha = 0.94f)
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)

    Surface(
        color = barBg,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = {
                            SoundManager.playReaction()
                            onBack()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = textDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textDark,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

/**
 * Iconic Double Edge Glass Border Tokens & Modifier
 * Signature Oscillom / Samsung One UI multi-layer specular rim
 */
val IconicGlassBorderOuterBrush = Brush.linearGradient(
    0.00f to Color.White.copy(alpha = 0.58f),
    0.28f to Color(0xFF38BDF8).copy(alpha = 0.42f),
    0.65f to Color(0xFF0381FE).copy(alpha = 0.28f),
    1.00f to Color.White.copy(alpha = 0.38f),
    start = Offset.Zero,
    end = Offset.Infinite
)

val IconicGlassBorderInnerBrush = Brush.linearGradient(
    0.00f to Color.White.copy(alpha = 0.32f),
    0.50f to Color.Transparent,
    1.00f to Color(0xFF38BDF8).copy(alpha = 0.22f),
    start = Offset.Zero,
    end = Offset.Infinite
)

/**
 * Iconic Double Edge Glass Border:
 * Multi-layer specular glass rim combining:
 * 1. Outer hairline stroke: Multi-stop linear gradient from crisp white specular catch (0.58f)
 *    to cyan lens bloom (0.42f) and Galaxy AI deep blue (0.28f).
 * 2. Inner refracted stroke: Inset specular meniscus highlight creating the signature double-edge refraction.
 *
 * Designed for Samsung One UI cards, squircle containers, dialogs, and panels.
 * (Note: Never applied to pill/capsule shapes per standing user rules).
 */
fun Modifier.doubleEdgeGlassBorder(
    cornerRadius: Dp = 26.dp,
    outerWidth: Dp = 1.dp,
    innerWidth: Dp = 1.dp,
    inset: Dp = 1.2.dp,
    outerBorderBrush: Brush = IconicGlassBorderOuterBrush,
    innerBorderBrush: Brush = IconicGlassBorderInnerBrush
): Modifier = this.drawWithCache {
    val cornerRadiusPx = cornerRadius.toPx()
    val outerStrokePx = outerWidth.toPx()
    val innerStrokePx = innerWidth.toPx()
    val halfOuter = outerStrokePx / 2f
    val insetPx = outerStrokePx + inset.toPx()

    onDrawWithContent {
        drawContent()

        // 1. Outer crisp specular rim
        drawRoundRect(
            brush = outerBorderBrush,
            topLeft = Offset(halfOuter, halfOuter),
            size = Size(size.width - outerStrokePx, size.height - outerStrokePx),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = outerStrokePx)
        )

        // 2. Inner refracted glass edge (the iconic second edge)
        if (size.width > insetPx * 2 && size.height > insetPx * 2) {
            val innerCornerRadiusPx = (cornerRadiusPx - insetPx).coerceAtLeast(0f)
            drawRoundRect(
                brush = innerBorderBrush,
                topLeft = Offset(insetPx, insetPx),
                size = Size(size.width - insetPx * 2, size.height - insetPx * 2),
                cornerRadius = CornerRadius(innerCornerRadiusPx, innerCornerRadiusPx),
                style = Stroke(width = innerStrokePx)
            )
        }
    }
}

/**
 * Samsung One UI Glass Card:
 * 26dp squircle container with deep frosted glass background and the iconic double edge glass border.
 */
@Composable
fun OneUiGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    backgroundColor: Color? = null,
    useDoubleEdgeGlassBorder: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isAppInDark
    val cardBg = backgroundColor ?: if (isDark) Color(0xFF10141E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .then(
                if (useDoubleEdgeGlassBorder) {
                    Modifier.doubleEdgeGlassBorder(cornerRadius = cornerRadius)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Samsung One UI 7.0 Settings Group Container (26dp Squircle) with Iconic Double Edge Glass Border
 */
@Composable
fun OneUiSettingsGroup(
    title: String = "",
    header: String = title,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    useDoubleEdgeGlassBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isAppInDark
    val cardBg = if (isDark) Color(0xFF10141E) else Color(0xFFFFFFFF)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val displayTitle = if (title.isNotEmpty()) title else header

    Column(modifier = modifier.fillMaxWidth()) {
        if (displayTitle.isNotEmpty()) {
            Text(
                text = displayTitle.uppercase(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textMuted,
                letterSpacing = 0.8.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(cardBg)
                .then(
                    if (useDoubleEdgeGlassBorder) {
                        Modifier.doubleEdgeGlassBorder(cornerRadius = cornerRadius)
                    } else {
                        Modifier.border(1.dp, if (isDark) Color(0xFF1F2430) else Color.Transparent, RoundedCornerShape(cornerRadius))
                    }
                ),
            content = content
        )
    }
}

/**
 * Specular shine light gradient border for settings icon badges:
 * Illuminates the top-left corner (0f, 0f) and bottom-right corner (width, height),
 * fading to transparent along the top-right and bottom-left edges.
 */
val SettingsShineBorderBrush = Brush.linearGradient(
    0.00f to Color.White.copy(alpha = 0.85f),
    0.20f to Color.White.copy(alpha = 0.22f),
    0.36f to Color.Transparent,
    0.64f to Color.Transparent,
    0.80f to Color.White.copy(alpha = 0.16f),
    1.00f to Color.White.copy(alpha = 0.52f),
    start = Offset.Zero,
    end = Offset.Infinite
)

/**
 * Authentic Apple-style clean vertical gradient for settings icons:
 * For example: linear-gradient(180deg, #0055ff 0%, #0022cc 100%)
 */
fun getAppleCleanIconGradient(baseColor: Color): List<Color> {
    val appleCleanBlueGradient = listOf(Color(0xFF0055FF), Color(0xFF0022CC))
    val r = baseColor.red
    val g = baseColor.green
    val b = baseColor.blue

    // Blue family (e.g. 0xFF0381FE, 0xFF0381FE, 0xFF0095FF)
    if (b > 0.70f && r < 0.35f) {
        return listOf(Color(0xFF0055FF), Color(0xFF0022CC))
    }
    // Green family (e.g. 0xFF34C759, 0xFF30D158, 0xFF10B981, 0xFF22C55E)
    if (g > 0.60f && r < 0.40f && b < 0.50f) {
        return listOf(Color(0xFF34D465), Color(0xFF1E8738))
    }
    // Red family (e.g. 0xFFFF3B30, 0xFFFF453A, 0xFFEF4444)
    if (r > 0.85f && g < 0.35f && b < 0.35f) {
        return listOf(Color(0xFFFF453A), Color(0xFFBA1A1A))
    }
    // Orange family (e.g. 0xFFFF9500, 0xFFFF9F0A, 0xFFF97316)
    if (r > 0.85f && g in 0.35f..0.70f && b < 0.25f) {
        return listOf(Color(0xFFFF9F0A), Color(0xFFC93E00))
    }
    // Yellow family (e.g. 0xFFFFCC00, 0xFFEAB308)
    if (r > 0.85f && g > 0.70f && b < 0.25f) {
        return listOf(Color(0xFFFFD60A), Color(0xFFCC8400))
    }
    // Purple / Violet family (e.g. 0xFFAF52DE, 0xFFBF5AF2, 0xFF8B5CF6)
    if (r in 0.45f..0.85f && g < 0.45f && b > 0.70f) {
        return listOf(Color(0xFFBF5AF2), Color(0xFF6E28A0))
    }
    // Pink / Magenta family (e.g. 0xFFFF2D55, 0xFFEC4899)
    if (r > 0.85f && g < 0.35f && b in 0.30f..0.60f) {
        return listOf(Color(0xFFFF375F), Color(0xFFBA0F37))
    }
    // Teal / Cyan family (e.g. 0xFF5AC8FA, 0xFF06B6D4)
    if (b > 0.75f && g > 0.65f) {
        return listOf(Color(0xFF64D2FF), Color(0xFF0073A8))
    }
    // Indigo family (e.g. 0xFF5856D6, 0xFF6366F1)
    if (b > 0.70f && r in 0.30f..0.55f && g < 0.45f) {
        return listOf(Color(0xFF5E5CE6), Color(0xFF3330A2))
    }
    // Gray / Neutral family (e.g. 0xFF8E8E93, 0xFF6B7280)
    if (kotlin.math.abs(r - g) < 0.08f && kotlin.math.abs(g - b) < 0.08f) {
        return listOf(Color(0xFF98989D), Color(0xFF48484A))
    }

    val topColor = Color(
        red = (r * 1.05f).coerceIn(0f, 1f),
        green = (g * 1.05f).coerceIn(0f, 1f),
        blue = (b * 1.05f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val bottomColor = Color(
        red = (r * 0.58f).coerceIn(0f, 1f),
        green = (g * 0.58f).coerceIn(0f, 1f),
        blue = (b * 0.65f).coerceIn(0f, 1f),
        alpha = 1f
    )
    return listOf(topColor, bottomColor)
}

/**
 * Settings Icon Badge with 11dp squircle (decreased by 5px from 16dp),
 * authentic Apple-style vertical clean gradient background, and specular shine border.
 */
@Composable
fun OneUiIconBadge(
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF0055FF),
    iconGradient: List<Color>? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    val gradient = iconGradient ?: getAppleCleanIconGradient(iconColor)
    val bgModifier = Modifier.background(Brush.verticalGradient(gradient))

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(shape)
            .then(bgModifier)
            .border(width = 0.9.dp, brush = SettingsShineBorderBrush, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Samsung One UI 7.0 Settings Row with Squircle Icon Badge
 */
@Composable
fun OneUiSettingsNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF0055FF),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    valueText: String? = null,
    valueTextColor: Color? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDark
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val effectiveTint = if (iconTint != Color(0xFF0055FF) && iconTint != Color(0xFF0381FE)) iconTint else iconColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                SoundManager.playReaction()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null || iconGradient != null) {
                OneUiIconBadge(
                    icon = icon,
                    iconColor = effectiveTint,
                    iconGradient = iconGradient
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) Color(0xFFEF4444) else textDark,
                    fontFamily = InterFontFamily
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = textMuted,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = valueTextColor ?: textMuted,
                    fontFamily = InterFontFamily
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Samsung One UI 7.0 Settings Toggle Row
 */
@Composable
fun OneUiSettingsToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF0055FF),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    infoOnClick: (() -> Unit)? = null,
    activeTrackColor: Color = Color(0xFF0055FF),
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDark
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val effectiveTint = if (iconTint != Color(0xFF0055FF) && iconTint != Color(0xFF0381FE)) iconTint else iconColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                SoundManager.playReaction()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null || iconGradient != null) {
                OneUiIconBadge(
                    icon = icon,
                    iconColor = effectiveTint,
                    iconGradient = iconGradient
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textDark,
                    fontFamily = InterFontFamily
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = textMuted,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (infoOnClick != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            SoundManager.playReaction()
                            infoOnClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = "Info",
                        tint = textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AppleSwitch(
                checked = checked,
                onCheckedChange = {
                    onCheckedChange(it)
                },
                activeTrackColor = activeTrackColor
            )
        }
    }
}

/**
 * Samsung One UI 7.0 Settings Slider Row
 */
@Composable
fun OneUiSettingsSliderRow(
    title: String,
    valueText: String,
    valueTextColor: Color = Color(0xFF0381FE),
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF0055FF),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDark
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val effectiveTint = if (iconTint != Color(0xFF0055FF) && iconTint != Color(0xFF0381FE)) iconTint else iconColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null || iconGradient != null) {
                    OneUiIconBadge(
                        icon = icon,
                        iconColor = effectiveTint,
                        iconGradient = iconGradient
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textDark,
                    fontFamily = InterFontFamily
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = valueTextColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueTextColor,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF0381FE),
                activeTrackColor = Color(0xFF0381FE),
                inactiveTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Samsung One UI 7.0 Settings Divider
 */
@Composable
fun OneUiSettingsDivider(
    modifier: Modifier = Modifier,
    paddingStart: Dp = 16.dp
) {
    val isDark = isAppInDark
    val dividerColor = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0)
    HorizontalDivider(
        modifier = modifier.padding(start = paddingStart, end = 16.dp),
        thickness = 0.75.dp,
        color = dividerColor
    )
}
