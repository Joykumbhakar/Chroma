package com.example.chromastudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val Primary = Color(0xFF4F46E5)
val PrimaryHover = Color(0xFF4338CA)
val BgLight = Color(0xFFF8FAFC)
val BgDark = Color(0xFF0D1117)
val TextMainLight = Color(0xFF111827)
val TextMainDark = Color(0xFFF9FAFB)
val TextMutedLight = Color(0xFF4B5563)
val TextMutedDark = Color(0xFF9CA3AF)
val ErrorColor = Color(0xFFEF4444)
val SuccessColor = Color(0xFF10B981)

val GlassBgLight = Color(0x8CFFFFFF) // 55%
val GlassBgDark = Color(0x8C191C29)
val GlassBorderLight = Color(0x99FFFFFF) // 60%
val GlassBorderDark = Color(0x14FFFFFF) // 8%

// App State Models
data class LayerModel(
    val id: String,
    var name: String,
    var isVisible: Boolean = true,
    var opacity: Float = 1.0f,
    var type: EngineType = EngineType.BLOB,
    var blendMode: String = "Normal"
)

enum class EngineType(val title: String) {
    LINEAR("Linear"), RADIAL("Radial"), CONIC("Conic"),
    MESH("Mesh"), BLOB("Blob"), AURORA("Aurora"), LIQUID("Liquid")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChromaStudioApp()
        }
    }
}

@Composable
fun ChromaStudioApp() {
    var isDarkTheme by remember { mutableStateOf(true) }
    val hazeState = remember { HazeState() }
    
    val bgColor = if (isDarkTheme) BgDark else BgLight
    val textColor = if (isDarkTheme) TextMainDark else TextMainLight

    // State
    var layers by remember { 
        mutableStateOf(listOf(
            LayerModel("1", "Mix 1", type = EngineType.BLOB),
            LayerModel("2", "Base Gradient", type = EngineType.LINEAR)
        )) 
    }
    var activeLayerId by remember { mutableStateOf<String?>("1") }
    var showExportModal by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // 1. Grid Background (Bottom Layer)
            GridPattern(isDarkTheme)

            // 2. Main Content (Feeds into Haze for glass effect)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
            ) {
                TopNavBar(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onHandoffClick = { showExportModal = true }
                )
                
                PreviewCanvas(
                    modifier = Modifier.weight(1f),
                    isDarkTheme = isDarkTheme
                )
            }

            // 3. Glass Overlays (Drawer and Modals)
            BottomDrawer(
                isDarkTheme = isDarkTheme,
                hazeState = hazeState,
                layers = layers,
                activeLayerId = activeLayerId,
                onLayerClick = { activeLayerId = if(activeLayerId == it) null else it },
                onToggleVisibility = { id -> 
                    layers = layers.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it }
                },
                onAddLayer = {
                    layers = listOf(LayerModel(System.currentTimeMillis().toString(), "New Layer")) + layers
                }
            )
            
            if (showExportModal) {
                ExportModal(
                    isDarkTheme = isDarkTheme,
                    hazeState = hazeState,
                    onClose = { showExportModal = false }
                )
            }
        }
    }
}

@Composable
fun Modifier.glassPanel(isDarkTheme: Boolean, hazeState: HazeState, shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    val bgColor = if (isDarkTheme) GlassBgDark else GlassBgLight
    val borderColor = if (isDarkTheme) GlassBorderDark else GlassBorderLight
    
    return this
        .hazeChild(
            state = hazeState, 
            shape = shape,
            style = HazeStyle(backgroundColor = bgColor, tint = null)
        )
        .background(bgColor, shape)
        .border(1.dp, borderColor, shape)
}

@Composable
fun GridPattern(isDarkTheme: Boolean) {
    val lineColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 60f
        var x = 0f
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSize
        }
        var y = 0f
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSize
        }
        // Simulated radial mask fade at bottom
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, if(isDarkTheme) BgDark else BgLight),
                startY = size.height * 0.5f,
                endY = size.height
            ),
            size = size
        )
    }
}

