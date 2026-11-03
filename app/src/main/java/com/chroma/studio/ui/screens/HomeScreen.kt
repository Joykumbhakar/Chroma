package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.GlassIconButton

import com.chroma.studio.ui.components.OscellionBrandLockup

import com.chroma.studio.ui.components.OscellionWebLogo

import com.chroma.studio.ui.components.GlassButton

import com.chroma.studio.ui.components.IosAlertDialog

import com.chroma.studio.ui.components.AppleDialog

import com.chroma.studio.ui.components.LiquidGlassSecondaryButton

import com.chroma.studio.ui.components.SynchronizedRotatingActionMenu

import com.chroma.studio.ui.components.ActionMenuType

import com.chroma.studio.ui.components.AiWaveSphereIcon

import androidx.compose.foundation.text.BasicTextField

import androidx.compose.ui.text.TextStyle

import com.chroma.studio.ui.theme.InterFontFamily

import com.chroma.studio.ui.theme.TheSignatureFontFamily

import androidx.compose.ui.text.style.TextAlign



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

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.platform.LocalDensity

import kotlin.math.roundToInt

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

import androidx.compose.ui.layout.layout

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

import com.chroma.studio.ui.components.AppleSwitch

import com.chroma.studio.ui.graphics3d.Apple3DGraphicId

import com.chroma.studio.ui.graphics3d.AppleSettingsSquircleIcon

import com.chroma.studio.ui.graphics3d.AppleSettingsSquircleVectorIcon

import com.chroma.studio.ui.components.AppleHomeGraphic

import com.chroma.studio.ui.components.AppleWorksGraphic

import com.chroma.studio.ui.components.AppleEmptyStateView

