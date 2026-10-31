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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

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
 */
@Composable
fun OneUiViewingArea(
    title: String,
    subtitle: String? = null,
    gradientStyle: OneUiGradientStyle = OneUiGradientStyle.GALAXY_AI,
    onCycleGradient: () -> Unit = {},
    showBadge: Boolean = false,
    height: Dp = 175.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F4F7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (gradientStyle != OneUiGradientStyle.SOLID) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gradientStyle.primaryColor.copy(alpha = if (isDark) 0.35f else 0.18f),
                            gradientStyle.secondaryColor.copy(alpha = if (isDark) 0.25f else 0.12f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientStyle.orbColor.copy(alpha = if (isDark) 0.40f else 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.28f),
                        radius = w * 0.65f
                    )
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        0.60f to Color.Transparent,
                        1.0f to screenBg
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showBadge) {
                Surface(
                    onClick = onCycleGradient,
                    shape = RoundedCornerShape(999.dp),
                    color = if (gradientStyle == OneUiGradientStyle.SOLID) {
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    } else {
                        gradientStyle.primaryColor.copy(alpha = 0.16f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (gradientStyle == OneUiGradientStyle.SOLID) {
                            if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
                        } else {
                            gradientStyle.primaryColor.copy(alpha = 0.30f)
                        }
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
                                .background(
                                    if (gradientStyle == OneUiGradientStyle.SOLID) {
                                        if (isDark) Color.White else Color.Black
                                    } else {
                                        gradientStyle.orbColor
                                    }
                                )
                        )
                        Text(
                            text = "ONE UI 7.0 • ${gradientStyle.label.uppercase()}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            Text(
                text = title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                letterSpacing = (-0.5).sp,
                fontFamily = InterFontFamily
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
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
    val isDark = isSystemInDarkTheme()
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
 * Samsung One UI 7.0 Settings Group Container (26dp Squircle)
 */
@Composable
fun OneUiSettingsGroup(
    title: String = "",
    header: String = title,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF12141A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF1F2430) else Color.Transparent
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
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(26.dp)),
            content = content
        )
    }
}

/**
 * Samsung One UI 7.0 Squircle Icon Badge
 */
@Composable
fun OneUiIconBadge(
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF2563EB),
    iconGradient: List<Color>? = null,
    modifier: Modifier = Modifier
) {
    val bgModifier = if (iconGradient != null && iconGradient.isNotEmpty()) {
        Modifier.background(Brush.linearGradient(iconGradient))
    } else {
        Modifier.background(iconColor.copy(alpha = 0.12f))
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(bgModifier),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (iconGradient != null) Color.White else iconColor,
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
    iconColor: Color = Color(0xFF2563EB),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    valueText: String? = null,
    valueTextColor: Color? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val effectiveTint = if (iconTint != Color(0xFF2563EB)) iconTint else iconColor

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
    iconColor: Color = Color(0xFF2563EB),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val effectiveTint = if (iconTint != Color(0xFF2563EB)) iconTint else iconColor

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

        Switch(
            checked = checked,
            onCheckedChange = {
                SoundManager.playReaction()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                uncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )
        )
    }
}

/**
 * Samsung One UI 7.0 Settings Slider Row
 */
@Composable
fun OneUiSettingsSliderRow(
    title: String,
    valueText: String,
    valueTextColor: Color = Color(0xFF2563EB),
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF2563EB),
    iconTint: Color = iconColor,
    iconGradient: List<Color>? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textDark = if (isDark) Color.White else Color(0xFF0F172A)
    val effectiveTint = if (iconTint != Color(0xFF2563EB)) iconTint else iconColor

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
                thumbColor = Color(0xFF2563EB),
                activeTrackColor = Color(0xFF2563EB),
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
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0)
    HorizontalDivider(
        modifier = modifier.padding(start = paddingStart, end = 16.dp),
        thickness = 0.75.dp,
        color = dividerColor
    )
}
