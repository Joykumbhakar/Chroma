package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalChromaColors
import com.composables.icons.lucide.*

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
    onExport: () -> Unit = {},
    onSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current

    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        cornerRadius = 0
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // ---- LEFT CONTROLS ----
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconGhostButton(icon = Lucide.Save, contentDesc = "Save", onClick = onSave)
            }

            // ---- CENTER TITLE ----
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Chroma",
                    color = colors.textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Studio",
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp
                )
            }

            // ---- RIGHT CONTROLS ----
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.End
            ) {
                IconGhostButton(icon = Lucide.Undo, contentDesc = "Undo", disabled = !canUndo, onClick = onUndo)
                IconGhostButton(icon = Lucide.Redo, contentDesc = "Redo", disabled = !canRedo, onClick = onRedo)
                
                // Divider
                Box(modifier = Modifier.padding(horizontal = 4.dp).width(1.dp).height(20.dp).background(colors.glassBorder))

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
                        modifier = Modifier.background(colors.bg)
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
}

@Composable
private fun IconGhostButton(
    icon: ImageVector,
    contentDesc: String,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalChromaColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .glossyBorder(CircleShape, colors)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = !disabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (disabled) colors.textMuted.copy(alpha = 0.4f) else colors.textMain
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
    }
}