import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState

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

    label: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    val colors = LocalOscellionColors.current

    

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(12.dp))

            .background(if (selected) Color(0xFF0A84FF).copy(alpha = 0.08f) else Color.Transparent)

            .clickable { onClick() }

            .padding(horizontal = 10.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(14.dp)

    ) {

        AppleSettingsSquircleVectorIcon(

            icon = icon,

            size = 24.dp,

            iconTint = if (selected) Color(0xFF0A84FF) else colors.textMain.copy(alpha = 0.85f)

        )

        Text(

            text = label,

            color = if (selected) Color(0xFF0A84FF) else colors.textMain,

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

    isLoading: Boolean = false,

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

    onResetHomeBackground: (() -> Unit)? = null,

    onNavigateToDiscover: () -> Unit,

    onRequireLogin: () -> Unit,

    onSetAsLiveWallpaper: (ChromaWork) -> Unit,

    onShareToDiscover: (String) -> Unit = {},

    isVerified: Boolean,

    repository: WorkRepository,

    showHeader: Boolean = true,

    onNavigateToCreatePost: (() -> Unit)? = null,

    onNavigateToFlashPicker: (() -> Unit)? = null,

    onNavigateToCreateTextStatus: (() -> Unit)? = null,

    onNavigateToDrafts: () -> Unit = {}

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

    var backgroundWorkToConfirm by remember { mutableStateOf<ChromaWork?>(null) }

    var showResetBgConfirm by remember { mutableStateOf(false) }

    var workPendingDelete by remember { mutableStateOf<ChromaWork?>(null) }

    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }



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



    val handleActionSelected: (ActionMenuType) -> Unit = { actionType ->

        showCreatePostMenu = false

        when (actionType) {

            ActionMenuType.GRADIENTS -> {

                onNewWork()

            }

            ActionMenuType.POST -> {

                if (FirebaseAuth.getInstance().currentUser != null) {

                    onNavigateToCreatePost?.invoke()

                } else {

                    showLoginScreen = true

                }

            }

            ActionMenuType.VIDEO -> {

                if (FirebaseAuth.getInstance().currentUser != null) {

                    onNavigateToFlashPicker?.invoke()

                } else {

                    showLoginScreen = true

                }

            }

            ActionMenuType.STORY -> {

                if (FirebaseAuth.getInstance().currentUser != null) {

                    onNavigateToCreateTextStatus?.invoke()

                } else {

                    showLoginScreen = true

                }

            }

            ActionMenuType.AI_GENERATOR -> {

                if (FirebaseAuth.getInstance().currentUser != null) {

                    val intent = Intent(context, com.chroma.studio.MainActivity::class.java).apply {

                        putExtra("EXTRA_START_AI_GENERATOR", true)

                    }

                    context.startActivity(intent)

                } else {

                    showLoginScreen = true

                }

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

                    OscellionBrandLockup(

                        iconSize = 32.dp,

                        fontSize = 24.dp,

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



                    if (customHomeBgLayersJson != null) {

                        Spacer(Modifier.height(4.dp))

                        DrawerItem(

                            icon = Lucide.RotateCcw,

                            label = "Reset Background to Default",

                            selected = false

                        ) {

                            scope.launch { drawerState.close() }

                            showResetBgConfirm = true

                        }

                    }



                    Spacer(Modifier.height(4.dp))



                    DrawerItem(

                        icon = Lucide.FileText,

                        label = "Drafts",

                        selected = false

                    ) {

                        scope.launch { drawerState.close() }

                        onNavigateToDrafts()

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

                    

                    // Dark mode toggle (Clean transparent row with Apple squircle vector icon & Apple HIG switch)

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

                            AppleSettingsSquircleVectorIcon(

                                icon = if (isDarkMode) Lucide.Moon else Lucide.Sun,

                                size = 24.dp,

                                iconTint = Color(0xFF0A84FF)

                            )

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

                        AppleSwitch(

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

                        // Left side: Menu -> Oscellion Brand

                        AnimatedVisibility(

                            visible = !searchActive,

                            enter = fadeIn() + expandHorizontally(),

                            exit = fadeOut() + shrinkHorizontally()

                        ) {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(10.dp)

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

                                OscellionBrandLockup(

                                    iconSize = 22.dp,

                                    fontSize = 18.dp

                                )

                            }

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

                                .size(26.dp)

                                .clip(CircleShape)

                                .background(heroIconBg),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(Lucide.ChevronRight, null, tint = heroTextColor, modifier = Modifier.size(15.dp))

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

                                        AiWaveSphereIcon(modifier = Modifier.size(26.dp))

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

                                Text("Pro Magic", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)

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

                                            .clickable { showDeleteSelectedConfirm = true },

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

                if (isLoading && works.isEmpty()) {

                    HomeSkeletonGrid(viewMode = viewMode, isDarkMode = isDarkMode)

                } else if (works.isEmpty()) {

                    EmptyState(

                        isOpen = showCreatePostMenu,

                        onToggle = { showCreatePostMenu = !showCreatePostMenu },

                        onClose = { showCreatePostMenu = false },

                        onActionSelected = handleActionSelected

                    )

                } else if (filteredWorks.isEmpty()) {

                    val searchGraphicState = rememberAppleGraphicAnimationState()

                    AppleEmptyStateView(

                        graphic = {

                            AppleWorksGraphic(

                                modifier = Modifier.size(130.dp),

                                state = searchGraphicState,

                                enableClickToAnimate = true

                            )

                        },

                        title = "No Works Found",

                        subtitle = "No creations matched \"$searchQuery\". Try searching for another name or tag.",

                        modifier = Modifier

                            .fillMaxSize()

                            .padding(bottom = 135.dp)

                    )

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

                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 180.dp),

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

                                onSoftDelete = { workPendingDelete = work },

                                onSetAsBackground = { backgroundWorkToConfirm = work },

                                isCurrentHomeBg = (customHomeBgLayersJson != null && work.layersJson == customHomeBgLayersJson),

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

            

            // ── Synchronized Rotating Action Menu (Floating for non-empty state) ──

            if (works.isNotEmpty() && showCreatePostMenu) {

                Box(

                    modifier = Modifier

                        .fillMaxSize()

                        .pointerInput(Unit) {

                            detectTapGestures { showCreatePostMenu = false }

                        }

                )

                androidx.activity.compose.BackHandler {

                    showCreatePostMenu = false

                }

            }



            AnimatedVisibility(

                visible = works.isNotEmpty() && showCreatePostMenu,

                enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),

                exit = scaleOut(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),

                modifier = Modifier

                    .align(Alignment.BottomCenter)

                    .navigationBarsPadding()

                    .padding(bottom = 106.dp)

            ) {

                Box(contentAlignment = Alignment.Center) {

                    SynchronizedRotatingActionMenu(

                        isOpen = showCreatePostMenu,

                        onToggle = { showCreatePostMenu = !showCreatePostMenu },

                        onClose = { showCreatePostMenu = false },

                        onActionSelected = handleActionSelected

                    )

                }

            }

        }

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



    // Set Background Confirmation Dialog with Live Animated Preview

    if (backgroundWorkToConfirm != null) {

        val work = backgroundWorkToConfirm!!

        val previewLayers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }

        AppleDialog(

            title = "Set as Home Background?",

            message = "Apply \"${work.name}\" to your Home screen?",

            primaryButtonText = "Set Background",

            onPrimaryClick = {

                val target = work

                backgroundWorkToConfirm = null

                onSetAsBackground(target)

            },

            secondaryButtonText = "Cancel",

            onSecondaryClick = { backgroundWorkToConfirm = null },

            onDismiss = { backgroundWorkToConfirm = null },

            customContent = {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(136.dp)

                        .clip(RoundedCornerShape(14.dp))

                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))

                ) {

                    CanvasPreview(

                        layers = previewLayers,

                        shape = work.canvasShape,

                        borderColor = Color.Transparent,

                        colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,

                        postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,

                        animStatus = com.chroma.studio.model.AnimStatus.PLAYING,

                        animStyle = com.chroma.studio.model.AnimStyle.DRIFT,

                        animSpeed = 35f,

                        animAmount = 35f,

                        reactOffset = Offset.Zero,

                        textContent = "",

                        onTextContentChange = {},

                        blobDragOverrides = emptyMap(),

                        modifier = Modifier.fillMaxSize()

                    )

                }

            }

        )

    }



    // Reset to Default Confirmation Dialog with Live Animated Default Preview

    if (showResetBgConfirm) {

        val defaultLayers = remember { repository.deserializeLayers(DEFAULT_HOME_BG_JSON) }

        AppleDialog(

            title = "Reset to Default?",

            message = "Restore the original animated gradient background for your Home screen?",

            primaryButtonText = "Reset",

            primaryIsDestructive = true,

            onPrimaryClick = {

                showResetBgConfirm = false

                onResetHomeBackground?.invoke()

                com.chroma.studio.chat.sound.SoundManager.playRefresh(context)

            },

            secondaryButtonText = "Cancel",

            onSecondaryClick = { showResetBgConfirm = false },

            onDismiss = { showResetBgConfirm = false },

            customContent = {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(136.dp)

                        .clip(RoundedCornerShape(14.dp))

                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))

                ) {

                    CanvasPreview(

                        layers = defaultLayers,

                        shape = "full",

                        borderColor = Color.Transparent,

                        colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,

                        postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,

                        animStatus = com.chroma.studio.model.AnimStatus.PLAYING,

                        animStyle = com.chroma.studio.model.AnimStyle.DRIFT,

                        animSpeed = 35f,

                        animAmount = 35f,

                        reactOffset = Offset.Zero,

                        textContent = "",

                        onTextContentChange = {},

                        blobDragOverrides = emptyMap(),

                        modifier = Modifier.fillMaxSize()

                    )

                }

            }

        )

    }



    // Move Single Work to Trash Confirmation Dialog (Apple HIG Style)

    if (workPendingDelete != null) {

        val work = workPendingDelete!!

        IosAlertDialog(

            title = "Move to Trash?",

            message = "Are you sure you want to move \"${work.name.ifBlank { "Untitled" }}\" to Trash?",

            confirmText = "Move to Trash",

            isDestructive = true,

            onConfirm = {

                val toDelete = work

                workPendingDelete = null

                onSoftDelete(toDelete.id)

            },

            dismissText = "Cancel",

            onDismiss = { workPendingDelete = null }

        )

    }



    // Move Multiple Works to Trash Confirmation Dialog (Apple HIG Style)

    if (showDeleteSelectedConfirm) {

        val count = selectedWorkIds.size

        IosAlertDialog(

            title = "Move to Trash?",

            message = "Are you sure you want to move $count ${if (count == 1) "work" else "works"} to Trash?",

            confirmText = "Move to Trash",

            isDestructive = true,

            onConfirm = {

                showDeleteSelectedConfirm = false

                onDeleteSelected()

            },

            dismissText = "Cancel",

            onDismiss = { showDeleteSelectedConfirm = false }

        )

    }

}



