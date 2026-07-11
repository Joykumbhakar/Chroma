package com.example.chromastudio.ui.components

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
import com.example.chromastudio.theme.LocalChromaColors

@Composable
fun ChromaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalChromaColors.current
    val bgColor = if (checked) colors.primaryHover else colors.gridLine.copy(alpha = 0.15f)
    
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun ChromaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(32.dp),
        colors = SliderDefaults.colors(
            thumbColor = LocalChromaColors.current.primaryHover,
            activeTrackColor = LocalChromaColors.current.primaryHover,
            inactiveTrackColor = Color(0x26000000)
        )
    )
}
