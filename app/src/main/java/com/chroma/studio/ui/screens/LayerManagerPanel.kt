package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.composables.icons.lucide.*

@Composable
fun LayerManagerPanel(
    clips: List<Clip>,
    trackVisibility: Map<Int, Boolean>,
    trackLock: Map<Int, Boolean>,
    onMoveTrack: (fromTrack: Int, toTrack: Int) -> Unit,
    onToggleVisibility: (trackId: Int) -> Unit,
    onToggleLock: (trackId: Int) -> Unit
) {
    val colors = LocalOscellionColors.current
    
    // Group clips by trackId to show one row per track
    val tracks = remember(clips) {
        clips.groupBy { it.trackId }.keys.sortedDescending()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Layers & Tracks",
            color = Color(0xFF1C1C1E),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tracks) { trackId ->
                val trackName = when (trackId) {
                    1 -> "Main Video"
                    2 -> "B-Roll"
                    3 -> "Text / Titles"
                    4 -> "Audio / SFX"
                    5 -> "Stickers"
                    else -> "Track $trackId"
                }
                
                val isVisible = trackVisibility[trackId] ?: true
                val isLocked = trackLock[trackId] ?: false

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF9FAFB))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.GripVertical,
                        contentDescription = "Reorder",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Text(
                        text = trackName,
                        color = Color(0xFF1C1C1E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Visibility Toggle
                    Icon(
                        imageVector = if (isVisible) Lucide.Eye else Lucide.EyeOff,
                        contentDescription = "Toggle Visibility",
                        tint = if (isVisible) Color(0xFF1C1C1E) else Color(0xFF9CA3AF),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { 
                                onToggleVisibility(trackId)
                            }
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    // Lock Toggle
                    Icon(
                        imageVector = Lucide.Lock,
                        contentDescription = "Toggle Lock",
                        tint = if (isLocked) colors.primary else Color(0xFF9CA3AF),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { 
                                onToggleLock(trackId)
                            }
                    )
                }
            }
        }
    }
}
