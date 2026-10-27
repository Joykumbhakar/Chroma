package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.chroma.studio.ui.graphics3d.Apple3DGraphicId
import com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon
import com.chroma.studio.ui.graphics3d.AppleSettingsSquircleIcon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic macOS 3D Aluminum Squircle System Settings Gear Icon
 */
@Composable
fun MacOSSettingsGearIcon(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    val isDark = isSystemInDarkTheme()
    val squircleCorner = size * 0.2237f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(squircleCorner),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.20f),
                ambientColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(squircleCorner))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD0D4DB),
                        Color(0xFFA2A8B2),
                        Color(0xFF757C87),
                        Color(0xFF555B65)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Black.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(squircleCorner)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Specular top highlight overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.1f),
                        radius = 120f
                    )
                )
        )

        // Recessed circular dark well
        val wellSize = size * 0.68f
        Box(
            modifier = Modifier
                .size(wellSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B4047),
                            Color(0xFF22262B)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Metallic 3D Gear Canvas
            val gearSize = wellSize * 0.88f
            Canvas(modifier = Modifier.size(gearSize)) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val outerR = this.size.width * 0.48f
                val rootR = this.size.width * 0.33f
                val innerHoleR = this.size.width * 0.16f
                val numTeeth = 6
                val stepAngle = 360f / numTeeth

                val gearPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    for (i in 0 until numTeeth) {
                        val midAngle = i * stepAngle
                        val radT1 = Math.toRadians((midAngle - 11.0))
                        val radT2 = Math.toRadians((midAngle + 11.0))
                        val radB1 = Math.toRadians((midAngle - 17.0))
                        val radB2 = Math.toRadians((midAngle + 17.0))

                        val b1x = cx + (rootR * cos(radB1)).toFloat()
                        val b1y = cy + (rootR * sin(radB1)).toFloat()
                        val t1x = cx + (outerR * cos(radT1)).toFloat()
                        val t1y = cy + (outerR * sin(radT1)).toFloat()
                        val t2x = cx + (outerR * cos(radT2)).toFloat()
                        val t2y = cy + (outerR * sin(radT2)).toFloat()
                        val b2x = cx + (rootR * cos(radB2)).toFloat()
                        val b2y = cy + (rootR * sin(radB2)).toFloat()

                        if (i == 0) {
                            moveTo(b1x, b1y)
                        } else {
                            lineTo(b1x, b1y)
                        }
                        lineTo(t1x, t1y)
                        lineTo(t2x, t2y)
                        lineTo(b2x, b2y)

                        // Next tooth root connection
                        val nextMid = (i + 1) * stepAngle
                        val nextB1Rad = Math.toRadians((nextMid - 17.0))
                        val nb1x = cx + (rootR * cos(nextB1Rad)).toFloat()
                        val nb1y = cy + (rootR * sin(nextB1Rad)).toFloat()
                        lineTo(nb1x, nb1y)
                    }
                    close()

                    // Center axle hole
                    addOval(
                        Rect(
                            left = cx - innerHoleR,
                            top = cy - innerHoleR,
                            right = cx + innerHoleR,
                            bottom = cy + innerHoleR
                        )
                    )
                }

                // Fill Metallic Gear Body
                drawPath(
                    path = gearPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8EAEF),
                            Color(0xFFB8BDC6),
                            Color(0xFF868D98),
                            Color(0xFF636A74)
                        ),
                        startY = 0f,
                        endY = this.size.height
                    )
                )

                // Gear highlight stroke
                drawPath(
                    path = gearPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.35f)
                        )
                    ),
                    style = Stroke(width = 0.5.dp.toPx())
                )

                // Central hub ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF707782),
                            Color(0xFF4A5059)
                        ),
                        center = Offset(cx, cy),
                        radius = innerHoleR * 1.5f
                    ),
                    radius = innerHoleR * 1.4f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }
        }
    }
}

/**
 * Apple Settings Screen Header with Status Bar Insets
 */
@Composable
fun AppleSettingsHeader(
    title: String = "Settings",
    icon: ImageVector? = null,
    useMacSettingsIcon: Boolean = true,
    iconGradient: List<Color> = listOf(Color(0xFF8E8E93), Color(0xFF636366)),
    doneText: String? = "Done",
    onDoneClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBackClick() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon ?: Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (useMacSettingsIcon) {
                MacOSSettingsGearIcon(size = 32.dp)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                color = colors.textMain,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        if (onDoneClick != null && doneText != null) {
            Text(
                text = doneText,
                color = Color(0xFF007AFF),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDoneClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        } else if (onBackClick != null) {
            Text(
                text = "Back",
                color = Color(0xFF007AFF),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Apple iOS / macOS Settings Search Field
 */
@Composable
fun AppleSettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = LocalOscellionColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE3E3E8))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Lucide.Search,
            contentDescription = null,
            tint = Color(0xFF8E8E93),
            modifier = Modifier.size(15.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF8E8E93),
                    fontSize = 14.5.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.textMain,
                    fontSize = 14.5.sp
                ),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = "Clear",
                tint = Color(0xFF8E8E93),
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

/**
 * Apple ID / Account Inspector Header Card (Apple Inset Grouped)
 */
@Composable
fun AppleSettingsProfileHeaderCard(
    displayName: String,
    handleOrEmail: String,
    avatarUrl: String? = null,
    gender: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val presetRes = remember(avatarUrl) { getAvatarPresetDrawableRes(avatarUrl) }
    val isPreset = remember(avatarUrl) { isPresetAvatar(avatarUrl) }
    val defaultRes = remember(gender) { getDefaultAvatarDrawableRes(gender) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            // Apple Style Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF007AFF), Color(0xFF0051D5))
                        )
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val pRes = presetRes
                val dRes = defaultRes
                if (pRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = pRes),
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!avatarUrl.isNullOrBlank() && !isPreset) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (dRes != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = dRes),
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = displayName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifBlank { "O" }

                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName.ifBlank { "Oscellion Creator" },
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = handleOrEmail.ifBlank { "Apple Account, iCloud & Media" },
                    color = Color(0xFF8E8E93),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Apple Inset Grouped Section Card
 */
@Composable
fun AppleSettingsGroup(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header.uppercase(),
                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6D6D72),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFDFE8F3),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            content()
        }

        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Universal Signature Programmatic Gradient for Icon Badges:
 * Single Unified Apple Blue Gradient matching Liquid Glass spec.
 */
