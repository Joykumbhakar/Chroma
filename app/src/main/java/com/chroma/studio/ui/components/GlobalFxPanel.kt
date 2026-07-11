package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ApplePresets
import com.chroma.studio.model.ColorBlindMode
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

/**
 * Tab 2 of the mobile drawer / desktop right column ("Global FX"):
 *  - animation speed/intensity sliders for the active layer (#anim-speed / #anim-intensity)
 *  - Apple-style presets grid (#preset-grid)
 *  - color-blind simulator select + contrast checker toggle (surfaced here for mobile,
 *    desktop keeps them anchored to the canvas like the source file)
 */
@Composable
fun GlobalFxPanel(vm: ChromaViewModel, modifier: Modifier = Modifier) {
    val colors = LocalChromaColors.current
    val activeLayer = vm.layers.find { it.id == vm.activeLayerId } ?: vm.layers.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("GLOBAL FX", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

        if (activeLayer != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Animate Layer", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = activeLayer.animated,
                    onCheckedChange = { vm.setAnimated(activeLayer.id, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
                )
            }

            SliderField("Speed", activeLayer.animSpeed, 1f..100f) { vm.setAnimSpeed(activeLayer.id, it) }
            SliderField("Amount", activeLayer.animIntensity, 0f..100f) { vm.setAnimIntensity(activeLayer.id, it) }
        }

        Spacer(
            Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder)
        )

        Text("Apple Style Presets", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ApplePresets) { preset ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(preset.colors))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                        .clickable(indication = null, interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }) {
                            vm.applyPreset(preset.colors, preset.type)
                        }
                )
            }
        }

        Spacer(
            Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder)
        )

        Text("Accessibility", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorBlindMode.entries.forEach { mode ->
                val active = vm.colorBlindMode == mode
                Text(
                    mode.label,
                    color = if (active) colors.onPrimary else colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) colors.primary else colors.glassBg, RoundedCornerShape(8.dp))
                        .clickable(indication = null, interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }) {
                            vm.updateColorBlindMode(mode)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Contrast Checker", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = vm.contrastCheckerEnabled,
                onCheckedChange = { vm.toggleContrastChecker() },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
            )
        }

        Spacer(
            Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder)
        )

        Text("Post FX", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Halftone", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = vm.halftoneEnabled,
                onCheckedChange = { vm.toggleHalftone() },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dither", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = vm.ditherEnabled,
                onCheckedChange = { vm.toggleDither() },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
            )
        }
    }
}

@Composable
private fun SliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val colors = LocalChromaColors.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value.toInt().toString(), color = colors.textMain, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
        )
    }
}
