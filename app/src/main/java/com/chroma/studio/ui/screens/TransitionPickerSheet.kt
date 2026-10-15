package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide

@Composable
fun TransitionPickerSheet(
    currentTransition: TransitionType,
    onTransitionSelected: (TransitionType) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(colors.bg)
            .padding(16.dp)
    ) {
        Text(
            text = "Select Transition",
            color = colors.textMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TransitionType.values()) { type ->
                val selected = type == currentTransition
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) colors.primary.copy(alpha = 0.2f) else colors.glassBgHover)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onTransitionSelected(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selected) {
                            Icon(Lucide.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = type.label,
                            color = if (selected) colors.primary else colors.textMain,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