val UniversalIconGradientColors = listOf(Color(0xFF4DA6FF), Color(0xFF0A84FF), Color(0xFF0051D5))
val UniversalIconBrush = Brush.verticalGradient(
    colors = UniversalIconGradientColors
)

/**
 * Settings List Option Icon
 * Clean thin vector icon without squircle background box (Matching Liquid Glass Design System Section 3)
 */
@Composable
fun AppleIconBadge(
    icon: ImageVector? = null,
    graphic3dId: Apple3DGraphicId? = null,
    iconColor: Color = Color(0xFF0A84FF),
    iconGradient: List<Color>? = null,
    size: Dp = 22.dp,
    hasBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (hasBackground) {
        val squircleShape = RoundedCornerShape(8.dp)
        if (icon != null) {
            Box(
                modifier = modifier
                    .size(30.dp)
                    .clip(squircleShape)
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (graphic3dId != null) {
            val brush = if (iconGradient != null) {
                Brush.linearGradient(
                    colors = iconGradient,
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            } else {
                UniversalIconBrush
            }

            Box(
                modifier = modifier
                    .size(30.dp)
                    .clip(squircleShape)
                    .background(brush)
                    .border(1.dp, Color.White.copy(alpha = 0.42f), squircleShape),
                contentAlignment = Alignment.Center
            ) {
                Apple3DGraphicIcon(
                    id = graphic3dId,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    } else {
        // Clean thin icon directly rendered with zero background box
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(size)
                )
            } else if (graphic3dId != null) {
                Apple3DGraphicIcon(
                    id = graphic3dId,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

/**
 * Navigation / Option Selection Row (with Apple standard secondary text and chevron)
 */
@Composable
fun AppleSettingNavRow(
    title: String,
    subtitle: String? = null,
    valueText: String? = null,
    valueTextColor: Color = Color(0xFF8E8E93),
    icon: ImageVector? = null,
    graphic3dId: Apple3DGraphicId? = null,
    iconColor: Color = Color(0xFF0A84FF),
    iconGradient: List<Color>? = null,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val titleColor = if (isDestructive) Color(0xFFFF3B30) else colors.textMain

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 9.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            if (graphic3dId != null || icon != null) {
                AppleIconBadge(
                    icon = icon,
                    graphic3dId = graphic3dId,
                    iconColor = if (isDestructive) Color(0xFFFF3B30) else iconColor,
                    iconGradient = iconGradient
                )
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!valueText.isNullOrBlank()) {
                Text(
                    text = valueText,
                    color = valueTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Toggle Row matching iOS Settings
 */
@Composable
fun AppleSettingToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    graphic3dId: Apple3DGraphicId? = null,
    iconColor: Color = Color(0xFF0A84FF),
    iconGradient: List<Color>? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 9.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            if (graphic3dId != null || icon != null) {
                AppleIconBadge(
                    icon = icon,
                    graphic3dId = graphic3dId,
                    iconColor = iconColor,
                    iconGradient = iconGradient
                )
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    color = colors.textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AppleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Slider Row matching Apple Settings
 */
@Composable
fun AppleSettingSliderRow(
    title: String,
    valueText: String,
    valueTextColor: Color = Color(0xFF0A84FF),
    icon: ImageVector? = null,
    graphic3dId: Apple3DGraphicId? = null,
    iconColor: Color = Color(0xFF0A84FF),
    iconGradient: List<Color>? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (graphic3dId != null || icon != null) {
                    AppleIconBadge(
                        icon = icon,
                        graphic3dId = graphic3dId,
                        iconColor = iconColor,
                        iconGradient = iconGradient
                    )
                }
                Text(
                    text = title,
                    color = colors.textMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Text(
                text = valueText,
                color = valueTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        AppleSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

/**
 * Segmented Control Row
 */
@Composable
fun AppleSettingSegmentedRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = title,
            color = colors.textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.6f))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(
                            elevation = if (isSelected) 2.dp else 0.dp,
                            shape = RoundedCornerShape(7.dp),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Color(0xFF3A3A3C) else Color.White
                            } else Color.Transparent
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) colors.textMain else Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Apple Setting Divider
 */
@Composable
fun AppleSettingDivider(paddingStart: Dp = 50.dp) {
    val isDark = isSystemInDarkTheme()
    HorizontalDivider(
        color = if (isDark) Color(0xFF38383A) else Color(0xFFDFE8F3),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = paddingStart)
    )
}
