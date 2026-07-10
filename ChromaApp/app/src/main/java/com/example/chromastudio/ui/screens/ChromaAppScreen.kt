package com.example.chromastudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chromastudio.theme.LocalChromaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun ChromaAppScreen() {
    val hazeState = remember { HazeState() }
    val colors = LocalChromaColors.current

    // Main scaffold wrapper
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background layer captured by Haze
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.gridLine) // TODO: implement the actual grid pattern later
                .haze(
                    state = hazeState,
                    style = HazeStyle(backgroundColor = Color.Transparent, blurRadius = 30.dp)
                )
        )
        
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(hazeState)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel
                LeftPanel(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    hazeState = hazeState
                )

                // Center Canvas
                CenterCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    hazeState = hazeState
                )

                // Right Panel
                RightPanel(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight(),
                    hazeState = hazeState
                )
            }
        }
    }
}

@Composable
fun TopAppBar(hazeState: HazeState) {
    val colors = LocalChromaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(backgroundColor = colors.glassBg, blurRadius = 30.dp)
            )
            .background(colors.glassBg, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF22D3EE))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎨",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chroma",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Studio",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Center Pills
        Row(
            modifier = Modifier
                .background(colors.glassBg, RoundedCornerShape(50))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(50))
                .padding(4.dp)
        ) {
            ShapeButton("Card", active = true)
            ShapeButton("Circle")
            ShapeButton("Full")
            ShapeButton("Text")
        }

        // Right Actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("🛠", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Handoff", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { /*TODO*/ }) {
                Text("🔄")
            }
        }
    }
}

@Composable
fun ShapeButton(text: String, active: Boolean = false) {
    val colors = LocalChromaColors.current
    val bgColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (active) Color.White else colors.textMuted

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun LeftPanel(modifier: Modifier = Modifier, hazeState: HazeState) {
    val colors = LocalChromaColors.current
    Column(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                style = HazeStyle(backgroundColor = colors.glassBg, blurRadius = 30.dp)
            )
            .background(colors.glassBg, RoundedCornerShape(16.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "GLOBAL FX & PRESETS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 1.sp
            )
        }
        HorizontalDivider(color = colors.glassBorder)
        
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Prompt
            Column {
                Text("AI Prompt-to-Palette", fontSize = 12.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = "", onValueChange = {}, 
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = colors.glassBg,
                            focusedContainerColor = colors.glassBgHover,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("e.g. Cyberpunk neon...", fontSize = 12.sp) }
                    )
                    Button(onClick = {}, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.primaryHover)) {
                        Text("AI", fontSize = 12.sp)
                    }
                }
            }
            
            HorizontalDivider(color = colors.glassBorder)
            
            // Post Processing
            Column {
                Text("Post-Processing FX", fontSize = 12.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                // Dropdown placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colors.glassBg, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("None (Clean)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            HorizontalDivider(color = colors.glassBorder)
            
            // Mouse Reactivity
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Mouse Reactivity", fontSize = 10.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
                    Text("Parallax effect on cursor move", fontSize = 9.sp, color = colors.textMuted)
                }
                var checked by remember { mutableStateOf(false) }
                com.example.chromastudio.ui.components.ChromaSwitch(checked = checked, onCheckedChange = { checked = it })
            }
        }
    }
}

@Composable
fun CenterCanvas(modifier: Modifier = Modifier, hazeState: HazeState) {
    val colors = LocalChromaColors.current
    Box(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                style = HazeStyle(backgroundColor = colors.glassBg, blurRadius = 30.dp)
            )
            .background(colors.glassBg, RoundedCornerShape(16.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Mocking the main canvas card
        Box(
            modifier = Modifier
                .width(400.dp)
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
        ) {
            Text(
                text = "CHROMA",
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                color = Color.Black.copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun RightPanel(modifier: Modifier = Modifier, hazeState: HazeState) {
    val colors = LocalChromaColors.current
    Column(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                style = HazeStyle(backgroundColor = colors.glassBg, blurRadius = 30.dp)
            )
            .background(colors.glassBg, RoundedCornerShape(16.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "LAYERS & STYLES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 1.sp
            )
        }
        HorizontalDivider(color = colors.glassBorder)
        
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Blend Mode", fontSize = 12.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.glassBg, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Normal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            
            // Layer list
            LayerItem(title = "Background", active = true)
            LayerItem(title = "Glass Card", active = false)
            LayerItem(title = "Blob 1 (Gradient)", active = false)
        }
    }
}

@Composable
fun LayerItem(title: String, active: Boolean) {
    val colors = LocalChromaColors.current
    val bgColor = if (active) colors.primaryHover.copy(alpha = 0.1f) else colors.glassBg
    val borderColor = if (active) colors.primaryHover else colors.glassBorder
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}
