package com.chroma.studio.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors

/**
 * Official Oscellion Brand Vector Logo
 * Exact mathematical recreation from the official Brand Identity Dashboard:
 * - Geometry: M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z
 * - Default: 3D Milled Metallic finish with specular highlights & drop shadows (Variation 2)
 * - Custom Color: Flat vector silhouette (Variations 5 & 6)
 */
@Composable
fun OscellionLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    outerColor: Color? = null,
    innerColor: Color = Color(0xFF3B82F6),
    strokeWidth: Dp? = null
) {
    if (outerColor == null) {
        OscellionWebLogo(modifier = modifier, size = size)
    } else {
        OscellionMonochromeLogo(modifier = modifier, size = size, color = outerColor)
    }
}

/**
 * Official Horizontal Lockup: 3D Logo + "Oscellion." with vibrant blue dot
 */
@Composable
fun OscellionBrandLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    fontSize: Dp = 20.dp
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OscellionWebLogo(
            modifier = Modifier.size(iconSize),
            size = iconSize
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oscellion",
                fontSize = fontSize.value.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = ".",
                fontSize = fontSize.value.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color(0xFF007AFF)
            )
        }
    }
}

/**
 * Official Stacked Lockup: Large 3D Logo over "Oscellion."
 */
@Composable
fun OscellionStackedLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OscellionWebLogo(
            modifier = Modifier.size(iconSize),
            size = iconSize
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Oscellion",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = ".",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color(0xFF007AFF)
            )
        }
    }
}
