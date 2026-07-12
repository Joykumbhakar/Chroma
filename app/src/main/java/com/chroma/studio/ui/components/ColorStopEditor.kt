package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.WandSparkles
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ColorStop
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.theme.LocalChromaColors
import kotlin.math.roundToInt

@Composable
fun ColorStopEditor(
    stops: List<ColorStop>,
    onStopsChange: (List<ColorStop>) -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(8.dp)

    // derivedStateOf: only re-sorts when stops actually change identity
    val sorted by remember(stops) { derivedStateOf { stops.sortedBy { it.position } } }

    var editingStopId by remember { mutableStateOf(sorted.firstOrNull()?.id) }

    // Auto-select first stop if current is deleted or null
    if (editingStopId == null && sorted.isNotEmpty()) {
        editingStopId = sorted.first().id
    } else if (editingStopId != null && sorted.none { it.id == editingStopId }) {
        editingStopId = sorted.firstOrNull()?.id
    }

    val currentOnStopsChange by androidx.compose.runtime.rememberUpdatedState(onStopsChange)
    val currentStops by androidx.compose.runtime.rememberUpdatedState(stops)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COLOR STOPS", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            if (trailingContent != null) trailingContent()
        }
        Spacer(Modifier.padding(top = 8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(shape)
                .background(checkerboardBrush())
                .border(1.dp, colors.glassBorder, shape)
        ) {
            val trackWidthDp = maxWidth

            // Gradient preview strip — key on sorted identities so it only recomposes when stop list changes
            val gradientBrush by remember(sorted) {
                derivedStateOf {
                    Brush.linearGradient(
                        colorStops = sorted.map { (it.position / 100f) to it.color }.toTypedArray()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(gradientBrush)
                    // tap empty space to insert a new stop at that position
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val pos = (tap.x / size.width * 100f).coerceIn(0f, 100f)
                            val nearestColor = currentStops
                                .minByOrNull { kotlin.math.abs(it.position - pos) }?.color ?: Color.White
                            val newStop = ColorStop(color = nearestColor, position = pos)
                            currentOnStopsChange(currentStops + newStop)
                            editingStopId = newStop.id
                        }
                    }
            )

            // Stop thumbs — each keyed on stop.id for stable identity
            sorted.forEach { stop ->
                val isSelected = stop.id == editingStopId

                // graphicsLayer translation: skips layout & draw passes, only triggers transform
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 42.dp)
                        .graphicsLayer {
                            val pct = stop.position / 100f
                            translationX = (trackWidthDp.toPx() * pct) - 7.dp.toPx()
                            translationY = -6.dp.toPx()
                        }
                ) {
                    // Vertical pin line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer { translationY = 14.dp.toPx() }
                            .size(width = 2.dp, height = 28.dp)
                            .background(Color.Black)
                    )
                    // Thumb circle — drag + tap combined in one pointerInput block
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(14.dp)
                            .pointerInput(stop.id, trackWidthDp) {
                                val trackPx = trackWidthDp.toPx()
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaPct = (dragAmount.x / trackPx) * 100f
                                    val newPos = (stop.position + deltaPct).coerceIn(0f, 100f)
                                    currentOnStopsChange(
                                        currentStops.map { if (it.id == stop.id) it.copy(position = newPos) else it }
                                    )
                                }
                            }
                            .pointerInput(stop.id) {
                                detectTapGestures(
                                    onTap = { editingStopId = stop.id },
                                    onLongPress = {
                                        if (currentStops.size > 2) {
                                            currentOnStopsChange(currentStops.filterNot { it.id == stop.id })
                                            if (editingStopId == stop.id) {
                                                editingStopId = currentStops.firstOrNull { it.id != stop.id }?.id
                                            }
                                        }
                                    }
                                )
                            }
                            .clip(CircleShape)
                            .background(stop.color)
                            .border(
                                2.dp,
                                if (isSelected) colors.primary else Color.White,
                                CircleShape
                            )
                    )
                }
            }
        }

        Spacer(Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TAP TRACK TO ADD", color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("LONG PRESS TO REMOVE", color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        val editing = sorted.find { it.id == editingStopId }
        if (editing != null) {
            Spacer(Modifier.padding(top = 12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(editing.color)
                            .border(1.5.dp, colors.primary, CircleShape)
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("SELECTED STOP", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "POS ${editing.position.toInt()}%",
                    color = colors.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            ChromaSlider(
                value = editing.position,
                onValueChange = { newPos ->
                    currentOnStopsChange(currentStops.map { if (it.id == editing.id) it.copy(position = newPos) else it })
                },
                valueRange = 0f..100f
            )

            Spacer(Modifier.padding(top = 12.dp))
            Text("COLOR", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(top = 8.dp))
            InlineColorPicker(
                initialColor = editing.color,
                onColorChange = { newColor ->
                    currentOnStopsChange(currentStops.map { if (it.id == editing.id) it.copy(color = newColor) else it })
                }
            )
        }
    }
}

@Composable
fun InlineColorPicker(
    initialColor: Color,
    onColorChange: (Color) -> Unit
) {
    val colors = LocalChromaColors.current
    val hsv = remember { FloatArray(3) }

    // mutableFloatStateOf: specialized primitive state, avoids boxing overhead on every drag
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var hexText by remember { mutableStateOf("") }

    val currentOnColorChange by androidx.compose.runtime.rememberUpdatedState(onColorChange)

    // Sync from external color ONLY when it changes (e.g., switching stops or undo)
    androidx.compose.runtime.LaunchedEffect(initialColor) {
        val currentLocalColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))).copy(alpha = alpha)
        if (currentLocalColor != initialColor) {
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            if (hsv[1] > 0.001f && hsv[2] > 0.001f || hexText.isEmpty()) {
                hue = hsv[0]
            }
            sat = hsv[1]
            value = hsv[2]
            alpha = initialColor.alpha
            hexText = colorToHex(initialColor)
        }
    }

    fun currentColor(): Color {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        return Color(rgb).copy(alpha = alpha)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .background(colors.glassBg)
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // SV Picker area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val pureHueColor by remember(hue) {
                    derivedStateOf { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))) }
                }

                // Single combined pointerInput for tap + drag to reduce gesture recognizer overhead
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                val nc = currentColor()
                                hexText = colorToHex(nc)
                                currentOnColorChange(nc)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { tap ->
                                sat = (tap.x / size.width).coerceIn(0f, 1f)
                                value = (1f - tap.y / size.height).coerceIn(0f, 1f)
                                val nc = currentColor()
                                hexText = colorToHex(nc)
                                currentOnColorChange(nc)
                            }
                        }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, pureHueColor)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                }

                // Thumb — graphicsLayer avoids layout pass on sat/value changes
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = maxWidth.toPx() * sat - 10.dp.toPx()
                            translationY = (140.dp.toPx() * (1f - value)) - 10.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))))
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Hue Slider
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            hue = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            hue = ((tap.x / size.width) * 360f).coerceIn(0f, 360f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
            ) {
                val hueColors = remember {
                    listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                }
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center).clip(RoundedCornerShape(6.dp))) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(hueColors))
                    }
                }
                // Thumb via graphicsLayer — no layout recomposition on hue drag
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = maxWidth.toPx() * (hue / 360f) - 10.dp.toPx()
                            translationY = 2.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Alpha Slider
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            alpha = (change.position.x / size.width).coerceIn(0f, 1f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            alpha = (tap.x / size.width).coerceIn(0f, 1f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
            ) {
                val baseColor by remember(hue, sat, value) {
                    derivedStateOf { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))) }
                }
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center).clip(RoundedCornerShape(6.dp)).background(checkerboardBrush())) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, baseColor)))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = maxWidth.toPx() * alpha - 10.dp.toPx()
                            translationY = 2.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color.White)
                        .drawWithContent {
                            drawCircle(checkerboardBrush())
                            drawCircle(currentColor())
                        }
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Hex + Alpha inputs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        hexToColor(text)?.let { c ->
                            android.graphics.Color.colorToHSV(c.toArgb(), hsv)
                            hue = hsv[0]; sat = hsv[1]; value = hsv[2]; alpha = c.alpha
                            currentOnColorChange(c)
                        }
                    },
                    modifier = Modifier.weight(2f).height(44.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = colors.textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.glassBorder,
                        focusedBorderColor = colors.primary,
                        unfocusedContainerColor = colors.glassBg,
                        focusedContainerColor = colors.glassBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = "${(alpha * 100).roundToInt()} %",
                    onValueChange = { /* read-only display */ },
                    modifier = Modifier.weight(1f).height(44.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = colors.textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.glassBorder,
                        focusedBorderColor = colors.primary,
                        unfocusedContainerColor = colors.glassBg,
                        focusedContainerColor = colors.glassBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))
            // Auto-Harmony Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .glossyBorder(RoundedCornerShape(8.dp), colors)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        hue = (hue + 30f) % 360f
                        val nc = currentColor()
                        hexText = colorToHex(nc)
                        currentOnColorChange(nc)
                    }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.WandSparkles, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(14.dp))
                Spacer(Modifier.padding(start = 6.dp))
                Text("Auto-Harmony", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun checkerboardBrush(): Brush {
    return Brush.linearGradient(
        listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
    )
}

fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return if (a == 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

fun hexToColor(hex: String): Color? {
    if (hex.isBlank()) return null
    var cleanHex = hex.trim().removePrefix("#")
    if (cleanHex.length == 3) {
        cleanHex = cleanHex.map { "$it$it" }.joinToString("")
    }
    return try {
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> {
                val a = cleanHex.substring(0, 2).toInt(16) / 255f
                val rgb = cleanHex.substring(2)
                Color(android.graphics.Color.parseColor("#$rgb")).copy(alpha = a)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
