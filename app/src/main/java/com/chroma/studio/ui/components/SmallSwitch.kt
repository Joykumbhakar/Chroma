package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chroma.studio.ui.theme.LocalChromaColors

@Composable
fun SmallSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.textMuted.copy(alpha = 0.3f),
        label = "trackColor",
        animationSpec = tween(200)
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 16.dp else 2.dp,
        label = "thumbOffset",
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .size(width = 32.dp, height = 18.dp)
            .clip(RoundedCornerShape(9.dp))
            .glossyBorder(RoundedCornerShape(9.dp), colors)
            .background(trackColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
