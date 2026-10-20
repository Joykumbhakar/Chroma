@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    com.chroma.studio.ui.components.LiquidGlassBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Export Video",
                color = Color(0xFF111827),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Resolution
            Text("Resolution", color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                resolutions.forEach { res ->
                    val selected = resolution == res
                    val label = if (res == 2160) "4K" else "${res}p"
                    OptionChip(label, selected) { resolution = res }
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            // FPS
            Text("Frame Rate", color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fpsList.forEach { f ->
                    OptionChip("${f}fps", fps == f) { fps = f }
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            // Codec
            Text("Format", color = Color(0xFF6B7280), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                codecs.forEach { c ->
                    val label = if (c == "libx264") "H.264" else "HEVC"
                    OptionChip(label, codec == c) { codec = c }
                }
            }
            
            Spacer(Modifier.height(22.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6))
                ) {
                    Text("Cancel", color = Color(0xFF374151), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onExport(ExportSettings(resolution, fps, codec)) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Export", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF374151),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