@Composable

fun HomeSkeletonGrid(

    viewMode: ViewMode,

    isDarkMode: Boolean

) {

    val transition = rememberInfiniteTransition(label = "home_skeleton")

    val translateAnim by transition.animateFloat(

        initialValue = -400f,

        targetValue = 1600f,

        animationSpec = infiniteRepeatable(

            animation = tween(durationMillis = 1300, easing = LinearEasing),

            repeatMode = RepeatMode.Restart

        ),

        label = "shimmer_offset"

    )



    val shimmerColors = if (isDarkMode) {

        listOf(

            Color(0xFF22232C).copy(alpha = 0.5f),

            Color(0xFF383A49).copy(alpha = 0.9f),

            Color(0xFF22232C).copy(alpha = 0.5f)

        )

    } else {

        listOf(

            Color(0xFFE2E8F0).copy(alpha = 0.6f),

            Color(0xFFF8FAFC).copy(alpha = 0.95f),

            Color(0xFFE2E8F0).copy(alpha = 0.6f)

        )

    }



    val shimmerBrush = Brush.linearGradient(

        colors = shimmerColors,

        start = Offset(translateAnim - 400f, translateAnim - 400f),

        end = Offset(translateAnim, translateAnim)

    )



    val gridColumns = when (viewMode) {

        ViewMode.LIST, ViewMode.LIST_DETAILS -> GridCells.Fixed(1)

        ViewMode.GRID, ViewMode.GRID_DETAILS -> GridCells.Fixed(2)

    }



    val itemCount = when (viewMode) {

        ViewMode.LIST, ViewMode.LIST_DETAILS -> 7

        ViewMode.GRID, ViewMode.GRID_DETAILS -> 6

    }



    LazyVerticalGrid(

        columns = gridColumns,

        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 180.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp),

        horizontalArrangement = Arrangement.spacedBy(12.dp),

        modifier = Modifier.fillMaxSize(),

        userScrollEnabled = false

    ) {

        items(itemCount) {

            SkeletonWorkCard(

                viewMode = viewMode,

                shimmerBrush = shimmerBrush

            )

        }

    }

}



