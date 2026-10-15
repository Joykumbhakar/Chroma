package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.IosAlertDialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import com.chroma.studio.ui.theme.InterFontFamily

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.google.firebase.auth.FirebaseAuth
import com.chroma.studio.ui.screens.LoginScreen
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.chroma.studio.MainActivity
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.components.SmallSwitch
import com.composables.icons.lucide.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import java.text.SimpleDateFormat
import java.util.Locale

val DrawerIconGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF006FFF), Color(0xFF111C42)),
    start = Offset.Zero,
    end = Offset.Infinite
)

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconGradient: List<Color>? = null,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(10.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC))
                .background(
                    if (iconGradient != null) Brush.linearGradient(iconGradient, start = Offset.Zero, end = Offset.Infinite)
                    else DrawerIconGradient
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Text(
            text = label,
            color = if (selected) Color(0xFF006FFF) else colors.textMain,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

enum class ViewMode {
    GRID, LIST, GRID_DETAILS, LIST_DETAILS;
    fun next(): ViewMode {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }
    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (this) {
            GRID -> Lucide.LayoutGrid
            LIST -> Lucide.List
            GRID_DETAILS -> Lucide.LayoutDashboard
            LIST_DETAILS -> Lucide.AlignJustify
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    works: List<ChromaWork>,
    selectedWorkIds: Set<String>,
    selectionMode: Boolean,
    isDarkMode: Boolean,
    initialViewMode: ViewMode,
    onToggleDarkMode: () -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    onNewWork: () -> Unit,
    onOpenWork: (ChromaWork) -> Unit,
    onRenameWork: (String, String, String) -> Unit, // id, name, desc
    onDuplicateWork: (ChromaWork) -> Unit,
    onToggleSelection: (String) -> Unit,
    onUpdateSelection: (Set<String>) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSoftDelete: (String) -> Unit,
    onEditHomeBackground: () -> Unit,
    customHomeBgLayersJson: String?,
    customHomeBgShape: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToBackgrounds: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onSetAsBackground: (ChromaWork) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onRequireLogin: () -> Unit,
    onSetAsLiveWallpaper: (ChromaWork) -> Unit,
    onShareToDiscover: (String) -> Unit = {},
    isVerified: Boolean,
    repository: WorkRepository,
    showHeader: Boolean = true,
    onNavigateToCreatePost: (() -> Unit)? = null,
    onNavigateToFlashPicker: (() -> Unit)? = null,
    onNavigateToCreateTextStatus: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val gson = remember { com.google.gson.Gson() }
    
    var workToExport by remember { mutableStateOf<ChromaWork?>(null) }
    val exportWorkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && workToExport != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    val jsonString = gson.toJson(workToExport)
                    stream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                com.chroma.studio.ui.components.ToastManager.showToast("Exported JSON successfully")
            } catch (e: Exception) {
                com.chroma.studio.ui.components.ToastManager.showToast("Export failed")
            }
        }
        workToExport = null
    }

    val importWorkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val jsonString = stream.bufferedReader().use { it.readText() }
                    val importedWork = gson.fromJson(jsonString, ChromaWork::class.java)
                    val newWork = importedWork.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        name = importedWork.name + " (Imported)",
                        createdAt = System.currentTimeMillis(),
                        lastModifiedAt = System.currentTimeMillis()
                    )
                    repository.save(newWork)
                    com.chroma.studio.ui.components.ToastManager.showToast("Imported JSON successfully")
                }
            } catch (e: Exception) {
                com.chroma.studio.ui.components.ToastManager.showToast("Import failed")
            }
        }
    }

    var dragAnchorIndex by remember { mutableStateOf<Int?>(null) }
    var dragInitialSelectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dragIsSelecting by remember { mutableStateOf(true) }
    
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var showCreatePostMenu by remember { mutableStateOf(false) }

    if (showLoginScreen) {
        LoginScreen(
            onLoginSuccess = { 
                showLoginScreen = false 
                val intent = Intent(context, com.chroma.studio.MainActivity::class.java).apply {
                    putExtra("EXTRA_START_AI_GENERATOR", true)
                }
                context.startActivity(intent)
            },
            onCancel = { showLoginScreen = false }
        )
        return
    }

    var viewMode by remember { mutableStateOf(initialViewMode) }
    val filteredWorks by remember(works) {
        derivedStateOf {
            if (searchQuery.isBlank()) works.toList()
            else works.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.bg,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
                    Text(
                        "Oscellion",
                        color = colors.textMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 32.dp, top = 24.dp, start = 8.dp)
                    )

                    DrawerItem(
                        icon = Lucide.House,
                        label = "Home",
                        selected = true
                    ) { scope.launch { drawerState.close() } }

                    Spacer(Modifier.height(4.dp))

                    DrawerItem(
                        icon = Lucide.Image,
                        label = "Backgrounds",
                        selected = false
                    ) {
                        scope.launch { drawerState.close() }
                        onNavigateToBackgrounds()
                    }

                    Spacer(Modifier.height(4.dp))

                    DrawerItem(
                        icon = Lucide.Settings,
                        label = "Settings & Preferences",
                        selected = false
                    ) { 
                        scope.launch { drawerState.close() }
                        onNavigateToSettings() 
                    }

                    Spacer(Modifier.height(4.dp))

                    DrawerItem(
                        icon = Lucide.Trash2,
                        label = "Trash",
                        selected = false
                    ) { 
                        scope.launch { drawerState.close() }
                        onNavigateToTrash() 
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider(
                        color = colors.glassBorder.copy(alpha = 0.6f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "PREFERENCES",
                        color = colors.textMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    Spacer(Modifier.height(6.dp))
                    
                    // Dark mode toggle (Clean transparent row with gradient icon & thin glass border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Transparent)
                            .clickable { onToggleDarkMode() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .shadow(
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(10.dp),
                                        spotColor = Color.Black.copy(alpha = 0.15f),
                                        ambientColor = Color.Black.copy(alpha = 0.08f)
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .background(DrawerIconGradient)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Lucide.Moon else Lucide.Sun,
                                    contentDescription = "Theme",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                    color = colors.textMain,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isDarkMode) "Dark theme enabled" else "Light theme enabled",
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        SmallSwitch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() }
                        )
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val effectiveJson = customHomeBgLayersJson ?: DEFAULT_HOME_BG_JSON
            val customLayers = remember(effectiveJson) {
                repository.deserializeLayers(effectiveJson)
            }
            if (customLayers.isNotEmpty()) {
                CanvasPreview(
                    layers = customLayers,
                    shape = customHomeBgShape,
                    borderColor = Color.Transparent,
                    colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,
                    postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,
                    animStatus = com.chroma.studio.model.AnimStatus.PLAYING,
                    animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                    animSpeed = 50f,
                    animAmount = 50f,
                    reactOffset = Offset.Zero,
                    textContent = "",
                    onTextContentChange = {},
                    blobDragOverrides = emptyMap(),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onEditHomeBackground() })
                        }
                )
            }
            Box(modifier = Modifier.fillMaxSize().then(if (showHeader) Modifier.statusBarsPadding() else Modifier)) {
                // â”€â”€ Hero Section â”€â”€
                Column(modifier = Modifier.fillMaxWidth().height(if (showHeader) 420.dp else 340.dp).zIndex(1f).padding(horizontal = 20.dp, vertical = if (showHeader) 16.dp else 12.dp)) {
                    
                    // Top Bar with Animated Search
                    if (showHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Menu -> Import
                        AnimatedVisibility(
                            visible = !searchActive,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Icon(
                                Lucide.Menu,
                                contentDescription = "Menu",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { scope.launch { drawerState.open() } }
                                    .padding(end = 4.dp)
                            )
                        }
                        
                        // Right side: ViewMode + Search
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .animateContentSize()
                                .then(if (searchActive) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                        ) {
                            if (!searchActive) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .clickable { 
                                            importWorkLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Upload, "Import JSON File", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .clickable { 
                                            val clipboardText = clipboardManager.getText()?.text
                                            if (clipboardText.isNullOrBlank()) {
                                                com.chroma.studio.ui.components.ToastManager.showToast("Clipboard is empty")
                                            } else {
                                                try {
                                                    val importedWork = gson.fromJson(clipboardText, com.chroma.studio.model.ChromaWork::class.java)
                                                    if (importedWork != null && importedWork.layersJson.isNotEmpty()) {
                                                        val newWork = importedWork.copy(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            name = importedWork.name + " (Pasted)",
                                                            createdAt = System.currentTimeMillis(),
                                                            lastModifiedAt = System.currentTimeMillis()
                                                        )
                                                        repository.save(newWork)
                                                        com.chroma.studio.ui.components.ToastManager.showToast("Pasted JSON successfully")
                                                    } else {
                                                        com.chroma.studio.ui.components.ToastManager.showToast("Invalid Oscellion JSON format")
                                                    }
                                                } catch (e: Exception) {
                                                    com.chroma.studio.ui.components.ToastManager.showToast("Failed to parse JSON")
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.ClipboardPaste, "Paste JSON", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .clickable { 
                                            viewMode = viewMode.next()
                                            onViewModeChanged(viewMode)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(viewMode.icon, "View Mode", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            if (searchActive) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    decorationBox = { innerTextField ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Lucide.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (searchQuery.isEmpty()) {
                                                    Text("Search works...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                                }
                                                innerTextField()
                                            }
                                            Icon(Lucide.X, null, tint = Color.White, modifier = Modifier.size(18.dp).clickable { 
                                                if (searchQuery.isEmpty()) searchActive = false else searchQuery = "" 
                                            })
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .clickable { searchActive = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Search, "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Text section
                    Text("Gradient create", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Get started", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Button 1: New video
                        val cardBrush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.20f)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                        )
                        val cardTextColor = Color.White
                        
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBrush)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .clickable { showCreatePostMenu = true },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                                Icon(Lucide.Plus, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("New gradient", color = cardTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        // Button 2: Generate (AI)
                        val generateButtonLayers = remember(GENERATE_BUTTON_BG_JSON) {
                            repository.deserializeLayers(GENERATE_BUTTON_BG_JSON)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                    .clickable { 
                                        if (FirebaseAuth.getInstance().currentUser != null) {
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                putExtra("EXTRA_START_AI_GENERATOR", true)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            showLoginScreen = true
                                        }
                                    }
                            ) {
                                CanvasPreview(layers = generateButtonLayers, shape = "full", borderColor = Color.Transparent, modifier = Modifier.fillMaxSize())
                                // Glossy highlight overlay
                                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))))
                                
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                                        Icon(Lucide.Sparkles, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("Generate", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-8).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("AI Powered", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // â”€â”€ Grid Area (Fading to white) â”€â”€
                val density = androidx.compose.ui.platform.LocalDensity.current
                val fadeEndY = remember(density) { with(density) { 110.dp.toPx() } }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 294.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to colors.bg.copy(alpha = 0f),
                                1.0f to colors.bg,
                                startY = 0f,
                                endY = fadeEndY
                            )
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(top = 126.dp)) {
                        // â”€â”€ Contextual Action Bar for Selection Mode â”€â”€
                        if (selectionMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.primary)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlassIconButton(onClick = onClearSelection) {
                                        Icon(Lucide.X, "Cancel", tint = Color.White)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${selectedWorkIds.size} Selected",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                GlassIconButton(onClick = onDeleteSelected) {
                                    Icon(Lucide.Trash2, "Delete Selected", tint = Color.White)
                                }
                            }
                        }
                        // â”€â”€ Work grid â”€â”€
                if (works.isEmpty()) {
                    EmptyState(onNewWork = onNewWork)
                } else if (filteredWorks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No works found for \"$searchQuery\"", color = colors.textMuted, fontSize = 15.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (viewMode == ViewMode.LIST || viewMode == ViewMode.LIST_DETAILS) 300.dp else 80.dp),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectionMode) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val item = gridState.layoutInfo.visibleItemsInfo.find {
                                            offset.x >= it.offset.x && offset.x <= it.offset.x + it.size.width &&
                                            offset.y >= it.offset.y && offset.y <= it.offset.y + it.size.height
                                        }
                                        if (item != null) {
                                            dragAnchorIndex = item.index
                                            dragInitialSelectedIds = selectedWorkIds
                                            val work = filteredWorks.getOrNull(item.index)
                                            if (work != null) {
                                                dragIsSelecting = !selectedWorkIds.contains(work.id)
                                                // Apply initial toggle
                                                val newSelection = if (dragIsSelecting) {
                                                    selectedWorkIds + work.id
                                                } else {
                                                    selectedWorkIds - work.id
                                                }
                                                onUpdateSelection(newSelection)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        dragAnchorIndex = null
                                    },
                                    onDragCancel = {
                                        dragAnchorIndex = null
                                    },
                                    onDrag = { change, _ ->
                                        val anchor = dragAnchorIndex ?: return@detectDragGesturesAfterLongPress
                                        val offset = change.position
                                        val item = gridState.layoutInfo.visibleItemsInfo.find {
                                            offset.x >= it.offset.x && offset.x <= it.offset.x + it.size.width &&
                                            offset.y >= it.offset.y && offset.y <= it.offset.y + it.size.height
                                        }
                                        if (item != null) {
                                            val currentIndex = item.index
                                            val minIdx = minOf(anchor, currentIndex)
                                            val maxIdx = maxOf(anchor, currentIndex)
                                            
                                            val currentDragSet = (minIdx..maxIdx)
                                                .mapNotNull { filteredWorks.getOrNull(it)?.id }
                                                .toSet()

                                            val newSelection = if (dragIsSelecting) {
                                                dragInitialSelectedIds + currentDragSet
                                            } else {
                                                dragInitialSelectedIds - currentDragSet
                                            }
                                            
                                            if (newSelection != selectedWorkIds) {
                                                onUpdateSelection(newSelection)
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        items(filteredWorks, key = { it.id }) { work ->
                            val isSelected = selectedWorkIds.contains(work.id)
                            WorkCard(
                                work = work,
                                isSelected = isSelected,
                                selectionMode = selectionMode,
                                viewMode = viewMode,
                                repository = repository,
                                onClick = { 
                                    if (selectionMode) onToggleSelection(work.id) 
                                    else onOpenWork(work) 
                                },
                                onRename = { name, desc -> onRenameWork(work.id, name, desc) },
                                onDuplicate = { onDuplicateWork(work) },
                                onSoftDelete = { onSoftDelete(work.id) },
                                onSetAsBackground = { onSetAsBackground(work) },
                                onSetAsLiveWallpaper = { onSetAsLiveWallpaper(work) },
                                onShareToDiscover = { onShareToDiscover(work.id) },
                                onExportWork = {
                                    workToExport = work
                                    exportWorkLauncher.launch("${work.name}.json")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
    
if (showCreatePostMenu) {
        CreatePostMenuSheet(
            onDismiss = { showCreatePostMenu = false },
            onCreateGradient = {
                showCreatePostMenu = false
                onNewWork()
            },
            onCreatePost = {
                showCreatePostMenu = false
                if (FirebaseAuth.getInstance().currentUser != null) {
                    onNavigateToCreatePost?.invoke()
                } else {
                    showLoginScreen = true
                }
            },
            onCreateFlash = {
                showCreatePostMenu = false
                if (FirebaseAuth.getInstance().currentUser != null) {
                    onNavigateToFlashPicker?.invoke()
                } else {
                    showLoginScreen = true
                }
            },
            onCreateStory = {
                showCreatePostMenu = false
                if (FirebaseAuth.getInstance().currentUser != null) {
                    onNavigateToCreateTextStatus?.invoke()
                } else {
                    showLoginScreen = true
                }
            },
            onCreateAIGenerate = {
                showCreatePostMenu = false
                if (FirebaseAuth.getInstance().currentUser != null) {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("EXTRA_START_AI_GENERATOR", true)
                    }
                    context.startActivity(intent)
                } else {
                    showLoginScreen = true
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkCard(
    work: ChromaWork,
    isSelected: Boolean,
    selectionMode: Boolean,
    viewMode: ViewMode,
    repository: WorkRepository,
    onClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: () -> Unit,
    onSoftDelete: () -> Unit,
    onSetAsBackground: () -> Unit,
    onSetAsLiveWallpaper: () -> Unit = {},
    onShareToDiscover: () -> Unit = {},
    onExportWork: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isSelected) 0.92f else 1f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val borderColor = if (isSelected) colors.primary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .background(colors.glassBg)
            .clickable(onClick = onClick)
    ) {
        val isListMode = viewMode == ViewMode.LIST || viewMode == ViewMode.LIST_DETAILS
        val showDetails = viewMode == ViewMode.GRID_DETAILS || viewMode == ViewMode.LIST_DETAILS
        
        @Composable
        fun Thumbnail() {
            Box(
                modifier = Modifier
                    .run { if (isListMode) this.width(70.dp).fillMaxHeight() else this.fillMaxWidth().height(70.dp) }
                    .clip(
                        if (isListMode) RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp) 
                        else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }
                if (layers.isEmpty()) {
                    Text(
                        work.name.take(1).uppercase(),
                        color = Color.Black.copy(alpha = 0.2f),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    CanvasPreview(
                        layers = layers,
                        shape = work.canvasShape,
                        borderColor = Color.Transparent,
                        isStatic = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Check, "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        
        @Composable
        fun Info() {
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth().run { if (isListMode) this.fillMaxHeight() else this }, verticalArrangement = Arrangement.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        work.name,
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!selectionMode) {
                        Box {
                            Icon(
                                Lucide.EllipsisVertical,
                                contentDescription = "Options",
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showMenu = true }
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .glossyBorder(RoundedCornerShape(12.dp), colors)
                                    .background(colors.glassBg, RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename / Edit Info", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Pencil, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; showRenameDialog = true }
                                )
                                if (work.isHomeBackground) {
                                    DropdownMenuItem(
                                        text = { Text("Set as Home Background", fontSize = 13.sp, color = colors.textMain) },
                                        leadingIcon = { Icon(Lucide.Image, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                        onClick = { showMenu = false; onSetAsBackground() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Duplicate", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Copy, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onDuplicate() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to JSON", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Download, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onExportWork() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Set as Live Wallpaper", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Smartphone, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onSetAsLiveWallpaper() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share to Discover", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Globe, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onShareToDiscover() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Trash", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                                    leadingIcon = { Icon(Lucide.Trash2, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onSoftDelete() }
                                )
                            }
                        }
                    }
                }
                
                if (showDetails) {
                    if (work.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            work.description,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy â€¢ hh:mm a", java.util.Locale.getDefault()) }
                    val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        dateString,
                        color = colors.textMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        
        if (isListMode) {
            Row(modifier = Modifier.height(70.dp)) {
                Thumbnail()
                Info()
            }
        } else {
            Column {
                Thumbnail()
                Info()
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = work.name,
            initialDescription = work.description,
            onConfirm = { name, desc -> onRename(name, desc); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
}

// Keep the existing RenameDialog, EmptyState, and ChromaAnimatedBackground
@Composable
fun RenameDialog(
    initialName: String,
    initialDescription: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDescription) }
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    IosAlertDialog(
        title = "Edit Work Info",
        message = "Enter a title and optional description for this artwork canvas.",
        confirmText = "Save",
        onConfirm = { onConfirm(name.trim(), desc.trim()) },
        dismissText = "Cancel",
        onDismiss = onDismiss,
        customContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                        .border(0.5.dp, if (isDark) Color(0x22FFFFFF) else Color(0x1F000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (name.isEmpty()) {
                                Text("Title", color = Color(0xFF8E8E93), fontSize = 14.sp, fontFamily = InterFontFamily)
                            }
                            innerTextField()
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                        .border(0.5.dp, if (isDark) Color(0x22FFFFFF) else Color(0x1F000000), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        textStyle = TextStyle(
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily
                        ),
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (desc.isEmpty()) {
                                Text("Description (optional)", color = Color(0xFF8E8E93), fontSize = 13.sp, fontFamily = InterFontFamily)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun EmptyState(onNewWork: () -> Unit) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(colors.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Paintbrush, "Empty", tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No works yet",
            color = colors.textMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a new canvas to start designing.",
            color = colors.textMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))
        GlassButton(
            onClick = onNewWork,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.glossyBorder(RoundedCornerShape(12.dp), colors),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Create New Work", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ChromaAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    // Slow, organic movement - reduces per-frame GPU work vs faster speeds
    val t1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t1")
    val t2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t2")
    val t3 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t3")

    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current

    // Promote the animated background to its own RenderNode layer so its
    // redraws don't invalidate the rest of the UI composition tree.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(colors.bg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val b1x = w * (0.15f + 0.25f * t1)
            val b1y = h * (0.10f + 0.20f * t2)
            val b1r = minOf(w, h) * 0.55f
            drawCircle(Brush.radialGradient(listOf(Color(0x885E35CA), Color(0x00000000)), Offset(b1x, b1y), b1r), b1r, Offset(b1x, b1y))

            val b2x = w * (0.55f + 0.30f * t2)
            val b2y = h * (0.30f + 0.25f * t1)
            val b2r = minOf(w, h) * 0.50f
            drawCircle(Brush.radialGradient(listOf(Color(0x7700BCD4), Color(0x00000000)), Offset(b2x, b2y), b2r), b2r, Offset(b2x, b2y))

            val b3x = w * (0.30f + 0.20f * t3)
            val b3y = h * (0.65f + 0.20f * t2)
            val b3r = minOf(w, h) * 0.45f
            drawCircle(Brush.radialGradient(listOf(Color(0x66EC407A), Color(0x00000000)), Offset(b3x, b3y), b3r), b3r, Offset(b3x, b3y))
        }
        val overlayColor = if (colors.isDark) Color(0xDD000000) else Color(0xAAFFFFFF)
        Box(modifier = Modifier.fillMaxSize().background(overlayColor))
    }
}

const val DEFAULT_HOME_BG_JSON = """[
  {
    "activeBlobIdx": 0,
    "angle": 135.0,
    "animIntensity": 50.0,
    "animSpeed": 50.0,
    "animated": false,
    "blendMode": "NORMAL",
    "blobBgColor": -72057594037927936,
    "blobs": [
      {
        "feather": 100.0,
        "height": 40.0,
        "opacity": 1.0,
        "rotation": 0.0,
        "width": 40.0,
        "x": 25.0,
        "y": 40.0
      },
      {
        "feather": 100.0,
        "height": 40.0,
        "opacity": 1.0,
        "rotation": 0.0,
        "width": 40.0,
        "x": 75.0,
        "y": 60.0
      }
    ],
    "brightness": 50.0,
    "centerX": 50.0,
    "centerY": 50.0,
    "columns": 3,
    "complexity": 50.0,
    "expanded": true,
    "feather": 100.0,
    "hasBaseBackground": false,
    "height": 70.0,
    "id": "6e0c3f5d-cb01-4aeb-8877-8415e7a0b27d",
    "meshPoints": [
      {
        "x": 0.0,
        "y": 0.0
      },
      {
        "x": 100.0,
        "y": 100.0
      }
    ],
    "name": "Base",
    "opacity": 1.0,
    "repeatPattern": false,
    "rows": 3,
    "stops": [
      {
        "color": -70649712348233728,
        "id": "090d95d1-c68c-476d-bb21-2d0b46da000e",
        "position": 0.0
      },
      {
        "color": -72046169424920576,
        "id": "1bcbcd37-78a1-4c19-9aaa-ab465ca4bd95",
        "position": 100.0
      },
      {
        "color": -71866283309662208,
        "id": "46c10578-1ef2-4f29-90f8-d266368cdf65",
        "position": 50.81967
      },
      {
        "color": -71717527117365248,
        "id": "4df2c4b8-99ab-4058-8e1c-d9c304bac886",
        "position": 70.023415
      },
      {
        "color": -20847844269228032,
        "id": "e8654a9a-a2c2-480b-acc9-ca75e0a2bf62",
        "position": 25.526932
      },
      {
        "color": -71972506440826880,
        "id": "f5611c6c-d4d6-4d64-88f8-392f84ac08f0",
        "position": 37.93911
      }
    ],
    "type": "AURORA",
    "visible": true,
    "waveSpeed": 50.0,
    "width": 70.0
  }
]"""

const val GENERATE_BUTTON_BG_JSON = """[{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"NORMAL","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"8d6b1d6b-0f10-418d-b692-a24b1cf4bd4c","meshPoints":[{"x":25.0,"y":40.0},{"x":75.0,"y":60.0}],"name":"Mix 1","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-30162120800731136,"id":"32dfc511-7164-47ee-b9ba-b6222e6e3d7c","position":0.0},{"color":-54873820728655872,"id":"e0d2d1bf-369e-4971-b6d1-aa64622043ba","position":50.0},{"color":-38489628595978240,"id":"6df8758d-6b95-4422-8695-d58b2a393f36","position":100.0}],"type":"AURORA","visible":true,"waveSpeed":50.0,"width":70.0},{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"OVERLAY","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"3ebfc43c-55b6-4048-b9b1-523c9bfc9224","meshPoints":[{"x":25.0,"y":40.0},{"x":75.0,"y":60.0}],"name":"Mix 2","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-62539834840842240,"id":"c4af048b-7fb4-4077-85ea-f86b3ea51464","position":0.0},{"color":-47462137399869440,"id":"53c0a0c8-d5c3-4768-887d-c096f73cb321","position":50.0},{"color":-15062510436614144,"id":"f7686eb6-d4f6-44b9-9fa9-9dce22365bbc","position":100.0}],"type":"RADIAL","visible":true,"waveSpeed":50.0,"width":70.0}]"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostMenuSheet(
    onDismiss: () -> Unit,
    onCreateGradient: () -> Unit,
    onCreatePost: () -> Unit,
    onCreateFlash: () -> Unit,
    onCreateStory: () -> Unit,
    onCreateAIGenerate: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create New",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose what type of content to create or share",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Normal
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Cupertino Inset Grouped Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color.White)
                    .border(
                        0.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                CreateOptionRow(
                    icon = Lucide.Palette,
                    iconGradient = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)),
                    title = "New Gradient Canvas",
                    subtitle = "Design procedural mesh gradient artwork",
                    onClick = onCreateGradient
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )
                
                CreateOptionRow(
                    icon = Lucide.Image,
                    iconGradient = listOf(Color(0xFF5856D6), Color(0xFFAF52DE)),
                    title = "Community Post",
                    subtitle = "Share images or artwork with captions & tags",
                    onClick = onCreatePost
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )
                
                CreateOptionRow(
                    icon = Lucide.Film,
                    iconGradient = listOf(Color(0xFFFF9500), Color(0xFFFF3B30)),
                    title = "Flash / Reel Video",
                    subtitle = "Record or upload short-form video reels",
                    onClick = onCreateFlash
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )
                
                CreateOptionRow(
                    icon = Lucide.Type,
                    iconGradient = listOf(Color(0xFF34C759), Color(0xFF30B0C7)),
                    title = "Story / Text Status",
                    subtitle = "Publish formatted 24-hour typography status",
                    onClick = onCreateStory
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    thickness = 0.5.dp
                )
                
                CreateOptionRow(
                    icon = Lucide.Sparkles,
                    iconGradient = listOf(Color(0xFF0A84FF), Color(0xFF64D2FF)),
                    title = "AI Magic Generator",
                    subtitle = "Synthesize unique gradients via prompt AI",
                    badge = "AI",
                    onClick = onCreateAIGenerate
                )
            }
            
            Spacer(Modifier.height(18.dp))
            
            // Apple Action Sheet Cancel Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color.White)
                    .border(
                        0.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF007AFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CreateOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconGradient: List<Color>? = null,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Universal Signature Icon Capsule
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(11.dp),
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFF8FAFC))
                .background(
                    if (iconGradient != null) Brush.linearGradient(iconGradient, start = Offset.Zero, end = Offset.Infinite)
                    else DrawerIconGradient
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color.Black
                )
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF007AFF).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8E8E93),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(16.dp)
        )
    }
}
