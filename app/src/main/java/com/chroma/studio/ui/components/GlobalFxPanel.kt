package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Zap
import com.composables.icons.lucide.Grid3x3
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.Activity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun GlobalFxPanel(vm: ChromaViewModel, modifier: Modifier = Modifier) {
    val colors = LocalChromaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                launch(Dispatchers.Main) { vm.generatePaletteFromImage(bitmap) }
            }
        }
    }

    GlassPanel(modifier = modifier.fillMaxSize(), cornerRadius = 16) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── AI PROMPT (kept as-is per request) ──────────────────────────
            item {
                FxSectionCard(
                    title = "AI PROMPT-TO-PALETTE",
                    icon = Lucide.Sparkles,
                    defaultExpanded = true
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vm.promptText,
                            onValueChange = { vm.updatePromptText(it) },
                            placeholder = { Text("e.g., Cyberpunk sunset", color = colors.textMuted, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            textStyle = TextStyle(fontSize = 12.sp, color = colors.textMain),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = colors.glassBorder,
                                focusedBorderColor = colors.primary,
                                unfocusedContainerColor = colors.glassBg,
                                focusedContainerColor = colors.glassBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = { vm.generateAiPalette(vm.promptText) },
                            modifier = Modifier.size(48.dp).glossyBorder(RoundedCornerShape(8.dp), colors),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                        ) {
                            Icon(Lucide.Sparkles, contentDescription = "Generate", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── EXTRACT FROM IMAGE ───────────────────────────────────────────
            item {
                FxSectionCard(title = "EXTRACT FROM IMAGE", icon = Lucide.Image) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassBg)
                            .glossyBorder(RoundedCornerShape(10.dp), colors)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                pickMedia.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Lucide.Image, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(18.dp))
                            Text("Upload Reference Image", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                        }
                    }
                }
            }

            // ── CSS ANIMATION ────────────────────────────────────────────────
            item {
                FxSectionCard(title = "CSS ANIMATION", icon = Lucide.Play) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Playback controls — TypeButton style
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AnimStatus.STOPPED to Lucide.Square,
                                AnimStatus.PLAYING to Lucide.Play,
                                AnimStatus.PAUSED to Lucide.Pause
                            ).forEach { (status, icon) ->
                                val active = vm.globalAnimStatus == status
                                val shape = RoundedCornerShape(10.dp)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shape)
                                        .background(colors.glassBg)
                                        .border(
                                            width = if (active) 2.dp else 1.dp,
                                            brush = if (active) {
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
                                            shape = shape
                                        )
                                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                            vm.setAnimStatus(status)
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(icon, contentDescription = status.name,
                                        tint = if (active) colors.primary else colors.textMain,
                                        modifier = Modifier.size(18.dp))
                                    Text(status.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 10.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) colors.primary else colors.textMain)
                                }
                            }
                        }

                        // Style dropdown — matches BlendModeSelector in LayerCard
                        FxDropdown(
                            label = "STYLE",
                            value = vm.globalAnimStyle.label,
                            onExpand = {}
                        ) { dismiss ->
                            AnimStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.label, color = colors.textMain, fontSize = 12.sp) },
                                    onClick = { vm.setAnimStyle(style); dismiss() }
                                )
                            }
                        }

                        // Speed slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SPEED", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${vm.globalAnimSpeed.toInt()}", color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            ChromaSlider(
                                value = vm.globalAnimSpeed,
                                onValueChange = { vm.updateGlobalAnimSpeed(it) },
                                valueRange = 10f..100f
                            )
                        }

                        // Amount slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("AMOUNT", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${vm.globalAnimAmount.toInt()}", color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            ChromaSlider(
                                value = vm.globalAnimAmount,
                                onValueChange = { vm.updateGlobalAnimAmount(it) },
                                valueRange = 0f..100f
                            )
                        }
                    }
                }
            }

            // ── POST-PROCESSING FX ───────────────────────────────────────────
            item {
                FxSectionCard(title = "POST-PROCESSING FX", icon = Lucide.Zap) {
                    FxDropdown(
                        label = "MODE",
                        value = vm.postFxMode.label,
                        onExpand = {}
                    ) { dismiss ->
                        PostProcessingFx.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label, color = colors.textMain, fontSize = 12.sp) },
                                onClick = { vm.updatePostFxMode(mode); dismiss() }
                            )
                        }
                    }
                }
            }

            // ── MOUSE REACTIVITY ─────────────────────────────────────────────
            item {
                FxSectionCard(title = "MOUSE REACTIVITY", icon = Lucide.Activity, collapsible = false) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Parallax effect on cursor move", color = colors.textMuted, fontSize = 11.sp)
                        SmallSwitch(
                            checked = vm.mouseReactivity,
                            onCheckedChange = { vm.toggleMouseReactivity(it) }
                        )
                    }
                }
            }

            // ── APPLE STYLE PRESETS ──────────────────────────────────────────
            item {
                FxSectionCard(title = "APPLE STYLE PRESETS", icon = Lucide.Grid3x3, defaultExpanded = true) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(164.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ApplePresets) { preset ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(4.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(preset.colors))
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                        vm.applyPreset(preset.colors, preset.type)
                                    }
                            )
                        }
                    }
                }
            }

            // ── ACCESSIBILITY ────────────────────────────────────────────────
            item {
                FxSectionCard(title = "ACCESSIBILITY", icon = Lucide.CircleDashed) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorBlindMode.entries.forEach { mode ->
                            val active = vm.colorBlindMode == mode
                            val shape = RoundedCornerShape(10.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .background(if (active) colors.primary else colors.glassBg)
                                    .border(1.dp, if (active) colors.primary else colors.glassBorder, shape)
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                        vm.updateColorBlindMode(mode)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    mode.label,
                                    color = if (active) colors.onPrimary else colors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                                if (active) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(colors.onPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Contrast Checker row — same SmallSwitch pattern as LayerCard toggles
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassBg)
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Contrast Checker", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            SmallSwitch(
                                checked = vm.contrastCheckerEnabled,
                                onCheckedChange = { vm.toggleContrastChecker() }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Reusable section card — matches LayerCard's collapsible glass card pattern ──

@Composable
private fun FxSectionCard(
    title: String,
    icon: ImageVector,
    collapsible: Boolean = true,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LocalChromaColors.current
    var expanded by remember { mutableStateOf(!collapsible || defaultExpanded) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glassBg, shape)
            .border(
                width = 1.dp,
                brush = if (expanded && collapsible) {
                    SolidColor(colors.primary)
                } else {
                    Brush.linearGradient(
                        listOf(
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                        )
                    )
                },
                shape = shape
            )
            .padding(12.dp)
    ) {
        // Header row — identical to LayerCard header style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (collapsible) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                color = colors.textMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (collapsible) {
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = "Expand",
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }

        // Expandable content — same AnimatedVisibility spec as LayerCard
        if (collapsible) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        } else {
            Column(Modifier.padding(top = 12.dp)) {
                content()
            }
        }
    }
}

// ── Styled dropdown — matches BlendModeSelector in LayerCard ────────────────

@Composable
private fun FxDropdown(
    label: String,
    value: String,
    onExpand: () -> Unit,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    val colors = LocalChromaColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassBg)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                    .clickable { expanded = true; onExpand() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(value, fontSize = 12.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                Icon(Lucide.ChevronDown, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.glassBg)
            ) {
                menuContent { expanded = false }
            }
        }
    }
}
