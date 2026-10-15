package com.chroma.studio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import com.chroma.studio.R

import com.chroma.studio.ui.theme.LocalOscellionColors

@Composable
fun OscellionBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Theme-aware Background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalOscellionColors.current.bg)
    ) {
        // Grain overlay
        val imageBitmap = ImageBitmap.imageResource(R.drawable.noise_grain)
        val brush = ShaderBrush(ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated))
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, alpha = 1f) // The PNG itself is generated with low opacity
        )
        
        // Content
        content()
    }
}
