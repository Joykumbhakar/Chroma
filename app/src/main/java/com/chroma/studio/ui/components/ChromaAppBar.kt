package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalChromaColors

/**
 * .app-bar { height: 64px; display:flex; align-items:center; justify-content:space-between;
 *            padding: 0 24px; border-bottom-left/right-radius: var(--radius-md); }
 */
@Composable
fun ChromaAppBar(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onAddLayer: () -> Unit,
    onRandomize: () -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current

    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        cornerRadius = 0
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary)
                )
                Text(
                    text = "Chroma Studio",
                    color = colors.textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconGhostButton(icon = "↶", contentDesc = "Undo", disabled = !canUndo, onClick = onUndo)
                IconGhostButton(icon = "↷", contentDesc = "Redo", disabled = !canRedo, onClick = onRedo)
                IconGhostButton(icon = "🔀", contentDesc = "Randomize All", onClick = onRandomize)
                IconGhostButton(icon = "＋", contentDesc = "Add Layer", filled = true, onClick = onAddLayer)

                // .switch-container dark mode toggle, matching the pill/thumb from the CSS
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDarkMode) colors.primary else colors.glassBgHover)
                        .clickable(indication = null, interactionSource = MutableInteractionSource()) { onToggleDarkMode() }
                        .padding(4.dp),
                    contentAlignment = if (isDarkMode) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.onPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = isDarkMode, label = "themeIcon") { dark ->
                            Icon(
                                imageVector = if (dark) Icons.Filled.Nightlight else Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGhostButton(icon: String, contentDesc: String, filled: Boolean = false, disabled: Boolean = false, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (filled) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(indication = null, interactionSource = MutableInteractionSource(), enabled = !disabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 15.sp,
            color = when {
                disabled -> colors.textMuted.copy(alpha = 0.4f)
                filled -> colors.onPrimary
                else -> colors.textMain
            }
        )
    }
}
