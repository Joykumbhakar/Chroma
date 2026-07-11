package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.utils.ExportEngine
import com.chroma.studio.viewmodel.ChromaViewModel

@Composable
fun DeveloperHandoffModal(vm: ChromaViewModel, onClose: () -> Unit) {
    val colors = LocalChromaColors.current
    var selectedFramework by remember { mutableStateOf("CSS / SCSS") }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bg)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Code, null, tint = colors.textMain, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "DEVELOPER HANDOFF",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    Lucide.X,
                    contentDescription = "Close",
                    tint = colors.textMain,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() }
                )
            }

            Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))

            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Horizontal Scrollable Formats
                val allFormats = listOf("CSS / SCSS", "Tailwind", "React / Next.js", "Vue", "Svelte", "Angular", "Canvas JS", "SwiftUI", "Jetpack Compose", "XML Drawable", "Kotlin", "Java", "Flutter", "React Native")
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    items(allFormats) { format ->
                        val isSelected = format == selectedFramework
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primary else Color.Transparent)
                                .clickable { selectedFramework = format }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format,
                                color = if (isSelected) colors.onPrimary else colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportButton("PNG", Lucide.Image, onExport = { /* Add export logic here */ })
                    ExportButton("JPG", Lucide.Image, onExport = { /* Add export logic here */ })
                    ExportButton("SVG", Lucide.Code, onExport = { /* Add export logic here */ })
                }

                // Record Button
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.primary)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Video, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Record .webm (5s)", color = colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Code Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F3F5)) // light gray
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    val generatedCode = remember(selectedFramework, vm.layers, vm.globalAnimStatus, vm.globalAnimStyle, vm.globalAnimSpeed, vm.globalAnimAmount, vm.canvasShape, vm.textPreviewContent) {
                        ExportEngine.generateCode(
                            type = selectedFramework,
                            layersRaw = vm.layers,
                            isDarkTheme = false, // You could pull this from theme later
                            hasAnim = vm.globalAnimStatus != com.chroma.studio.model.AnimStatus.STOPPED,
                            aStyle = vm.globalAnimStyle,
                            aSpeed = vm.globalAnimSpeed,
                            intensity = vm.globalAnimAmount.toInt(),
                            shape = vm.canvasShape,
                            textContent = vm.textPreviewContent
                        )
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = generatedCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF374151),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Footer Note
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Info, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Note: For dynamic engines like Aurora or complex Blobs, standard CSS output will approximate with static gradients. Use Canvas JS for 1:1 Aurora rendering.",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onExport: () -> Unit) {
    val colors = LocalChromaColors.current
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .clickable {
                if (!isSaving) {
                    isSaving = true
                    coroutineScope.launch {
                        onExport()
                        kotlinx.coroutines.delay(1500) // Simulate saving delay
                        isSaving = false
                    }
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSaving) {
            IOSLoader(color = colors.textMain, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Saving...", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(icon, null, tint = colors.textMain, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IOSLoader(modifier: Modifier = Modifier, color: Color = Color.Gray) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ios_loader")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "ios_loader_angle"
    )
    val step = (angle / 30f).toInt()
    androidx.compose.foundation.Canvas(modifier = modifier.rotate(step * 30f)) {
        for (i in 0 until 12) {
            rotate(i * 30f) {
                drawLine(
                    color = color.copy(alpha = 1f - (i / 12f)),
                    start = androidx.compose.ui.geometry.Offset(size.width/2, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width/2, 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

