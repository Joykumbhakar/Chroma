package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors

@Composable
fun ExportSettingsDialog(
    initialSettings: ExportSettings,
    onDismiss: () -> Unit,
    onExport: (ExportSettings) -> Unit
) {
    val colors = LocalOscellionColors.current
    var resolution by remember { mutableStateOf(initialSettings.resolution) }
    var fps by remember { mutableStateOf(initialSettings.fps) }
    var codec by remember { mutableStateOf(initialSettings.codec) }
    
    val resolutions = listOf(720, 1080, 2160)
    val fpsList = listOf(24, 30, 60)
    val codecs = listOf("libx264", "libx265")

    com.chroma.studio.ui.components.GlassDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Text("Export Settings", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // Resolution
            Text("Resolution", color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                resolutions.forEach { res ->
                    val selected = resolution == res
                    val label = if (res == 2160) "4K" else "${res}p"
                    OptionChip(label, selected) { resolution = res }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // FPS
            Text("Frame Rate", color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fpsList.forEach { f ->
                    OptionChip("${f}fps", fps == f) { fps = f }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Codec
            Text("Codec", color = colors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                codecs.forEach { c ->
                    val label = if (c == "libx264") "H.264" else "HEVC (H.265)"
                    OptionChip(label, codec == c) { codec = c }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.chroma.studio.ui.components.ChromaPillButton(
                    onClick = onDismiss,
                    variant = com.chroma.studio.ui.components.ChromaPillVariant.DarkGlass,
                    height = 36.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                com.chroma.studio.ui.components.ChromaPillButton(
                    onClick = { onExport(ExportSettings(resolution, fps, codec)) },
                    variant = com.chroma.studio.ui.components.ChromaPillVariant.Emerald,
                    height = 36.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text("Export", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RowScope.OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.2f) else colors.glassBgHover.copy(alpha = 0.3f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.glassBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) colors.primary else colors.textMain,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
