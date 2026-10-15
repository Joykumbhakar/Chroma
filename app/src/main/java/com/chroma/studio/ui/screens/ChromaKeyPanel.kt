package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pipette

@Composable
fun ChromaKeyPanel(
    clip: Clip,
    viewModel: ChromaEditorViewModel
) {
    val colors = LocalOscellionColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chroma Key", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            AppleSwitch(
                checked = clip.chromaKeyEnabled,
                onCheckedChange = { viewModel.updateClipChromaKeyEnabled(clip.id, it) }
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (clip.chromaKeyEnabled) {
            // Color Picker Mock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Key Color", color = colors.textMain, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(clip.chromaKeyColor)
                        .border(1.dp, colors.glassBorder, CircleShape)
                        .clickable { /* Open Color Picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Pipette, contentDescription = "Pick Color", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("Intensity", color = colors.textMain, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            AppleSlider(
                value = clip.chromaKeyIntensity,
                onValueChange = { viewModel.updateClipChromaKeyIntensity(clip.id, it) },
                valueRange = 0.01f..1.0f
            )
            
            Spacer(Modifier.height(8.dp))
            Text("Spill Reduction", color = colors.textMain, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            AppleSlider(
                value = clip.chromaKeySpill,
                onValueChange = { viewModel.updateClipChromaKeySpill(clip.id, it) },
                valueRange = 0f..1.0f
            )
        }
    }
}