@Composable
fun PreviewCanvas(modifier: Modifier = Modifier, isDarkTheme: Boolean) {
    val borderColor = if (isDarkTheme) GlassBorderDark else GlassBorderLight
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        // MOCK GRADIENT PREVIEW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF00EA).copy(alpha = 0.6f),
                            Color(0xFF4300FF).copy(alpha = 0.8f)
                        ),
                        center = Offset(300f, 300f),
                        radius = 800f
                    )
                )
        ) {
            // Mock Blob points
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(Color(0x33FFFFFF), radius = 250f, center = Offset(size.width*0.4f, size.height*0.4f), style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                drawCircle(Color(0xFFFFFFFF), radius = 16f, center = Offset(size.width*0.4f, size.height*0.4f))
                drawCircle(Color(0xFFFFD60A), radius = 12f, center = Offset(size.width*0.4f, size.height*0.4f))
            }
        }
    }
}

@Composable
fun TopNavBar(isDarkTheme: Boolean, onToggleTheme: () -> Unit, onHandoffClick: () -> Unit) {
    val textColor = if (isDarkTheme) TextMainDark else TextMainLight
    val mutedColor = if (isDarkTheme) TextMutedDark else TextMutedLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF00C8E8))),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lens, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Chroma", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Studio", color = Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Handoff Button
            Button(
                onClick = onHandoffClick,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Rounded.Code, contentDescription = "Handoff", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Handoff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Theme Toggle
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = "Theme",
                    tint = textColor
                )
            }
        }
    }
}

