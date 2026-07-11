package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalChromaColors
import kotlin.math.roundToInt

/**
 * Direct port of the sat/val square + hue/alpha sliders + hex input from index.html:
 *   .sat-val-area (140px tall, crosshair drag) / .hue-track / .alpha-track-bg (checkerboard)
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val colors = LocalChromaColors.current
    val hsv = remember { FloatArray(3) }
    android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
    var hue by remember { mutableStateOf(hsv[0]) }           // 0..360
    var sat by remember { mutableStateOf(hsv[1]) }           // 0..1
    var value by remember { mutableStateOf(hsv[2]) }         // 0..1
    var alpha by remember { mutableStateOf(initialColor.alpha) }
    var hexText by remember { mutableStateOf(colorToHex(initialColor)) }

    fun currentColor(): Color {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        return Color(rgb).copy(alpha = alpha)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        title = { Text("Edit Color", color = colors.textMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // ---- Saturation/Value square (.sat-val-area) ----
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                ) {
                    val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                    hexText = colorToHex(currentColor())
                                }
                            }
                            .pointerInput(hue) {
                                detectTapGestures { tap ->
                                    sat = (tap.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - tap.y / size.height).coerceIn(0f, 1f)
                                    hexText = colorToHex(currentColor())
                                }
                            }
                    ) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, pureHueColor)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    }
                    // .sv-thumb — positioned from live drag state
                    Box(
                        modifier = Modifier
                            .offset(x = maxWidth * sat - 8.dp, y = (maxHeight * (1f - value)) - 8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(2.5.dp, Color.White, CircleShape)
                    )
                }

                Spacer(Modifier.padding(top = 14.dp))
                FieldLabelRow("Hue")
                Slider(
                    value = hue,
                    onValueChange = { hue = it; hexText = colorToHex(currentColor()) },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = pureHueSliderColor(hue), activeTrackColor = colors.primary)
                )

                Spacer(Modifier.padding(top = 6.dp))
                FieldLabelRow("Alpha", "${(alpha * 100).roundToInt()}%")
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it; hexText = colorToHex(currentColor()) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )

                Spacer(Modifier.padding(top = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentColor())
                            .border(1.dp, colors.glassBorder, CircleShape)
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { text ->
                            hexText = text
                            hexToColor(text)?.let { c ->
                                android.graphics.Color.colorToHSV(c.toArgb(), hsv)
                                hue = hsv[0]; sat = hsv[1]; value = hsv[2]; alpha = c.alpha
                            }
                        },
                        singleLine = true,
                        label = { Text("HEX", fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.glassBorder,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        )
                    )
                }
            }
        },
        confirmButton = {
            Text(
                "Apply",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(12.dp)
                    .pointerInput(Unit) { detectTapGestures { onConfirm(currentColor()) } }
            )
        },
        dismissButton = {
            Text(
                "Cancel",
                color = colors.textMuted,
                modifier = Modifier
                    .padding(12.dp)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            )
        }
    )
}

@Composable
private fun FieldLabelRow(label: String, value: String? = null) {
    val colors = LocalChromaColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label.uppercase(), color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        if (value != null) Text(value, color = colors.textMain, fontSize = 10.sp)
    }
}

@Composable
private fun pureHueSliderColor(hue: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

/** #RRGGBB — matches the .flat-input hex field (alpha is edited via its own slider). */
private fun colorToHex(c: Color): String {
    val argb = c.toArgb()
    return String.format("#%06X", argb and 0x00FFFFFF)
}

private fun hexToColor(hex: String): Color? {
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return try {
        val argb = if (cleaned.length == 6) "FF$cleaned" else cleaned
        Color(android.graphics.Color.parseColor("#$argb"))
    } catch (e: IllegalArgumentException) {
        null
    }
}
