package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.chroma.studio.ui.theme.LocalChromaColors
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.absoluteOffset
import com.chroma.studio.utils.ExportHelper
import com.chroma.studio.utils.ExportEngine
import com.chroma.studio.viewmodel.ChromaViewModel
import com.composables.icons.lucide.Copy

@Composable
fun DeveloperHandoffModal(vm: ChromaViewModel, workName: String, onClose: () -> Unit) {
    val colors = LocalChromaColors.current
    var selectedFramework by remember { mutableStateOf("CSS / SCSS") }
    val clipboardManager = LocalClipboardManager.current
    val safeWorkName = if (workName.isBlank()) "Untitled" else workName.replace(" ", "_")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val picture = remember { android.graphics.Picture() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClose() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Offscreen Canvas for Export (moved to root to avoid disrupting Column layout)
        Box(
            modifier = Modifier
                .absoluteOffset(x = (-10000).dp)
                .size(1080.dp, 1920.dp)
                .drawWithCache {
                    val width = this.size.width.toInt()
                    val height = this.size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas = androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
                        draw(this, this.layoutDirection, pictureCanvas, this.size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()
                        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
                    }
                }
        ) {
            CanvasPreview(
                layers = vm.layers,
                shape = vm.canvasShape,
                borderColor = Color.Transparent,
                textContent = vm.textPreviewContent,
                isStatic = true
            )
        }

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* consume tap */ })
                },
            cornerRadius = 16,
            backgroundColor = colors.glassBg.copy(alpha = 0.85f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                val allFormats = listOf("CSS / SCSS", "Tailwind", "React / Next.js", "Vue", "Svelte", "Angular", "Canvas JS", "SwiftUI", "Jetpack Compose", "XML Drawable", "Kotlin", "Java", "Flutter", "React Native", "JSON")
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    items(allFormats) { format ->
                        val isSelected = format == selectedFramework
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.glassBg else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = if (isSelected) {
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF60A5FA),
                                                Color(0xFF3B82F6),
                                                Color(0xFF2563EB)
                                            )
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                                                colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFramework = format }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = format,
                                color = if (isSelected) colors.primary else colors.textMuted,
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
                    ExportButton("PNG", Lucide.Image, onExport = { 
                        ToastManager.showToast("Generating PNG...")
                        kotlinx.coroutines.delay(500)
                        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            android.graphics.Bitmap.createBitmap(picture)
                        } else {
                            val b = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                            android.graphics.Canvas(b).drawPicture(picture)
                            b
                        }
                        val uri = ExportHelper.saveBitmap(context, bitmap, "ChromaStudio_$safeWorkName", true)
                        if (uri != null) {
                            ToastManager.showToast("Saved to Gallery", "Open", 4000L, onAction = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/png")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            })
                        }
                    })
                    ExportButton("JPG", Lucide.Image, onExport = { 
                        ToastManager.showToast("Generating JPG...")
                        kotlinx.coroutines.delay(500)
                        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            android.graphics.Bitmap.createBitmap(picture)
                        } else {
                            val b = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                            android.graphics.Canvas(b).drawPicture(picture)
                            b
                        }
                        val uri = ExportHelper.saveBitmap(context, bitmap, "ChromaStudio_$safeWorkName", false)
                        if (uri != null) {
                            ToastManager.showToast("Saved to Gallery", "Open", 4000L, onAction = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/jpeg")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            })
                        }
                    })
                    ExportButton("SVG", Lucide.Code, onExport = { 
                        ToastManager.showToast("Generating SVG...")
                        kotlinx.coroutines.delay(500)
                        val svgCode = ExportEngine.generateCode(
                            type = "SVG",
                            layersRaw = vm.layers,
                            isDarkTheme = false,
                            hasAnim = vm.globalAnimStatus != com.chroma.studio.model.AnimStatus.STOPPED,
                            aStyle = vm.globalAnimStyle,
                            aSpeed = vm.globalAnimSpeed,
                            intensity = vm.globalAnimAmount.toInt(),
                            shape = vm.canvasShape,
                            textContent = vm.textPreviewContent
                        )
                        val uri = ExportHelper.saveText(context, svgCode, "ChromaStudio_$safeWorkName", ".svg")
                        if (uri != null) {
                            ToastManager.showToast("Saved to Downloads", "Open", 4000L, onAction = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/svg+xml")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            })
                        }
                    })
                }

                // ZIP Export Button
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.glassBg)
                            .border(
                                width = 2.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6), // Purple
                                        Color(0xFFD946EF), // Fuchsia
                                        Color(0xFFF43F5E)  // Rose
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    ToastManager.showToast("Generating ZIP Archive...")
                                    kotlinx.coroutines.delay(500)
                                    
                                    try {
                                        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                            android.graphics.Bitmap.createBitmap(picture)
                                        } else {
                                            val b = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                                            android.graphics.Canvas(b).drawPicture(picture)
                                            b
                                        }
                                        
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            val assets = mutableListOf<ExportHelper.ZipAsset>()
                                            
                                            val pngStream = java.io.ByteArrayOutputStream()
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, pngStream)
                                            assets.add(ExportHelper.ZipAsset("ChromaStudio_$safeWorkName.png", pngStream.toByteArray()))
                                            
                                            val jpgStream = java.io.ByteArrayOutputStream()
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, jpgStream)
                                            assets.add(ExportHelper.ZipAsset("ChromaStudio_$safeWorkName.jpg", jpgStream.toByteArray()))
                                            
                                            val formats = listOf("SVG", "CSS / SCSS", "Tailwind", "React / Next.js", "SwiftUI")
                                            val extensions = listOf(".svg", ".css", ".tailwind.js", ".jsx", ".swift")
                                            
                                            for (i in formats.indices) {
                                                val code = ExportEngine.generateCode(
                                                    type = formats[i],
                                                    layersRaw = vm.layers,
                                                    isDarkTheme = false,
                                                    hasAnim = vm.globalAnimStatus != com.chroma.studio.model.AnimStatus.STOPPED,
                                                    aStyle = vm.globalAnimStyle,
                                                    aSpeed = vm.globalAnimSpeed,
                                                    intensity = vm.globalAnimAmount.toInt(),
                                                    shape = vm.canvasShape,
                                                    textContent = vm.textPreviewContent
                                                )
                                                assets.add(ExportHelper.ZipAsset("ChromaStudio_${safeWorkName}${extensions[i]}", code.toByteArray(Charsets.UTF_8)))
                                            }
                                            
                                            val uri = ExportHelper.saveZip(context, "ChromaStudio_$safeWorkName", assets)
                                            
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                if (uri != null) {
                                                    ToastManager.showToast("Saved ZIP to Downloads", "Open", 4000L, onAction = {
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, "application/zip")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(intent)
                                                    })
                                                } else {
                                                    ToastManager.showToast("Failed to save ZIP")
                                                }
                                            }
                                        }
                                    } catch(e: Exception) {
                                        ToastManager.showToast("Error: ${e.message}")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Copy, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download All (.zip)", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Code Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val generatedCode = remember(selectedFramework, vm.layers, vm.globalAnimStatus, vm.globalAnimStyle, vm.globalAnimSpeed, vm.globalAnimAmount, vm.canvasShape, vm.textPreviewContent) {
                        if (selectedFramework == "JSON") {
                            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(vm.layers)
                        } else {
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
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassBg)
                            .glossyBorder(RoundedCornerShape(8.dp), colors)
                            .padding(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = generatedCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = colors.textMain,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    // Copy Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(generatedCode))
                                ToastManager.showToast("Code Copied")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Copy, contentDescription = "Copy Code", tint = colors.textMain, modifier = Modifier.size(14.dp))
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
}

@Composable
private fun ExportButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onExport: suspend () -> Unit) {
    val colors = LocalChromaColors.current
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glassBg)
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .clickable {
                if (!isSaving) {
                    isSaving = true
                    coroutineScope.launch {
                        onExport()
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

