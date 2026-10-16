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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.graphics.graphicsLayer
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

enum class ViewMode(val title: String, val subtitle: String) {
    GRID("Grid", "Two-column visual canvas cards"),
    LIST("List", "Compact rows with quick actions"),
    LIST_DETAILS("List with details", "Expanded rows with description & dates"),
    GRID_DETAILS("Grid with details", "Cards with artwork, title, and metadata");

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
    var showViewModeSheet by remember { mutableStateOf(false) }

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
            } else {
                ChromaAnimatedBackground()
            }
            Box(modifier = Modifier.fillMaxSize().then(if (showHeader) Modifier.statusBarsPadding() else Modifier)) {
                // ── Hero Section ──
                Column(modifier = Modifier.fillMaxWidth().height(if (showHeader) 420.dp else 340.dp).zIndex(1f).padding(horizontal = 20.dp, vertical = if (showHeader) 16.dp else 12.dp)) {
                    
                    val heroTextColor = Color.White
                    val heroMutedColor = Color.White.copy(alpha = 0.9f)
                    val heroIconBg = Color.White.copy(alpha = 0.15f)
                    val heroIconBorder = Color.White.copy(alpha = 0.4f)

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
                                tint = heroTextColor.copy(alpha = 0.9f),
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
                                        .border(1.dp, heroIconBorder, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .clickable { 
                                            importWorkLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Upload, "Import JSON File", tint = heroTextColor, modifier = Modifier.size(16.dp))
                                }
                                
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, heroIconBorder, CircleShape)
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
                                    Icon(Lucide.ClipboardPaste, "Paste JSON", tint = heroTextColor, modifier = Modifier.size(16.dp))
                                }
                                val haptic = LocalHapticFeedback.current
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, heroIconBorder, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                        .combinedClickable(
                                            onClick = { 
                                                viewMode = viewMode.next()
                                                onViewModeChanged(viewMode)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showViewModeSheet = true
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(viewMode.icon, "View Mode: ${viewMode.title}", tint = heroTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            if (searchActive) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = heroTextColor, fontSize = 16.sp),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(heroTextColor),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(heroIconBg)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    decorationBox = { innerTextField ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Lucide.Search, null, tint = heroTextColor, modifier = Modifier.size(18.dp))
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (searchQuery.isEmpty()) {
                                                    Text("Search works...", color = heroMutedColor, fontSize = 14.sp)
                                                }
                                                innerTextField()
                                            }
                                            Icon(Lucide.X, null, tint = heroTextColor, modifier = Modifier.size(18.dp).clickable { 
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
                                        .background(heroIconBg)
                                        .clickable { searchActive = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Search, "Search", tint = heroTextColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Text section
                    Text("Gradient create", color = heroMutedColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Get started", color = heroTextColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(heroIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.ChevronRight, null, tint = heroTextColor, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Button 1: New gradient
                        val cardBrush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.20f)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                        )
                        val cardTextColor = Color.White
                        val cardBorderColor = Color.White.copy(alpha = 0.4f)
                        
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBrush)
                                .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
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
                        AnimatedVisibility(
                            visible = selectionMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color(0x33007AFF), spotColor = Color(0x4D007AFF))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF007AFF), Color(0xFF0056D2))
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .clickable { onClearSelection() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.X,
                                            contentDescription = "Cancel",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${selectedWorkIds.size} Selected",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val allSelected = filteredWorks.isNotEmpty() && selectedWorkIds.size >= filteredWorks.size
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (allSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.20f))
                                            .clickable {
                                                if (allSelected) onClearSelection()
                                                else onUpdateSelection(filteredWorks.map { it.id }.toSet())
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.CheckCheck,
                                            contentDescription = if (allSelected) "Deselect All" else "Select All",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30))
                                            .clickable { onDeleteSelected() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Trash2,
                                            contentDescription = "Delete Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // â”€â”€ Work grid â”€â”€
                if (works.isEmpty()) {
                    EmptyState(
                        onNewWork = onNewWork,
                        onCreateClick = { showCreatePostMenu = true }
                    )
                } else if (filteredWorks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No works found for \"$searchQuery\"", color = colors.textMuted, fontSize = 15.sp)
                    }
                } else {
                    val haptic = LocalHapticFeedback.current

                    val gridColumns = when (viewMode) {
                        ViewMode.LIST, ViewMode.LIST_DETAILS -> GridCells.Fixed(1)
                        ViewMode.GRID, ViewMode.GRID_DETAILS -> GridCells.Fixed(2)
                    }

                    val currentSelectedIds by rememberUpdatedState(selectedWorkIds)
                    val currentFilteredWorks by rememberUpdatedState(filteredWorks)

                    LazyVerticalGrid(
                        columns = gridColumns,
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val item = gridState.layoutInfo.visibleItemsInfo.find {
                                            offset.x >= it.offset.x && offset.x <= it.offset.x + it.size.width &&
                                            offset.y >= it.offset.y && offset.y <= it.offset.y + it.size.height
                                        }
                                        if (item != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragAnchorIndex = item.index
                                            dragInitialSelectedIds = currentSelectedIds
                                            val work = currentFilteredWorks.getOrNull(item.index)
                                            if (work != null) {
                                                dragIsSelecting = !currentSelectedIds.contains(work.id)
                                                // Apply initial toggle
                                                val newSelection = if (dragIsSelecting) {
                                                    currentSelectedIds + work.id
                                                } else {
                                                    currentSelectedIds - work.id
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
                                                .mapNotNull { currentFilteredWorks.getOrNull(it)?.id }
                                                .toSet()

                                            val newSelection = if (dragIsSelecting) {
                                                dragInitialSelectedIds + currentDragSet
                                            } else {
                                                dragInitialSelectedIds - currentDragSet
                                            }
                                            
                                            if (newSelection != currentSelectedIds) {
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
                                onLongClick = { onToggleSelection(work.id) },
                                onRename = { name, desc -> onRenameWork(work.id, name, desc) },
                                onDuplicate = { onDuplicateWork(work) },
                                onSoftDelete = { onSoftDelete(work.id) },
                                onSetAsBackground = { onSetAsBackground(work) },
                                onSetAsLiveWallpaper = { onSetAsLiveWallpaper(work) },
                                onShareToDiscover = { onShareToDiscover(work.id) },
                                onExportWork = {
                                    workToExport = work
                                    exportWorkLauncher.launch("${work.name}.json")
                                },
                                onOpenViewMode = { showViewModeSheet = true }
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

    if (showViewModeSheet) {
        ViewModeBottomSheet(
            currentMode = viewMode,
            onSelectMode = { newMode ->
                viewMode = newMode
                onViewModeChanged(newMode)
            },
            onDismiss = { showViewModeSheet = false }
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
    onLongClick: () -> Unit = {},
    onRename: (String, String) -> Unit,
    onDuplicate: () -> Unit,
    onSoftDelete: () -> Unit,
    onSetAsBackground: () -> Unit,
    onSetAsLiveWallpaper: () -> Unit = {},
    onShareToDiscover: () -> Unit = {},
    onExportWork: () -> Unit = {},
    onOpenViewMode: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (isSelected) 0.94f else 1f)
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val borderColor = if (isSelected) colors.primary else Color.Transparent

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }
    val dateTimeString = remember(work.lastModifiedAt) { dateTimeFormat.format(java.util.Date(work.lastModifiedAt)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .background(colors.glassBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        @Composable
        fun ActionMenu() {
            if (!selectionMode) {
                Box {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "Options",
                        tint = colors.textMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { showMenu = true }
                            .padding(2.dp)
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .glossyBorder(RoundedCornerShape(12.dp), colors)
                            .background(colors.glassBg, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("View as...", fontSize = 13.sp, color = colors.textMain) },
                            leadingIcon = { Icon(viewMode.icon, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                            onClick = { showMenu = false; onOpenViewMode() }
                        )
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

        @Composable
        fun CanvasBox(modifier: Modifier = Modifier) {
            Box(
                modifier = modifier.background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }
                if (layers.isEmpty()) {
                    Text(
                        work.name.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 32.sp,
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
                            .background(colors.primary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Check, "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        when (viewMode) {
            ViewMode.GRID -> {
                Column {
                    CanvasBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = work.name,
                                color = colors.textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            ActionMenu()
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = dateString,
                            color = colors.textMuted.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                }
            }

            ViewMode.GRID_DETAILS -> {
                Column {
                    CanvasBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = work.name,
                                color = colors.textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            ActionMenu()
                        }
                        if (work.description.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = work.description,
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                fontFamily = InterFontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dateString,
                            color = colors.textMuted.copy(alpha = 0.70f),
                            fontSize = 10.5.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                }
            }

            ViewMode.LIST -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvasBox(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = work.name,
                            color = colors.textMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = dateString,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                    ActionMenu()
                }
            }

            ViewMode.LIST_DETAILS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvasBox(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = work.name,
                                color = colors.textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            ActionMenu()
                        }
                        if (work.description.isNotBlank()) {
                            Text(
                                text = work.description,
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = dateTimeString,
                            color = colors.textMuted.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                }
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

private const val EMPTY_STATE_PLUS_LAYER_JSON = """[{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"NORMAL","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.609756,"expanded":true,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"b770567f-6c0e-4184-b420-7e2bc05cda73","meshPoints":[{"packedValue":4744666964463242507},{"packedValue":4798022454124478464}],"name":"Layer 2","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-4445291151360,"id":"5b568c1e-1572-4937-bbec-d0e217918156","position":0.0},{"color":-53699048388952064,"id":"67b73e9b-ca3f-4ccf-b663-0329095d9f57","position":100.0},{"color":-15579774822907904,"id":"418f51d7-235d-4990-9dc6-c5ad11d80186","position":50.0},{"color":-5933282571059200,"id":"1ead79c1-5bb3-45d1-92cd-d0071488eaa3","position":70.84309},{"color":-42438949808898048,"id":"55066126-b643-4ae3-b00b-92130349607b","position":30.093676},{"color":-23568031741378560,"id":"4687e55a-c0d9-492c-ae01-1445945dd886","position":84.66042},{"color":-75466870358016,"id":"aba0afa5-0acd-4bf3-bb01-baf0c2703746","position":15.105386},{"color":-44412573180755968,"id":"92f555c9-d3de-4713-b76c-b4a62a6cac75","position":62.177986},{"color":-47518693529223168,"id":"f10f3e5d-034f-470c-a9e4-a98d155d18d3","position":39.34426},{"color":-106257490903040,"id":"4933870b-5fde-4934-a108-e07cc781df79","position":23.302109},{"color":-2301651499089920,"id":"40a29fd5-e96c-461d-9ce6-9b3a19eafd17","position":78.92272},{"color":-55370718380032,"id":"dd6210e3-344f-4b17-b880-284852a7ee9d","position":92.27166},{"color":-189115999977472,"id":"f2e66f12-96f5-4db7-9a70-104927900cb0","position":5.503513}],"type":"AURORA","visible":true,"waveSpeed":50.0,"width":70.0}]"""

@Composable
fun EmptyState(
    onNewWork: () -> Unit,
    onCreateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emptyStatePlusScale"
    )

    val auroraStops = remember {
        arrayOf(
            0.000000f to Color(0xFFFFFBF5),
            0.055035f to Color(0xFFFF5400),
            0.151054f to Color(0xFFFFBB5D),
            0.233021f to Color(0xFFFF9F5C),
            0.300937f to Color(0xFF693A00),
            0.393443f to Color(0xFF572E00),
            0.500000f to Color(0xFFC8A647),
            0.621780f to Color(0xFF623700),
            0.708431f to Color(0xFFEAEBB6),
            0.789227f to Color(0xFFF7D2A9),
            0.846604f to Color(0xFFAC4500),
            0.922717f to Color(0xFFFFCDA4),
            1.000000f to Color(0xFF413900)
        )
    }

    val auroraBrush = remember(auroraStops) {
        Brush.verticalGradient(colorStops = auroraStops)
    }

    val emptyStateLayers = remember {
        try {
            com.chroma.studio.data.chromaGson.fromJson<List<com.chroma.studio.model.GradientLayer>>(
                EMPTY_STATE_PLUS_LAYER_JSON,
                object : com.google.gson.reflect.TypeToken<List<com.chroma.studio.model.GradientLayer>>() {}.type
            ) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Plus Icon Button with Aurora Properties (70.dp Circle)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFFFF5400).copy(alpha = 0.45f),
                        spotColor = Color(0xFFC8A647).copy(alpha = 0.50f)
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCreateClick?.invoke() ?: onNewWork()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (emptyStateLayers.isNotEmpty()) {
                    CanvasPreview(
                        layers = emptyStateLayers,
                        shape = "circle",
                        borderColor = Color.Transparent,
                        animStatus = com.chroma.studio.model.AnimStatus.PLAYING,
                        animSpeed = 50f,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(auroraBrush))
                }

                // Luxury specular glass overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Crisp Plus Icon
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = "Create",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Heading: Create
            Text(
                text = "Create",
                color = colors.textMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreateClick?.invoke() ?: onNewWork()
                }
            )

            Spacer(Modifier.height(3.dp))

            // Paragraph: Create new Work/Post/Edit anything
            Text(
                text = "Create new Work/Post/Edit anything",
                color = colors.textMuted,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.1).sp
            )
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



const val DEFAULT_HOME_BG_JSON = """[{"activeBlobIdx":0,"angle":180.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"NORMAL","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"fbc5d9bb-7c37-4972-b5ef-0a12dbd7cef3","meshPoints":[{"x":25.0,"y":40.0},{"x":75.0,"y":60.0}],"name":"Layer 2","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-71883875495706624,"id":"08c521a7-8c80-48fd-8aee-eedea5ba10ef","position":0.0},{"color":-71923565288488960,"id":"106a8c59-a666-4f87-a0a6-7fa5fb93deb3","position":100.0}],"type":"LINEAR","visible":false,"waveSpeed":50.0,"width":70.0},{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"NORMAL","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"f76aa4c7-a600-48a3-b425-9e1fe8c9bbf6","meshPoints":[{"x":25.0,"y":40.0},{"x":86.56671,"y":19.477528}],"name":"Mix 1","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-71971290965082112,"id":"662cf022-18e4-4023-9c6b-044682b8d162","position":0.0},{"color":-49623223209295872,"id":"cc216b58-ba3b-4b40-8052-e99981670adb","position":50.0},{"color":-71972283102527488,"id":"b12b98fb-40bd-4b1e-a3f9-3fa4bc65e9fb","position":100.0}],"type":"AURORA","visible":true,"waveSpeed":50.0,"width":70.0}]"""

const val GENERATE_BUTTON_BG_JSON = """[{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"NORMAL","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"8d6b1d6b-0f10-418d-b692-a24b1cf4bd4c","meshPoints":[{"x":25.0,"y":40.0},{"x":75.0,"y":60.0}],"name":"Mix 1","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-32270670570192896,"id":"32dfc511-7164-47ee-b9ba-b6222e6e3d7c","position":0.0},{"color":-46085162294837248,"id":"e0d2d1bf-369e-4971-b6d1-aa64622043ba","position":50.0},{"color":-59921781690990592,"id":"6df8758d-6b95-4422-8695-d58b2a393f36","position":100.0}],"type":"AURORA","visible":true,"waveSpeed":50.0,"width":70.0},{"activeBlobIdx":0,"angle":90.0,"animIntensity":50.0,"animSpeed":50.0,"animated":false,"blendMode":"OVERLAY","blobBgColor":-72057594037927936,"blobs":[{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":25.0,"y":40.0},{"feather":100.0,"height":40.0,"opacity":1.0,"rotation":0.0,"width":40.0,"x":75.0,"y":60.0}],"brightness":50.0,"centerX":50.0,"centerY":50.0,"columns":3,"complexity":50.0,"expanded":false,"feather":100.0,"hasBaseBackground":false,"height":70.0,"id":"3ebfc43c-55b6-4048-b9b1-523c9bfc9224","meshPoints":[{"x":25.0,"y":40.0},{"x":75.0,"y":60.0}],"name":"Mix 2","opacity":1.0,"repeatPattern":false,"rows":3,"stops":[{"color":-5427193689669632,"id":"c4af048b-7fb4-4077-85ea-f86b3ea51464","position":0.0},{"color":-53438807730552832,"id":"53c0a0c8-d5c3-4768-887d-c096f73cb321","position":50.0},{"color":-63596465515134976,"id":"f7686eb6-d4f6-44b9-9fa9-9dce22365bbc","position":100.0}],"type":"RADIAL","visible":true,"waveSpeed":50.0,"width":70.0}]"""

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewModeBottomSheet(
    currentMode: ViewMode,
    onSelectMode: (ViewMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

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
                text = "View as",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = if (isDark) Color.White else Color.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose how your works and canvases are displayed",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily
            )

            Spacer(Modifier.height(20.dp))

            // Inset Grouped Container
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
                ViewMode.entries.forEachIndexed { index, mode ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectMode(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) colors.primary.copy(alpha = 0.15f)
                                        else if (isDark) Color.White.copy(alpha = 0.08f)
                                        else Color.Black.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.title,
                                    tint = if (isSelected) colors.primary else if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = mode.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    color = if (isSelected) colors.primary else if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = mode.subtitle,
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93),
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "Selected",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < ViewMode.entries.size - 1) {
                        HorizontalDivider(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 68.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Cancel Button
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
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
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
