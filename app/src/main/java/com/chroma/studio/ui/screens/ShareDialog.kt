@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.ChromaPillVariant
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.LiquidGlassProgressBar
import com.chroma.studio.ui.components.LiquidGlassBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.ui.graphics3d.Apple3DGraphicId
import com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.hazeChild

@Suppress("EnumEntryName")
enum class ShareStatus { IDLE, PUBLISHING, SUCCESS, ERROR }

@Composable
fun ShareToDiscoverDialog(
    initialName: String,
    initialDescription: String,
    status: ShareStatus,
    isVerified: Boolean,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDescription) }
    var aspectRatio by remember { mutableStateOf("4:3") }

    val hazeState = com.chroma.studio.ui.components.LocalHazeState.current

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    LiquidGlassBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Apple3DGraphicIcon(
                    id = Apple3DGraphicId.NETWORK_GLOBE,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Share to Discover", color = colors.textMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Publish your work for the community to see.",
                    color = colors.textMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                when (status) {
                    ShareStatus.IDLE, ShareStatus.ERROR -> {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 50) name = it },
                            label = { Text("Title / Caption") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = name.isBlank(),
                            supportingText = {
                                Text(
                                    text = "${name.length}/50",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { if (it.length <= 300) desc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            isError = desc.isBlank(),
                            supportingText = {
                                Text(
                                    text = "${desc.length}/300",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Aspect Ratio", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val options = mutableListOf("1:1", "4:3", "5:4")
                            if (isVerified) options.add("16:9")
                            
                            options.forEach { ratio ->
                                val selected = aspectRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) colors.primary else colors.glassBg)
                                        .border(1.dp, if (selected) Color.Transparent else colors.textMuted.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                                        .clickable { aspectRatio = ratio }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ratio, color = if (selected) Color.White else colors.textMain, fontSize = 12.sp)
                                }
                            }
                        }
                        if (status == ShareStatus.ERROR) {
                            Spacer(Modifier.height(8.dp))
                            Text("Failed to publish. Please try again.", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                    ShareStatus.PUBLISHING -> {
                        LiquidGlassProgressBar(
                            modifier = Modifier.fillMaxWidth(),
                            height = 6.dp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Publishing...", color = colors.textMain, fontSize = 14.sp)
                    }
                    ShareStatus.SUCCESS -> {
                        Icon(
                            imageVector = Lucide.CircleCheck,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Published successfully!", color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status != ShareStatus.PUBLISHING) {
                        if (status == ShareStatus.SUCCESS) {
                            LiquidGlassPrimaryButton(
                                onClick = onDismiss,
                                isSmall = true
                            ) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        } else {
                            LiquidGlassSecondaryButton(
                                onClick = onDismiss,
                                isSmall = true
                            ) {
                                Text("Cancel", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(10.dp))
                            LiquidGlassPrimaryButton(
                                onClick = { onConfirm(name, desc, aspectRatio) },
                                enabled = name.isNotBlank() && desc.isNotBlank(),
                                isSmall = true
                            ) {
                                Icon(Lucide.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Publish", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
}
