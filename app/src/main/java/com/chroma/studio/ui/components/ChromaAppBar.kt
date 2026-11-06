package com.chroma.studio.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Clean, borderless Apple / One UI style Chroma Studio Top Bar:
 * - 36dp circular frosted tactile glass touch targets (no borders!)
 * - High-contrast brand lockup
 * - Tactile spring scale animations & subtle hairline divider
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
    onExport: () -> Unit = {},
    onSave: () -> Unit = {},
    isRemix: Boolean = false,
    onPublish: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .drawBehind {
                val stroke = 0.5.dp.toPx()
                drawLine(
                    color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 14.dp)
    ) {
        // ---- LEFT CONTROLS ----
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconGhostButton(icon = Lucide.ChevronLeft, contentDesc = "Back", onClick = onBack)
            IconGhostButton(icon = Lucide.Save, contentDesc = "Save", onClick = onSave)
            
            // Show Publish button only when editing a remixed work
            if (isRemix) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(colors.primary, Color(0xFFEC4899))
                            )
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onPublish() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Globe,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Publish Remix",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ---- CENTER TITLE ----
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OscellionBrandLockup(
                iconSize = 20.dp,
                fontSize = 17.dp,
                textColor = colors.textMain
            )
        }

        // ---- RIGHT CONTROLS ----
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconGhostButton(icon = Lucide.Undo, contentDesc = "Undo", disabled = !canUndo, onClick = onUndo)
            IconGhostButton(icon = Lucide.Redo, contentDesc = "Redo", disabled = !canRedo, onClick = onRedo)

            // More Menu
            var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
            Box {
                IconGhostButton(
                    icon = Lucide.Menu,
                    contentDesc = "More options",
                    onClick = { menuExpanded = true }
                )
                
                androidx.compose.material3.DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.bg)
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Developer Handoff", color = colors.textMain) },
                        leadingIcon = { Icon(Lucide.Code, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                        onClick = { 
                            menuExpanded = false
                            onExport() 
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode", color = colors.textMain) },
                        leadingIcon = { 
                            Icon(
                                if (isDarkMode) Lucide.Sun else Lucide.Moon, 
                                contentDescription = null, 
                                tint = colors.textMain, 
                                modifier = Modifier.size(16.dp)
                            ) 
                        },
                        onClick = { 
                            menuExpanded = false
                            onToggleDarkMode() 
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconGhostButton(
    icon: ImageVector,
    contentDesc: String,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !disabled) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconBtnScale"
    )

    val buttonBg = if (disabled) {
        if (colors.isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)
    } else if (isPressed) {
        if (colors.isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
    } else {
        if (colors.isDark) Color(0xFF1E222A) else Color(0xFFF1F4F8)
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(36.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                enabled = !disabled
            ) {
                com.chroma.studio.chat.sound.SoundManager.playReaction()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (disabled) colors.textMuted.copy(alpha = 0.35f) else colors.textMain
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(17.dp),
            tint = tint
        )
    }
}
