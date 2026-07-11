package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.AnimStatus
import com.chroma.studio.model.AnimStyle
import com.chroma.studio.model.ApplePresets
import com.chroma.studio.model.ColorBlindMode
import com.chroma.studio.model.PostProcessingFx
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

@Composable
fun GlobalFxPanel(vm: ChromaViewModel, modifier: Modifier = Modifier) {
    val colors = LocalChromaColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // AI PROMPT
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI PROMPT-TO-PALETTE", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.promptText,
                    onValueChange = { vm.updatePromptText(it) },
                    placeholder = { Text("e.g., Cyberpunk sunset", color = colors.textMuted, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = TextStyle(fontSize = 12.sp, color = colors.textMain),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.glassBorder,
                        focusedBorderColor = colors.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(24.dp))
                }
            }
        }

        // EXTRACT FROM IMAGE
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("EXTRACT FROM IMAGE", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.glassBg, contentColor = colors.textMain)
            ) {
                Icon(Icons.Filled.Image, contentDescription = "Upload", modifier = Modifier.size(18.dp))
                Spacer(Modifier.padding(start = 8.dp))
                Text("Upload Reference Image", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // POST-PROCESSING FX
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("POST-PROCESSING FX", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            var expanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(vm.postFxMode.label, fontSize = 12.sp, color = colors.textMain)
                    Icon(Icons.Filled.ExpandMore, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.glassBg)
                ) {
                    PostProcessingFx.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label, color = colors.textMain, fontSize = 12.sp) },
                            onClick = {
                                vm.updatePostFxMode(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // MOUSE REACTIVITY
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("MOUSE REACTIVITY", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Enable interactive hover effects", color = colors.textMuted, fontSize = 10.sp)
            }
            Switch(
                checked = vm.mouseReactivity,
                onCheckedChange = { vm.toggleMouseReactivity(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
            )
        }

        // CSS ANIMATION
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CSS ANIMATION", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AnimStatus.STOPPED to Icons.Filled.Stop,
                    AnimStatus.PLAYING to Icons.Filled.PlayArrow,
                    AnimStatus.PAUSED to Icons.Filled.Pause
                ).forEach { (status, icon) ->
                    val active = vm.globalAnimStatus == status
                    IconButton(
                        onClick = { vm.setAnimStatus(status) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) colors.primary else colors.glassBg)
                            .border(1.dp, if (active) colors.primary else colors.glassBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(icon, contentDescription = status.name, tint = if (active) colors.onPrimary else colors.textMain, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            var expanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(vm.globalAnimStyle.label, fontSize = 12.sp, color = colors.textMain)
                    Icon(Icons.Filled.ExpandMore, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.glassBg)
                ) {
                    AnimStyle.entries.forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.label, color = colors.textMain, fontSize = 12.sp) },
                            onClick = {
                                vm.setAnimStyle(style)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SPEED", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(vm.globalAnimSpeed.toInt().toString(), color = colors.textMain, fontSize = 10.sp)
                    }
                    Slider(
                        value = vm.globalAnimSpeed,
                        onValueChange = { vm.updateGlobalAnimSpeed(it) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("AMOUNT", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(vm.globalAnimAmount.toInt().toString(), color = colors.textMain, fontSize = 10.sp)
                    }
                    Slider(
                        value = vm.globalAnimAmount,
                        onValueChange = { vm.updateGlobalAnimAmount(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }
            }
        }

        Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))

        // APPLE STYLE PRESETS
        Text("APPLE STYLE PRESETS", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ApplePresets) { preset ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(preset.colors))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            vm.applyPreset(preset.colors, preset.type)
                        }
                )
            }
        }

        Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))

        Text("ACCESSIBILITY", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
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
        
        Spacer(Modifier.height(32.dp))
    }
}