@Composable

fun SkeletonWorkCard(

    viewMode: ViewMode,

    shimmerBrush: Brush

) {

    val colors = LocalOscellionColors.current



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .shadow(

                elevation = 2.dp,

                shape = RoundedCornerShape(18.dp),

                ambientColor = Color(0x10000000),

                spotColor = Color(0x18000000)

            )

            .clip(RoundedCornerShape(18.dp))

            .glossyBorder(RoundedCornerShape(18.dp), colors)

            .background(colors.glassBg)

    ) {

        when (viewMode) {

            ViewMode.GRID -> {

                Column {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .aspectRatio(1.22f)

                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))

                            .background(shimmerBrush)

                    )

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 12.dp, vertical = 10.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth(0.68f)

                                    .height(15.dp)

                                    .clip(RoundedCornerShape(4.dp))

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .size(24.dp)

                                    .clip(CircleShape)

                                    .background(shimmerBrush)

                            )

                        }

                        Spacer(Modifier.height(7.dp))

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(5.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .size(11.dp)

                                    .clip(CircleShape)

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .width(64.dp)

                                    .height(10.dp)

                                    .clip(RoundedCornerShape(3.dp))

                                    .background(shimmerBrush)

                            )

                        }

                    }

                }

            }



            ViewMode.GRID_DETAILS -> {

                Column {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .aspectRatio(1.22f)

                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))

                            .background(shimmerBrush)

                    )

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 12.dp, vertical = 10.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth(0.68f)

                                    .height(15.dp)

                                    .clip(RoundedCornerShape(4.dp))

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .size(24.dp)

                                    .clip(CircleShape)

                                    .background(shimmerBrush)

                            )

                        }

                        Spacer(Modifier.height(6.dp))

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(0.92f)

                                .height(11.dp)

                                .clip(RoundedCornerShape(3.dp))

                                .background(shimmerBrush)

                        )

                        Spacer(Modifier.height(4.dp))

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(0.55f)

                                .height(11.dp)

                                .clip(RoundedCornerShape(3.dp))

                                .background(shimmerBrush)

                        )

                        Spacer(Modifier.height(8.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {

                            Box(

                                modifier = Modifier

                                    .width(44.dp)

                                    .height(16.dp)

                                    .clip(RoundedCornerShape(6.dp))

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .width(55.dp)

                                    .height(10.dp)

                                    .clip(RoundedCornerShape(3.dp))

                                    .background(shimmerBrush)

                            )

                        }

                    }

                }

            }



            ViewMode.LIST -> {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 12.dp, vertical = 8.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Box(

                        modifier = Modifier

                            .size(56.dp)

                            .clip(RoundedCornerShape(13.dp))

                            .background(shimmerBrush)

                    )

                    Spacer(Modifier.width(12.dp))

                    Column(

                        modifier = Modifier.weight(1f),

                        verticalArrangement = Arrangement.Center

                    ) {

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(0.58f)

                                .height(15.dp)

                                .clip(RoundedCornerShape(4.dp))

                                .background(shimmerBrush)

                        )

                        Spacer(Modifier.height(6.dp))

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(0.38f)

                                .height(11.dp)

                                .clip(RoundedCornerShape(3.dp))

                                .background(shimmerBrush)

                        )

                    }

                    Box(

                        modifier = Modifier

                            .size(24.dp)

                            .clip(CircleShape)

                            .background(shimmerBrush)

                    )

                }

            }



            ViewMode.LIST_DETAILS -> {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 12.dp, vertical = 10.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Box(

                        modifier = Modifier

                            .size(76.dp)

                            .clip(RoundedCornerShape(15.dp))

                            .background(shimmerBrush)

                    )

                    Spacer(Modifier.width(12.dp))

                    Column(

                        modifier = Modifier.weight(1f),

                        verticalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Box(

                                modifier = Modifier

                                    .fillMaxWidth(0.6f)

                                    .height(15.dp)

                                    .clip(RoundedCornerShape(4.dp))

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .size(24.dp)

                                    .clip(CircleShape)

                                    .background(shimmerBrush)

                            )

                        }

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(0.85f)

                                .height(11.dp)

                                .clip(RoundedCornerShape(3.dp))

                                .background(shimmerBrush)

                        )

                        Spacer(Modifier.height(2.dp))

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .width(42.dp)

                                    .height(15.dp)

                                    .clip(RoundedCornerShape(5.dp))

                                    .background(shimmerBrush)

                            )

                            Box(

                                modifier = Modifier

                                    .width(60.dp)

                                    .height(10.dp)

                                    .clip(RoundedCornerShape(3.dp))

                                    .background(shimmerBrush)

                            )

                        }

                    }

                }

            }

        }

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

    isCurrentHomeBg: Boolean = false,

    onSetAsLiveWallpaper: () -> Unit = {},

    onShareToDiscover: () -> Unit = {},

    onExportWork: () -> Unit = {},

    onOpenViewMode: () -> Unit = {}

) {

    val colors = LocalOscellionColors.current

    var showMenu by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }



    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(if (isSelected) 0.94f else 1f, tween(200), label = "cardScale")

    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    val borderColor = if (isSelected) colors.primary else Color.Transparent



    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }

    val dateTimeString = remember(work.lastModifiedAt) { dateTimeFormat.format(java.util.Date(work.lastModifiedAt)) }

    val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .scale(scale)

            .shadow(

                elevation = if (isSelected) 8.dp else 2.5.dp,

                shape = RoundedCornerShape(18.dp),

                ambientColor = Color(0x14000000),

                spotColor = Color(0x1F000000)

            )

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

                    Box(

                        modifier = Modifier

                            .size(28.dp)

                            .clip(CircleShape)

                            .background(colors.glassBgHover.copy(alpha = 0.65f))

                            .border(0.5.dp, colors.glassBorder, CircleShape)

                            .clickable { showMenu = true },

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(

                            imageVector = Lucide.EllipsisVertical,

                            contentDescription = "Options",

                            tint = colors.textMuted,

                            modifier = Modifier.size(15.dp)

                        )

                    }

                    DropdownMenu(

                        expanded = showMenu,

                        onDismissRequest = { showMenu = false },

                        modifier = Modifier

                            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.15f))

                            .clip(RoundedCornerShape(24.dp))

                            .glossyBorder(RoundedCornerShape(24.dp), colors)

                            .background(colors.glassBg, RoundedCornerShape(24.dp))

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

                                text = {

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                                    ) {

                                        Text(

                                            if (isCurrentHomeBg) "Home Background" else "Set as Home Background",

                                            fontSize = 13.sp,

                                            color = colors.textMain

                                        )

                                        if (isCurrentHomeBg) {

                                            Text(

                                                "(Active)",

                                                fontSize = 11.sp,

                                                color = colors.primary,

                                                fontWeight = FontWeight.Bold

                                            )

                                        }

                                    }

                                },

                                leadingIcon = {

                                    Icon(

                                        if (isCurrentHomeBg) Lucide.Check else Lucide.Image,

                                        null,

                                        tint = if (isCurrentHomeBg) colors.primary else colors.textMain,

                                        modifier = Modifier.size(15.dp)

                                    )

                                },

                                onClick = {

                                    showMenu = false

                                    if (isCurrentHomeBg) {

                                        com.chroma.studio.ui.components.ToastManager.showToast("\"${work.name}\" is already your active background")

                                    } else {

                                        onSetAsBackground()

                                    }

                                }

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

                modifier = modifier.background(

                    Brush.verticalGradient(

                        listOf(Color(0xFF1E1E26), Color(0xFF131317))

                    )

                ),

                contentAlignment = Alignment.Center

            ) {

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



                // Top-right Wallpaper badge

                if (work.isHomeBackground) {

                    Row(

                        modifier = Modifier

                            .align(Alignment.TopEnd)

                            .padding(8.dp)

                            .clip(RoundedCornerShape(8.dp))

                            .background(Color.Black.copy(alpha = 0.55f))

                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))

                            .padding(horizontal = 6.dp, vertical = 3.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        AiWaveSphereIcon(

                            modifier = Modifier.size(12.dp)

                        )

                        Text(

                            text = "Wallpaper",

                            color = Color.White,

                            fontSize = 9.5.sp,

                            fontWeight = FontWeight.Bold

                        )

                    }

                }



                // Bottom-right Layer count pill

                if (layers.isNotEmpty()) {

                    Row(

                        modifier = Modifier

                            .align(Alignment.BottomEnd)

                            .padding(7.dp)

                            .clip(RoundedCornerShape(6.dp))

                            .background(Color.Black.copy(alpha = 0.45f))

                            .padding(horizontal = 5.dp, vertical = 2.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(3.dp)

                    ) {

                        Icon(

                            imageVector = Lucide.Layers,

                            contentDescription = null,

                            tint = Color.White.copy(alpha = 0.85f),

                            modifier = Modifier.size(9.dp)

                        )

                        Text(

                            text = "${layers.size}",

                            color = Color.White,

                            fontSize = 9.sp,

                            fontWeight = FontWeight.SemiBold

                        )

                    }

                }



                // Selection check indicator

                if (selectionMode) {

                    Box(

                        modifier = Modifier

                            .align(Alignment.TopStart)

                            .padding(8.dp)

                    ) {

                        if (isSelected) {

                            Box(

                                modifier = Modifier

                                    .size(26.dp)

                                    .background(colors.primary, CircleShape)

                                    .border(1.5.dp, Color.White, CircleShape),

                                contentAlignment = Alignment.Center

                            ) {

                                Icon(

                                    imageVector = Lucide.Check,

                                    contentDescription = "Selected",

                                    tint = Color.White,

                                    modifier = Modifier.size(15.dp)

                                )

                            }

                        } else {

                            Box(

                                modifier = Modifier

                                    .size(26.dp)

                                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)

                                    .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)

                            )

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

                            .padding(horizontal = 12.dp, vertical = 10.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                text = work.name,

                                color = colors.textMain,

                                fontSize = 14.5.sp,

                                fontWeight = FontWeight.Bold,

                                fontFamily = InterFontFamily,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis,

                                modifier = Modifier.weight(1f)

                            )

                            Spacer(Modifier.width(4.dp))

                            ActionMenu()

                        }

                        Spacer(Modifier.height(3.dp))

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(4.dp)

                        ) {

                            Icon(

                                imageVector = Lucide.Clock,

                                contentDescription = null,

                                tint = colors.textMuted.copy(alpha = 0.65f),

                                modifier = Modifier.size(10.5.dp)

                            )

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

                            .padding(horizontal = 12.dp, vertical = 10.dp)

                    ) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                text = work.name,

                                color = colors.textMain,

                                fontSize = 14.5.sp,

                                fontWeight = FontWeight.Bold,

                                fontFamily = InterFontFamily,

                                maxLines = 1,

                                overflow = TextOverflow.Ellipsis,

                                modifier = Modifier.weight(1f)

                            )

                            Spacer(Modifier.width(4.dp))

                            ActionMenu()

                        }

                        if (work.description.isNotBlank()) {

                            Spacer(Modifier.height(4.dp))

                            Text(

                                text = work.description,

                                color = colors.textMuted,

                                fontSize = 11.5.sp,

                                fontFamily = InterFontFamily,

                                maxLines = 2,

                                lineHeight = 15.sp,

                                overflow = TextOverflow.Ellipsis

                            )

                        }

                        Spacer(Modifier.height(6.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {

                            Box(

                                modifier = Modifier

                                    .clip(RoundedCornerShape(9999.dp))

                                    .background(colors.glassBgHover.copy(alpha = 0.7f))

                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(9999.dp))

                                    .padding(horizontal = 8.dp, vertical = 2.5.dp)

                            ) {

                                Text(

                                    text = work.canvasShape.replaceFirstChar { it.uppercase() },

                                    color = colors.textMuted,

                                    fontSize = 9.5.sp,

                                    fontWeight = FontWeight.Medium,

                                    fontFamily = InterFontFamily

                                )

                            }

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(3.dp)

                            ) {

                                Icon(

                                    imageVector = Lucide.Clock,

                                    contentDescription = null,

                                    tint = colors.textMuted.copy(alpha = 0.65f),

                                    modifier = Modifier.size(10.dp)

                                )

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

                }

            }



            ViewMode.LIST -> {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 12.dp, vertical = 8.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    CanvasBox(

                        modifier = Modifier

                            .size(56.dp)

                            .clip(RoundedCornerShape(13.dp))

                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(13.dp))

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

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Text(

                                text = "${layers.size} ${if (layers.size == 1) "layer" else "layers"} • $dateString",

                                color = colors.textMuted,

                                fontSize = 11.5.sp,

                                fontFamily = InterFontFamily,

                                maxLines = 1

                            )

                            if (work.isHomeBackground) {

                                Box(

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(4.dp))

                                        .background(Color(0xFFFFD60A).copy(alpha = 0.15f))

                                        .padding(horizontal = 4.dp, vertical = 1.dp)

                                ) {

                                    Text(

                                        text = "Wallpaper",

                                        color = Color(0xFFD97706),

                                        fontSize = 9.sp,

                                        fontWeight = FontWeight.Bold

                                    )

                                }

                            }

                        }

                    }

                    ActionMenu()

                }

            }



            ViewMode.LIST_DETAILS -> {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 12.dp, vertical = 10.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    CanvasBox(

                        modifier = Modifier

                            .size(76.dp)

                            .clip(RoundedCornerShape(15.dp))

                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(15.dp))

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

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .clip(RoundedCornerShape(5.dp))

                                    .background(colors.glassBgHover.copy(alpha = 0.7f))

                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(5.dp))

                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)

                            ) {

                                Text(

                                    text = work.canvasShape.replaceFirstChar { it.uppercase() },

                                    color = colors.textMuted,

                                    fontSize = 9.5.sp,

                                    fontWeight = FontWeight.Medium,

                                    fontFamily = InterFontFamily

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

fun EmptyState(

    isOpen: Boolean = false,

    onToggle: () -> Unit = {},

    onClose: () -> Unit = {},

    onActionSelected: (ActionMenuType) -> Unit = {},

    modifier: Modifier = Modifier

) {

    val colors = LocalOscellionColors.current

    if (isOpen) {

        androidx.activity.compose.BackHandler {

            onClose()

        }

    }

    Box(

        modifier = modifier.fillMaxSize(),

        contentAlignment = Alignment.Center

    ) {

        if (isOpen) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .pointerInput(Unit) {

                        detectTapGestures { onClose() }

                    }

            )

        }

        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(bottom = 135.dp)

                .padding(horizontal = 32.dp, vertical = 24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ) {

        // Center Plus Button with Synchronized Rotating Action Menu

        Box(

            modifier = Modifier

                .layout { measurable, constraints ->

                    val placeable = measurable.measure(constraints)

                    val reportedHeight = (placeable.height / 2) + 28.dp.roundToPx()

                    layout(placeable.width, reportedHeight) {

                        placeable.placeRelative(0, 0)

                    }

                },

            contentAlignment = Alignment.Center

        ) {

            SynchronizedRotatingActionMenu(

                isOpen = isOpen,

                onToggle = onToggle,

                onClose = onClose,

                onActionSelected = onActionSelected

            )

        }



        Spacer(Modifier.height(2.dp))



        // "Create" text with signature handwritten font style

        Text(

            text = "Create",

            color = colors.textMain,

            fontFamily = TheSignatureFontFamily,

            fontWeight = FontWeight.Bold,

            fontSize = 46.sp,

            textAlign = TextAlign.Center,

            modifier = Modifier.clickable(

                interactionSource = remember { MutableInteractionSource() },

                indication = null

            ) {

                onToggle()

            }

        )



        Spacer(Modifier.height(2.dp))



        Text(

            text = "Create your first canvas or generative wallpaper to get started in your studio.",

            color = colors.textMuted,

            fontSize = 13.sp,

            textAlign = TextAlign.Center,

            lineHeight = 18.sp,

            modifier = Modifier.padding(horizontal = 16.dp)

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

            LiquidGlassSecondaryButton(

                text = "Cancel",

                onClick = onDismiss,

                modifier = Modifier

                    .fillMaxWidth()

                    .height(52.dp)

            )



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