@Composable
fun BottomDrawer(
    isDarkTheme: Boolean, hazeState: HazeState,
    layers: List<LayerModel>, activeLayerId: String?,
    onLayerClick: (String) -> Unit, onToggleVisibility: (String) -> Unit, onAddLayer: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // Snap Points
    val collapsedHeight = 90.dp
    val midHeight = screenHeight * 0.5f
    val expandedHeight = screenHeight * 0.9f
    
    val density = LocalDensity.current
    val drawerOffsetY = remember { Animatable(with(density) { (screenHeight - midHeight).toPx() }) }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("Layers") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight)
                .offset { IntOffset(0, drawerOffsetY.value.roundToInt()) }
                .glassPanel(isDarkTheme, hazeState, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newY = drawerOffsetY.value + dragAmount
                                // Clamp between expanded (top) and collapsed (bottom)
                                val minY = with(density) { (screenHeight - expandedHeight).toPx() }
                                val maxY = with(density) { (screenHeight - collapsedHeight).toPx() }
                                drawerOffsetY.snapTo(newY.coerceIn(minY, maxY))
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val currentY = drawerOffsetY.value
                                val minPx = with(density) { (screenHeight - expandedHeight).toPx() }
                                val midPx = with(density) { (screenHeight - midHeight).toPx() }
                                val maxPx = with(density) { (screenHeight - collapsedHeight).toPx() }

                                val target = listOf(minPx, midPx, maxPx).minByOrNull { Math.abs(it - currentY) } ?: midPx
                                drawerOffsetY.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drawer Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(6.dp)
                            .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
                    )
                }

                // Mobile Segmented Tab
                SegmentedTabs(
                    isDarkTheme = isDarkTheme,
                    tabs = listOf("Layers Stack", "Global FX"),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                // Actions Header
                if (selectedTab == "Layers Stack") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ACTIVE LAYERS",
                            color = if(isDarkTheme) TextMutedDark else TextMutedLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAddLayer,
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Layers List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .padding(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        layers.forEach { layer ->
                            LayerCardItem(
                                layer = layer,
                                isExpanded = layer.id == activeLayerId,
                                isDarkTheme = isDarkTheme,
                                hazeState = hazeState,
                                onClick = { onLayerClick(layer.id) },
                                onToggleVisibility = { onToggleVisibility(layer.id) }
                            )
                        }
                    }
                } else {
                    // Global FX Mock Content
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
                        Text("Global FX Settings (Coming Soon)", color = if(isDarkTheme) TextMutedDark else TextMutedLight)
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedTabs(isDarkTheme: Boolean, tabs: List<String>, selectedTab: String, onTabSelected: (String) -> Unit) {
    val bgColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val indicatorColor = if (isDarkTheme) GlassBgDark else Color.White
    
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(24.dp))
            .padding(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) indicatorColor else Color.Transparent)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) (if(isDarkTheme) TextMainDark else TextMainLight) else (if(isDarkTheme) TextMutedDark else TextMutedLight)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomIOSSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val animatedOffset by animateFloatAsState(targetValue = if (checked) 20f else 2f)
    val bgColor by animateColorAsState(targetValue = if (checked) Primary else Color.Gray.copy(alpha = 0.3f))

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            }
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = animatedOffset.dp, y = 2.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun CustomSlider(value: Float, onValueChange: (Float) -> Unit, label: String, suffix: String = "%", isDarkTheme: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = if(isDarkTheme) TextMutedDark else TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${value.toInt()}$suffix", color = if(isDarkTheme) TextMainDark else TextMainLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun LayerCardItem(
    layer: LayerModel,
    isExpanded: Boolean,
    isDarkTheme: Boolean,
    hazeState: HazeState,
    onClick: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    val borderColor = if (isExpanded) Primary else if (isDarkTheme) GlassBorderDark else GlassBorderLight
    val titleColor = if (isDarkTheme) TextMainDark else TextMainLight
    val mutedColor = if (isDarkTheme) TextMutedDark else TextMutedLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(isDarkTheme, hazeState, RoundedCornerShape(12.dp))
            .border(if (isExpanded) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f))
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.DragIndicator, contentDescription = "Drag", tint = mutedColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (layer.isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Visibility", tint = mutedColor, modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = "Engine", tint = Primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(layer.name, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Up", tint = mutedColor, modifier = Modifier.size(20.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down", tint = mutedColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(18.dp))
            }
        }

        // Expanded Body
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Opacity Slider
                CustomSlider(value = layer.opacity * 100, onValueChange = { layer.opacity = it / 100f }, label = "OPACITY", isDarkTheme = isDarkTheme)
                
                Spacer(Modifier.height(16.dp))
                
                // Active Element Color 
                Text("Active Element Color", color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(isDarkTheme, hazeState, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // Mock Color Picker Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.White, Color.Red, Color.Black)
                                    )
                                )
                        ) {
                            Box(modifier = Modifier.offset(40.dp, 40.dp).size(16.dp).border(2.dp, Color.White, CircleShape))
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Hex Input Mock
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).height(40.dp).background(Color.Gray.copy(0.2f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Text("#FF00EA", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(0.5f).height(40.dp).background(Color.Gray.copy(0.2f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Text("100%", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { /* Auto Harmony */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Primary.copy(alpha=0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription="Auto", tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-Harmony", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportModal(isDarkTheme: Boolean, hazeState: HazeState, onClose: () -> Unit) {
    val bgColor = if (isDarkTheme) BgDark else BgLight
    val textColor = if (isDarkTheme) TextMainDark else TextMainLight
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {}, // Block touches
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .glassPanel(isDarkTheme, hazeState, RoundedCornerShape(20.dp))
                .background(bgColor)
                .clip(RoundedCornerShape(20.dp))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if(isDarkTheme) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, contentDescription="Code", tint = textColor)
                    Spacer(Modifier.width(8.dp))
                    Text("DEVELOPER HANDOFF", color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = textColor)
                }
            }

            // Export Actions (Image/Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportBadge("PNG", isDarkTheme)
                    ExportBadge("JPG", isDarkTheme)
                    ExportBadge("SVG", isDarkTheme)
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription="Video", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Record .webm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

            // Code Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = ".chroma-bg {\n  width: 100%;\n  height: 100vh;\n  position: relative;\n  background-color: #0D1117;\n}\n\n.chroma-bg::before {\n  content: '';\n  position: absolute;\n  inset: 0;\n  background: radial-gradient(...);\n  mix-blend-mode: overlay;\n}",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Target Size Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if(isDarkTheme) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Target Max Size", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=8.dp)) {
                        Box(modifier = Modifier.background(Color.Gray.copy(0.2f), RoundedCornerShape(8.dp)).padding(horizontal=12.dp, vertical=8.dp)) {
                            Text("200", color = textColor, fontWeight = FontWeight.Bold)
                        }
                        Text(" KB", color = if(isDarkTheme) TextMutedDark else TextMutedLight, modifier = Modifier.padding(start=8.dp, end=16.dp), fontWeight = FontWeight.Bold)
                        
                        Box(modifier = Modifier.background(Color.Gray.copy(0.2f), RoundedCornerShape(8.dp)).padding(horizontal=12.dp, vertical=8.dp)) {
                            Text("JPG ▼", color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription="DL")
                    Spacer(Modifier.width(8.dp))
                    Text("DOWNLOAD", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExportBadge(label: String, isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .background(if(isDarkTheme) Color.White.copy(alpha=0.05f) else Color.White)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = if(isDarkTheme) TextMainDark else TextMainLight)
            Spacer(Modifier.width(4.dp))
            Text(label, color = if(isDarkTheme) TextMainDark else TextMainLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
