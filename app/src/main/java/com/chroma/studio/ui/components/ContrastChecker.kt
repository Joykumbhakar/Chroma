package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.WandSparkles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalChromaColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * #contrast-checker — WCAG contrast ratio badge (AA/AAA) with an auto-fix wand
 * (#btn-fix-contrast) that nudges the top layer's first stop toward white/black
 * until the AA threshold (4.5:1) is met, mirroring autoFixContrast() in index.html.
 */
private fun relativeLuminance(c: Color): Double {
    fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relativeLuminance(a) + 0.05
    val l2 = relativeLuminance(b) + 0.05
    return max(l1, l2) / min(l1, l2)
}

@Composable
fun ContrastCheckerBadge(
    layers: List<GradientLayer>,
    onAutoFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current
    val top = layers.lastOrNull()?.stops?.firstOrNull()?.color ?: Color.White
    val bottomLayer = layers.firstOrNull()?.stops?.lastOrNull()?.color ?: Color.Black
    val ratio = remember(top, bottomLayer) { contrastRatio(top, bottomLayer) }
    val passesAA = ratio >= 4.5
    val passesAAA = ratio >= 7.0

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.glassBgHover, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("RATIO", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(
            " ${String.format("%.1f", ratio)}:1",
            color = colors.textMain,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = if (passesAAA) "AAA" else if (passesAA) "AA" else "FAIL",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (passesAA) colors.success else colors.error, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        if (!passesAA) {
            Icon(
                imageVector = Lucide.WandSparkles,
                contentDescription = "Auto-fix contrast",
                tint = colors.primary,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onAutoFix() }
            )
        }
    }
}
