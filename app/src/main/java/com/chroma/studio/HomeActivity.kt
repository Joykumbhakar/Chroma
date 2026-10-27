package com.chroma.studio
import com.chroma.studio.ui.components.OscellionLoadingSpinner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.FlashMediaType
import com.chroma.studio.ui.components.isVideoPost
import com.chroma.studio.ui.components.extractVideoUrlFromPost
import com.chroma.studio.ui.screens.HomeScreen
import com.chroma.studio.ui.screens.FlashFeedScreen
import com.chroma.studio.ui.screens.FlashPickerScreen
import com.chroma.studio.ui.screens.FlashPhotoEditorScreen
import com.chroma.studio.ui.screens.FlashPostScreen
import com.chroma.studio.ui.theme.OscellionTheme
import com.chroma.studio.viewmodel.HomeViewModel
import com.chroma.studio.viewmodel.FlashViewModel

import android.content.Context
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.ExperimentalFoundationApi
class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()
    private val flashVm: FlashViewModel by viewModels()
    private val commentVm: com.chroma.studio.viewmodel.FlashCommentViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response if needed
    }

    private fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startChatNotificationListener() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("chats")
            .whereArrayContains("participants", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        val room = change.document.toObject(com.chroma.studio.model.ChatRoom::class.java)
                        if (room.unreadCount[currentUser.uid] ?: 0 > 0) {
                            val otherUserId = room.participants.firstOrNull { it != currentUser.uid }
                            val senderName = if (room.isGroup) (room.name ?: "Group") else "New Message"
                            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            val channelId = "chat_notifications"
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val channel = android.app.NotificationChannel(channelId, "Chats", android.app.NotificationManager.IMPORTANCE_HIGH)
                                notificationManager.createNotificationChannel(channel)
                            }

                            val notificationId = room.id.hashCode()
                                val intent = android.content.Intent(this, HomeActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                val pendingIntent = android.app.PendingIntent.getActivity(
                                    this, notificationId, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                )

                                val remoteInput = androidx.core.app.RemoteInput.Builder("KEY_TEXT_REPLY").setLabel("Reply").build()
                                val replyIntent = android.content.Intent(this, ChatNotificationReceiver::class.java).apply {
                                    action = "ACTION_REPLY"
                                    putExtra("room_id", room.id)
                                    putExtra("notification_id", notificationId)
                                }
                                val replyPendingIntent = android.app.PendingIntent.getBroadcast(
                                    this, notificationId, replyIntent, android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                val actionReply = androidx.core.app.NotificationCompat.Action.Builder(
                                    0, "Reply", replyPendingIntent
                                ).addRemoteInput(remoteInput).build()

                                val readIntent = android.content.Intent(this, ChatNotificationReceiver::class.java).apply {
                                    action = "ACTION_MARK_READ"
                                    putExtra("room_id", room.id)
                                    putExtra("notification_id", notificationId)
                                }
                                val readPendingIntent = android.app.PendingIntent.getBroadcast(
                                    this, notificationId + 1, readIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                val actionRead = androidx.core.app.NotificationCompat.Action.Builder(
                                    0, "Mark as read", readPendingIntent
                                ).build()

                                val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
                                    .setSmallIcon(R.drawable.ic_oscellion_notification)
                                    .setContentTitle(senderName)
                                    .setContentText(room.lastMessage)
                                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .addAction(actionReply)
                                    .addAction(actionRead)

                                notificationManager.notify(notificationId, builder.build())
                        }
                    }
                }
            }
    }

    fun enterPictureInPicture(aspectRatioNumerator: Int = 9, aspectRatioDenominator: Int = 16) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(aspectRatioNumerator, aspectRatioDenominator))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            splashView.setBackgroundResource(R.drawable.splash_bg_gradient)
            val alphaAnim = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.ALPHA, 1f, 0f)
            val scaleXAnim = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.SCALE_X, 1f, 1.04f)
            val scaleYAnim = android.animation.ObjectAnimator.ofFloat(splashView, android.view.View.SCALE_Y, 1f, 1.04f)

            val animatorSet = android.animation.AnimatorSet().apply {
                playTogether(alphaAnim, scaleXAnim, scaleYAnim)
                duration = 320L
                interpolator = android.view.animation.PathInterpolator(0.16f, 1f, 0.3f, 1f)
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                    }
                })
            }
            animatorSet.start()
        }
        super.onCreate(savedInstanceState)
        com.chroma.studio.chat.sound.SoundManager.init(this)
        askNotificationPermission()
        startChatNotificationListener()
        com.chroma.studio.ChromaMessagingService.syncTokenForCurrentUser()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        val hasAcceptedTerms = prefs.getBoolean("has_accepted_terms", false)

        val isDarkModePref = prefs.getBoolean("is_dark_mode", false)
        val viewModePref = prefs.getString("view_mode", "GRID") ?: "GRID"
        val initialViewMode = try { com.chroma.studio.ui.screens.ViewMode.valueOf(viewModePref) } catch(e: Exception) { com.chroma.studio.ui.screens.ViewMode.GRID }
        vm.updateDarkMode(isDarkModePref)

        setContent {
            OscellionTheme(darkTheme = vm.isDarkMode) {
            com.chroma.studio.ui.components.OscellionBackground {


                // Refresh works list every time this screen is shown
                LaunchedEffect(Unit) { vm.refresh() }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
                val fullRoute = navBackStackEntry?.destination?.route ?: ""

                val isMain = currentRoute == "main"
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0, pageCount = { 5 })

                val handleLogout: () -> Unit = {
                    // Optimistic instant UI transition directly to Login screen (never blank)
                    prefs.edit().putBoolean("is_first_launch", true).apply()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        } catch (e: Exception) {
                            android.util.Log.e("HomeActivity", "Error signing out", e)
                        }
                    }
                }
                var isInsideChatTab by remember { mutableStateOf(false) }
                var isDiscoverStoryViewerActive by remember { mutableStateOf(false) }
                var isProfileStoryViewerActive by remember { mutableStateOf(false) }
                val hideBottomNavRoutes = setOf(
                    "chat", "group_info", "create_group",
                    "story_editor", "create_text_status", "story_viewer", "story",
                    "settings", "profile_identity", "security", "privacy",
                    "notifications", "notifications_settings", "notifications_feed", "archives",
                    "activity_history", "trash", "appearance_settings",
                    "backgrounds",
                    "flash_feed", "flash_picker", "flash_video_editor", "flash_photo_editor", "flash_post",
                    "create_post", "publish_work", "post_details", "creator_studio",
                    "drafts_list",
                    "terms_agreement", "privacy_terms", "login", "post_onboarding"
                )
                val isChatTabActive = isMain && pagerState.currentPage == 3
                val shouldHideOnChatTab = isChatTabActive && isInsideChatTab
                val isRouteHidingBottomNav = currentRoute == null ||
                    hideBottomNavRoutes.contains(currentRoute) ||
                    currentRoute.startsWith("creator_studio") ||
                    fullRoute.startsWith("creator_studio") ||
                    currentRoute.startsWith("chat") ||
                    fullRoute.startsWith("chat") ||
                    currentRoute.startsWith("story") ||
                    fullRoute.startsWith("story") ||
                    currentRoute.startsWith("group_info") ||
                    fullRoute.startsWith("group_info") ||
                    currentRoute.startsWith("settings") ||
                    fullRoute.startsWith("settings") ||
                    currentRoute.startsWith("appearance") ||
                    fullRoute.startsWith("appearance") ||
                    currentRoute.startsWith("profile_identity") ||
                    fullRoute.startsWith("profile_identity") ||
                    currentRoute.startsWith("security") ||
                    fullRoute.startsWith("security") ||
                    currentRoute.startsWith("privacy") ||
                    fullRoute.startsWith("privacy") ||
                    currentRoute.startsWith("notifications") ||
                    fullRoute.startsWith("notifications") ||
                    currentRoute.startsWith("archives") ||
                    fullRoute.startsWith("archives") ||
                    currentRoute.startsWith("activity_history") ||
                    fullRoute.startsWith("activity_history") ||
                    currentRoute.startsWith("trash") ||
                    fullRoute.startsWith("trash") ||
                    currentRoute.startsWith("backgrounds") ||
                    fullRoute.startsWith("backgrounds") ||
                    currentRoute.startsWith("flash") ||
                    fullRoute.startsWith("flash") ||
                    currentRoute.startsWith("create_") ||
                    fullRoute.startsWith("create_") ||
                    currentRoute.startsWith("publish_work") ||
                    fullRoute.startsWith("publish_work") ||
                    currentRoute.startsWith("post_details") ||
                    fullRoute.startsWith("post_details") ||
                    currentRoute.startsWith("login") ||
                    fullRoute.startsWith("login") ||
                    currentRoute.startsWith("terms") ||
                    fullRoute.startsWith("terms")

                val showBottomNav = (
                    (isMain && !shouldHideOnChatTab && !isDiscoverStoryViewerActive && !isProfileStoryViewerActive) ||
                    (currentRoute == "userProfile" && !isRouteHidingBottomNav && !isProfileStoryViewerActive) ||
                    (currentRoute == "user_list" && !isRouteHidingBottomNav)
                ) && !isRouteHidingBottomNav
                val chatVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.ChatViewModel>()
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                val hazeState = remember { dev.chrisbanes.haze.HazeState() }

                // ── Flash editor pass-through state ────────────────────────
                var flashPickerVideoUri by remember { mutableStateOf<Uri?>(null) }
                var flashPickerPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var flashImportedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var flashTrimStart by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                var flashTrimEnd by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                var flashFilterName by remember { mutableStateOf("None") }
                var flashBrightness by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                var flashContrast by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
                var flashSaturation by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
                var flashHue by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                var flashThumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                var flashMediaType by remember { mutableStateOf(FlashMediaType.VIDEO) }
                var flashDurationPerSlide by remember { androidx.compose.runtime.mutableLongStateOf(3000L) }
                var flashRemixPostId by remember { mutableStateOf<String?>(null) }
                var flashRemixAuthorUsername by remember { mutableStateOf<String?>(null) }

                val onRemixCommunityPost: (DiscoverPost) -> Unit = { work ->
                    val videoUrl = extractVideoUrlFromPost(work)
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (!currentUid.isNullOrBlank()) {
                        coroutineScope.launch {
                            val repo = com.chroma.studio.data.DiscoverRepository()
                            repo.incrementRemixCount(work.id, currentUid)
                        }
                    }

                    if (!videoUrl.isNullOrBlank()) {
                        coroutineScope.launch {
                            val steps = listOf(15, 35, 60, 85, 100)
                            for (pct in steps) {
                                if (pct < 100) {
                                    com.chroma.studio.ui.components.ToastManager.showToast("Downloading video asset... $pct%")
                                    kotlinx.coroutines.delay(200)
                                } else {
                                    com.chroma.studio.ui.components.ToastManager.showToast("Opening Flash Editor...")
                                }
                            }
                            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                                flashVm.downloadVideoForRemix(
                                    context = this@HomeActivity,
                                    videoUrl = videoUrl,
                                    onProgress = { },
                                    onComplete = { downloadedUri ->
                                        val finalUri = downloadedUri ?: Uri.parse(videoUrl)
                                        flashPickerVideoUri = finalUri
                                        flashMediaType = FlashMediaType.VIDEO
                                        flashRemixPostId = work.id
                                        flashRemixAuthorUsername = work.authorUsername
                                        flashImportedMediaUris = listOf(finalUri)
                                        navController.navigate("flash_video_editor")
                                    }
                                )
                            } else {
                                val finalUri = Uri.parse(videoUrl)
                                flashPickerVideoUri = finalUri
                                flashMediaType = FlashMediaType.VIDEO
                                flashRemixPostId = work.id
                                flashRemixAuthorUsername = work.authorUsername
                                flashImportedMediaUris = listOf(finalUri)
                                navController.navigate("flash_video_editor")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            val steps = listOf(10, 25, 50, 75, 90, 100)
                            for (pct in steps) {
                                if (pct < 100) {
                                    com.chroma.studio.ui.components.ToastManager.showToast("Downloading... $pct%")
                                    kotlinx.coroutines.delay(300)
                                } else {
                                    com.chroma.studio.ui.components.ToastManager.showToast("Download complete! Opening workspace...")
                                }
                            }
                            val remixId = java.util.UUID.randomUUID().toString()
                            vm.repository.save(
                                ChromaWork(
                                    id = remixId,
                                    name = work.title,
                                    description = work.description,
                                    layersJson = work.jsonUrl,
                                    canvasShape = "rounded",
                                    lastModifiedAt = System.currentTimeMillis()
                                )
                            )
                            val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_WORK_ID, remixId)
                                putExtra(MainActivity.EXTRA_WORK_NAME, work.title)
                                putExtra(MainActivity.EXTRA_WORK_DESCRIPTION, work.description)
                                putExtra(MainActivity.EXTRA_LAYERS_JSON, work.jsonUrl)
                                putExtra(MainActivity.EXTRA_CANVAS_SHAPE, "rounded")
                            }
                            startActivity(intent)
                        }
                    }
                }

                val onDownloadCommunityPost: (DiscoverPost) -> Unit = { work ->
                    val videoUrl = extractVideoUrlFromPost(work)
                    coroutineScope.launch {
                        val repo = com.chroma.studio.data.DiscoverRepository()
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { repo.incrementDownloadCount(work.id, it) }
                    }
                    if (!videoUrl.isNullOrBlank()) {
                        try {
                            val downloadUri = Uri.parse(videoUrl)
                            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                                val request = android.app.DownloadManager.Request(downloadUri).apply {
                                    setTitle(work.title.ifBlank { "Flash Video" })
                                    setDescription("Downloading video from @${work.authorUsername}...")
                                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MOVIES, "Chroma_${System.currentTimeMillis()}.mp4")
                                    setAllowedOverMetered(true)
                                    setAllowedOverRoaming(true)
                                }
                                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
                                manager?.enqueue(request)
                                com.chroma.studio.ui.components.ToastManager.showToast("Downloading video to Movies...")
                            } else {
                                com.chroma.studio.ui.components.ToastManager.showToast("Video saved to library!")
                            }
                        } catch (e: Exception) {
                            com.chroma.studio.ui.components.ToastManager.showToast("Video saved to library!")
                        }
                    } else {
                        vm.repository.save(
                            ChromaWork(
                                id = java.util.UUID.randomUUID().toString(),
                                name = work.title,
                                description = "Downloaded from @${work.authorUsername}",
                                layersJson = work.jsonUrl,
                                canvasShape = "rounded",
                                lastModifiedAt = System.currentTimeMillis()
                            )
                        )
                        com.chroma.studio.ui.components.ToastManager.showToast("Saved to library!")
                    }
                }

                // ── Old home bottom sheet state ────────────────────────────
                var showOldHomeSheet by remember { mutableStateOf(false) }

                // ── In-App Dynamic Island Notification HUD ──────────────────
                val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                LaunchedEffect(currentAuthUid) {
                    if (currentAuthUid != null) {
                        var isInitialSnapshot = true
                        val registration = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("chats")
                            .whereArrayContains("participants", currentAuthUid)
                            .addSnapshotListener { snapshot, _ ->
                                if (isInitialSnapshot) {
                                    isInitialSnapshot = false
                                    return@addSnapshotListener
                                }
                                snapshot?.documentChanges?.forEach { change ->
                                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED ||
                                        change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                                    ) {
                                        val room = change.document.toObject(com.chroma.studio.model.ChatRoom::class.java)
                                        val unread = room.unreadCount[currentAuthUid] ?: 0
                                        if (unread > 0 && room.lastMessage.isNotBlank()) {
                                            val currentBackStack = navController.currentBackStackEntry
                                            val currentRoute = currentBackStack?.destination?.route
                                            val routeRoomId = currentBackStack?.arguments?.getString("roomId")
                                            val isViewingInChatRoute = (currentRoute == "chat/{roomId}" || currentRoute?.startsWith("chat/") == true) &&
                                                (routeRoomId == room.id || chatVm.activeRoomId.value == room.id)
                                            val isViewingInChatTab = (currentRoute == "main" || currentRoute == null) &&
                                                pagerState.currentPage == 3 &&
                                                isInsideChatTab &&
                                                chatVm.activeRoomId.value == room.id
                                            val isUserViewingThisRoom = isViewingInChatRoute || isViewingInChatTab

                                            if (!isUserViewingThisRoom && room.lastSender != currentAuthUid) {
                                                val senderId: String? = room.lastSender
                                                val senderProfile: com.chroma.studio.model.UserProfile? = if (senderId != null) chatVm.getUserProfile(senderId) else null
                                                val senderName = senderProfile?.displayName?.ifBlank { null } ?: if (room.isGroup) (room.name ?: "Group") else "New Message"
                                                com.chroma.studio.ui.components.DynamicIslandManager.showChatMessage(
                                                    roomId = room.id,
                                                    senderName = senderName,
                                                    messageText = room.lastMessage,
                                                    avatarUrl = senderProfile?.avatarUrl ?: "",
                                                    onReply = { reply -> chatVm.sendMessageToRoom(room.id, reply) },
                                                    onClick = {
                                                        if (currentRoute == "chat/{roomId}" || currentRoute?.startsWith("chat/") == true) {
                                                            navController.popBackStack()
                                                        }
                                                        navController.navigate("chat/${room.id}")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        try {
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            registration.remove()
                        }
                    }
                }



                    androidx.compose.material3.Scaffold(
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        bottomBar = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showBottomNav,
                                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 24.dp)
                                        .navigationBarsPadding(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    val bgColor = if (vm.isDarkMode) androidx.compose.ui.graphics.Color(0xFF222222).copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                                    val iconTint = if (vm.isDarkMode) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
                                    val mutedTint = iconTint.copy(alpha = 0.4f)
                                    val primaryTint = if (vm.isDarkMode) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black

                                    val items = listOf(
                                        Lucide.House to "Home",
                                        Lucide.Search to "Explore",
                                        Lucide.Globe to "Community",
                                        Lucide.MessageCircle to "Messages",
                                        Lucide.User to "Profile"
                                    )

                                    val tabPositions = remember { Array(5) { androidx.compose.runtime.mutableStateOf(0f to 0f) } } // Array of (xOffset, width) states
                                    val density = androidx.compose.ui.platform.LocalDensity.current

                                    val activeTabX by animateFloatAsState(
                                        targetValue = tabPositions.getOrNull(pagerState.currentPage)?.value?.first ?: 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "activeTabX"
                                    )
                                    val activeTabWidth by animateFloatAsState(
                                        targetValue = tabPositions.getOrNull(pagerState.currentPage)?.value?.second ?: 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "activeTabWidth"
                                    )

                                    val navShape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                                    val glossyBorderBrush = if (vm.isDarkMode) {
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.02f),
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)
                                            )
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.04f),
                                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    // ─── Pill drag state ────────────────────────────────────────────
                                    var isDraggingPill by remember { androidx.compose.runtime.mutableStateOf(false) }
                                    var pillDragOffsetX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                                    // Captured at drag start so pointerInput closure never goes stale
                                    var dragStartTabX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                                    var dragStartTabWidth by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                                    // rememberUpdatedState ensures onDragEnd always sees latest tab positions
                                    val updatedTabPositions by androidx.compose.runtime.rememberUpdatedState(tabPositions)
                                    val navBarWidthPx = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                                    val pillScale by animateFloatAsState(
                                        targetValue = if (isDraggingPill) 1.10f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                        label = "pillScale"
                                    )
                                    val pillDisplayX = if (isDraggingPill) dragStartTabX + pillDragOffsetX else activeTabX

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .shadow(
                                                elevation = 16.dp,
                                                shape = navShape,
                                                spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f)
                                            )
                                            .hazeChild(state = hazeState, shape = navShape)
                                            .background(color = bgColor, shape = navShape)
                                            .drawWithContent {
                                                drawContent()
                                                val strokeWidth = 1.5.dp.toPx()
                                                drawRoundRect(
                                                    brush = glossyBorderBrush,
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                                                    size = size
                                                )
                                            }
                                            .onGloballyPositioned { navBarWidthPx.floatValue = it.size.width.toFloat() }
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        // Sliding Indicator (visual only, no touch handling)
                                        if (activeTabWidth > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = with(density) { pillDisplayX.toDp() })
                                                    .width(with(density) { activeTabWidth.toDp() })
                                                    .fillMaxHeight()
                                                    .padding(vertical = if (isDraggingPill) 6.dp else 10.dp)
                                                    .graphicsLayer {
                                                        scaleX = pillScale
                                                        scaleY = pillScale
                                                    }
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                                    .background(
                                                        if (vm.isDarkMode)
                                                            androidx.compose.ui.graphics.Color.White.copy(alpha = if (isDraggingPill) 0.18f else 0.1f)
                                                        else
                                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = if (isDraggingPill) 0.10f else 0.05f)
                                                    )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            // CORNER tabs: fixed when not active, weighted when active
                                            // MIDDLE tabs: always weighted (absorb the expansion)
                                            val cornerFixedWidth = 52.dp

                                            items.forEachIndexed { index, (icon, label) ->
                                                val isSelected = pagerState.currentPage == index
                                                val isCorner = index == 0 || index == 4

                                                // Determine modifier: corner tabs switch between fixed and weighted
                                                // Middle tabs are always weighted
                                                val tabModifier = when {
                                                    isCorner && isSelected ->
                                                        // Active corner: expand with weight, pushes middle tabs
                                                        Modifier
                                                            .weight(1.8f)
                                                            .fillMaxHeight()
                                                            .onGloballyPositioned { coordinates ->
                                                                tabPositions[index].value = coordinates.positionInParent().x to coordinates.size.width.toFloat()
                                                            }
                                                    isCorner && !isSelected ->
                                                        // Inactive corner: fixed width, never moves
                                                        Modifier
                                                            .width(cornerFixedWidth)
                                                            .fillMaxHeight()
                                                            .onGloballyPositioned { coordinates ->
                                                                tabPositions[index].value = coordinates.positionInParent().x to coordinates.size.width.toFloat()
                                                            }
                                                    else ->
                                                        // Middle tabs: always weighted, flex to give/take space
                                                        Modifier
                                                            .weight(if (isSelected) 1.8f else 1f)
                                                            .fillMaxHeight()
                                                            .onGloballyPositioned { coordinates ->
                                                                tabPositions[index].value = coordinates.positionInParent().x to coordinates.size.width.toFloat()
                                                            }
                                                }

                                                Box(
                                                    modifier = tabModifier
                                                        .clickable(
                                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            if (index >= 3) {
                                                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                                                if (auth.currentUser == null) {
                                                                    navController.navigate("login")
                                                                    return@clickable
                                                                }
                                                            }
                                                            if (currentRoute != "main") {
                                                                navController.popBackStack("main", inclusive = false)
                                                            }
                                                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                                        },
                                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            icon,
                                                            contentDescription = label,
                                                            tint = if (isSelected) primaryTint else mutedTint,
                                                            modifier = Modifier.size(22.dp)
                                                        )

                                                        androidx.compose.animation.AnimatedVisibility(
                                                            visible = isSelected,
                                                            enter = androidx.compose.animation.expandHorizontally() + androidx.compose.animation.fadeIn(),
                                                            exit = androidx.compose.animation.shrinkHorizontally() + androidx.compose.animation.fadeOut()
                                                        ) {
                                                            Row {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = label,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                                    color = primaryTint,
                                                                    maxLines = 1,
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Transparent gesture overlay — drawn LAST so it sits on top of
                                        // the Row's clickable items and correctly wins touch dispatch
                                        if (activeTabWidth > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = with(density) { pillDisplayX.toDp() })
                                                    .width(with(density) { activeTabWidth.toDp() })
                                                    .fillMaxHeight()
                                                    .pointerInput(Unit) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = { _ ->
                                                                dragStartTabX = activeTabX
                                                                dragStartTabWidth = activeTabWidth
                                                                isDraggingPill = true
                                                                pillDragOffsetX = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                val maxOffset = navBarWidthPx.floatValue - dragStartTabWidth - dragStartTabX
                                                                val minOffset = -dragStartTabX
                                                                pillDragOffsetX = (pillDragOffsetX + dragAmount.x)
                                                                    .coerceIn(minOffset, maxOffset)
                                                            },
                                                            onDragEnd = {
                                                                val pillCenter = dragStartTabX + pillDragOffsetX + dragStartTabWidth / 2f
                                                                val closest = updatedTabPositions.mapIndexed { i, state ->
                                                                    i to kotlin.math.abs((state.value.first + state.value.second / 2f) - pillCenter)
                                                                }.minByOrNull { it.second }?.first ?: pagerState.currentPage
                                                                isDraggingPill = false
                                                                pillDragOffsetX = 0f
                                                                if (closest >= 3) {
                                                                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                                                    if (auth.currentUser == null) {
                                                                        navController.navigate("login")
                                                                        return@detectDragGesturesAfterLongPress
                                                                    }
                                                                }
                                                                if (currentRoute != "main") {
                                                                    navController.popBackStack("main", inclusive = false)
                                                                }
                                                                coroutineScope.launch { pagerState.animateScrollToPage(closest) }
                                                            },
                                                            onDragCancel = {
                                                                isDraggingPill = false
                                                                pillDragOffsetX = 0f
                                                            }
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val activeUser = auth.currentUser
                    if (activeUser != null) {
                        androidx.compose.runtime.LaunchedEffect(activeUser.uid) {
                            try {
                                val repo = com.chroma.studio.data.DiscoverRepository()
                                val prof = repo.getUserProfile(activeUser.uid)
                                if (prof != null) {
                                    com.chroma.studio.data.SavedAccountManager.saveFromProfile(this@HomeActivity, prof, activeUser.email)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    val targetDestination = when {
                        !hasAcceptedTerms -> "terms_agreement"
                        isFirstLaunch || auth.currentUser == null -> "login"
                        !prefs.getBoolean("has_completed_post_onboarding", false) -> "post_onboarding"
                        else -> "main"
                    }
                    NavHost(navController, startDestination = targetDestination, modifier = androidx.compose.ui.Modifier.haze(state = hazeState)) {
                        composable("terms_agreement") {
                            com.chroma.studio.ui.screens.PrivacyPolicyTermsScreen(
                                isFirstLaunch = !hasAcceptedTerms,
                                onAccept = {
                                    prefs.edit().putBoolean("has_accepted_terms", true).apply()
                                    if (auth.currentUser == null) {
                                        navController.navigate("login") {
                                            popUpTo("terms_agreement") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("main") {
                                            popUpTo("terms_agreement") { inclusive = true }
                                        }
                                    }
                                },
                                onDecline = {
                                    finish()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("privacy_terms") {
                            com.chroma.studio.ui.screens.PrivacyPolicyTermsScreen(
                                isFirstLaunch = false,
                                onAccept = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("login") {
                            com.chroma.studio.ui.screens.LoginScreen(
                                onLoginSuccess = {
                                    prefs.edit().putBoolean("is_first_launch", false).apply()
                                    val hasCompletedOnboarding = prefs.getBoolean("has_completed_post_onboarding", false)
                                    if (!hasCompletedOnboarding) {
                                        navController.navigate("post_onboarding") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(this@HomeActivity, "Logged in successfully", Toast.LENGTH_SHORT).show()
                                        navController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                },
                                onCancel = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        finish()
                                    }
                                }
                            )
                        }

                        composable("post_onboarding") {
                            com.chroma.studio.ui.screens.PostOnboardingScreen(
                                onFinish = {
                                    prefs.edit().putBoolean("has_completed_post_onboarding", true).apply()
                                    Toast.makeText(this@HomeActivity, "Workspace ready", Toast.LENGTH_SHORT).show()
                                    navController.navigate("main") {
                                        popUpTo("post_onboarding") { inclusive = true }
                                    }
                                },
                                onToggleDarkMode = { isDark ->
                                    vm.updateDarkMode(isDark)
                                }
                            )
                        }

                        composable(
                    route = "main",
                    exitTransition = {
                        if (targetState.destination.route?.startsWith("chat/") == true) {
                            slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left, androidx.compose.animation.core.tween(350))
                        } else {
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
                        }
                    },
                    popEnterTransition = {
                        if (initialState.destination.route?.startsWith("chat/") == true) {
                            slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right, androidx.compose.animation.core.tween(350))
                        } else {
                            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300))
                        }
                    }
                ) {
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()
                            val storyVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.StoryViewModel>()
                            androidx.activity.compose.BackHandler(enabled = pagerState.currentPage != 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // ── Home Studio Screen ──────────────────────────────────────────
                                    HomeScreen(
                                        works = vm.works,
                                        selectedWorkIds = vm.selectedWorkIds,
                                        selectionMode = vm.selectionMode,
                                        isLoading = vm.isLoadingWorks,
                                        isDarkMode = vm.isDarkMode,
                                        isVerified = vm.isCurrentUserVerified,
                                        initialViewMode = initialViewMode,
                                        showHeader = true,
                                        onToggleDarkMode = {
                                            val newDark = !vm.isDarkMode
                                            vm.updateDarkMode(newDark)
                                            prefs.edit().putBoolean("is_dark_mode", newDark).apply()
                                        },
                                        onViewModeChanged = { newMode ->
                                            prefs.edit().putString("view_mode", newMode.name).apply()
                                        },
                                        onNewWork = {
                                            startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                                        },
                                        onNavigateToCreatePost = {
                                            navController.navigate("create_post")
                                        },
                                        onNavigateToFlashPicker = {
                                            flashRemixPostId = null
                                            flashRemixAuthorUsername = null
                                            navController.navigate("flash_picker")
                                        },
                                        onNavigateToCreateTextStatus = {
                                            navController.navigate("create_text_status")
                                        },
                                        onOpenWork = { work ->
                                            val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                                                putExtra(MainActivity.EXTRA_WORK_ID, work.id)
                                                putExtra(MainActivity.EXTRA_WORK_NAME, work.name)
                                                putExtra(MainActivity.EXTRA_WORK_DESCRIPTION, work.description)
                                                putExtra(MainActivity.EXTRA_LAYERS_JSON, work.layersJson)
                                                putExtra(MainActivity.EXTRA_CANVAS_SHAPE, work.canvasShape)
                                            }
                                            startActivity(intent)
                                        },
                                        onRenameWork = { id, name, desc -> vm.rename(id, name, desc) },
                                        onDuplicateWork = { work ->
                                            vm.duplicate(work)
                                            com.chroma.studio.ui.components.ToastManager.showToast(message = "Work duplicated")
                                        },
                                        onShareToDiscover = { workId ->
                                            navController.navigate("publish_work/$workId")
                                        },
                                        onToggleSelection = { id -> vm.toggleSelection(id) },
                                        onUpdateSelection = { newSelection -> vm.updateSelection(newSelection) },
                                        onClearSelection = { vm.clearSelection() },
                                        onDeleteSelected = {
                                            vm.softDeleteSelected()
                                            com.chroma.studio.ui.components.ToastManager.showToast(
                                                message = "Items moved to trash",
                                                actionLabel = "View",
                                                onAction = { navController.navigate("trash") }
                                            )
                                        },
                                        onSoftDelete = { id ->
                                            vm.softDelete(id)
                                            com.chroma.studio.ui.components.ToastManager.showToast(
                                                message = "Moved to trash",
                                                actionLabel = "View",
                                                onAction = { navController.navigate("trash") }
                                            )
                                        },
                                        onEditHomeBackground = {
                                            val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                                                putExtra(MainActivity.EXTRA_IS_HOME_BACKGROUND, true)
                                                if (vm.customHomeBgLayersJson != null) {
                                                    putExtra(MainActivity.EXTRA_LAYERS_JSON, vm.customHomeBgLayersJson)
                                                    putExtra(MainActivity.EXTRA_CANVAS_SHAPE, vm.customHomeBgShape)
                                                }
                                            }
                                            startActivity(intent)
                                        },
                                        customHomeBgLayersJson = vm.customHomeBgLayersJson,
                                        customHomeBgShape = vm.customHomeBgShape,
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onNavigateToBackgrounds = { navController.navigate("backgrounds") },
                                        onNavigateToDrafts = { navController.navigate("drafts_list") },
                                        onNavigateToDiscover = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                                        onNavigateToTrash = { navController.navigate("trash") },
                                        onRequireLogin = { navController.navigate("login") },
                                        onSetAsBackground = { work ->
                                            prefs.edit()
                                                .putString("custom_home_background_layers", work.layersJson)
                                                .putString("custom_home_background_shape", work.canvasShape)
                                                .apply()
                                            vm.refresh()
                                            com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background updated")
                                        },
                                        onResetHomeBackground = {
                                            prefs.edit()
                                                .remove("custom_home_background_layers")
                                                .remove("custom_home_background_shape")
                                                .apply()
                                            vm.refresh()
                                            com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background reset to default")
                                        },
                                        onSetAsLiveWallpaper = { work ->
                                            vm.repository.setLiveWallpaperId(work.id)
                                            val liveWallpaperIntent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                                putExtra(
                                                    android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                                    android.content.ComponentName(this@HomeActivity, ChromaWallpaperService::class.java)
                                                )
                                            }
                                            try {
                                                startActivity(liveWallpaperIntent)
                                            } catch (e: android.content.ActivityNotFoundException) {
                                                try {
                                                    startActivity(Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                                                } catch (e2: android.content.ActivityNotFoundException) {
                                                    com.chroma.studio.ui.components.ToastManager.showToast("Live wallpaper is not supported on this device")
                                                }
                                            }
                                        },
                                        repository = vm.repository
                                    )
                                }
                                1 -> {
                        com.chroma.studio.ui.screens.SearchScreen(
                            repository = vm.repository,
                            discoverViewModel = discoverVm,
                            onNavigateToProfile = { userId ->
                                navController.navigate("userProfile/$userId")
                            },
                            onNavigateToPost = { creatorId, postId ->
                                navController.navigate("userProfile/$creatorId?postId=$postId")
                            },
                            onRemixWork = onRemixCommunityPost,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                                }
                                2 -> {
                        val works by discoverVm.works.collectAsState()
                        val isLoading by discoverVm.isLoading.collectAsState()
                        val isRefreshing by discoverVm.isRefreshing.collectAsState()
                        val likedWorkIds by discoverVm.likedWorkIds.collectAsState()
                        val savedWorkIds by discoverVm.savedWorkIds.collectAsState()
                        val isLoadingMore by discoverVm.isLoadingMore.collectAsState()

                        LaunchedEffect(Unit) {
                            discoverVm.loadWorks()
                        }

                        com.chroma.studio.ui.screens.DiscoverScreen(
                            works = works,
                            isLoading = isLoading,
                            isLoadingMore = isLoadingMore,
                            isRefreshing = isRefreshing,
                            likedWorkIds = likedWorkIds,
                            savedWorkIds = savedWorkIds,
                            repository = vm.repository,
                            discoverViewModel = discoverVm,
                            chatViewModel = chatVm,
                            storyViewModel = storyVm,
                            onRefresh = { discoverVm.loadWorks(isRefresh = true) },
                            onLoadMore = { discoverVm.loadMoreWorks() },
                            onRequireLogin = { navController.navigate("login") },
                            onToggleLike = { workId, userId -> discoverVm.toggleLike(workId, userId) },
                            onAuthorClick = { creatorId, postId ->
                                if (postId != null) {
                                    navController.navigate("userProfile/$creatorId?postId=$postId")
                                } else {
                                    navController.navigate("userProfile/$creatorId")
                                }
                            },
                            onDownloadWork = onDownloadCommunityPost,
                            onRemixWork = onRemixCommunityPost,
                             onDeleteWork = { workId -> discoverVm.deleteWork(workId) },
                             onEditWork = { workId, newTitle, newDesc -> discoverVm.updateWork(workId, newTitle, newDesc) },
                             onCreatePost = { navController.navigate("creator_studio/post") },
                             onCreateFlash = { navController.navigate("flash_projects_hub") },
                             onPublishWork = { navController.navigate("creator_studio/work") },
                             onNavigateToNotifications = { navController.navigate("notifications_feed") },
                             onStoryViewerChange = { isDiscoverStoryViewerActive = it },
                             onNavigateToCreateStory = { navController.navigate("story_editor") },
                             onNavigateToDrafts = { navController.navigate("drafts_list") }
                        )
                                }
                                3 -> {
                                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                    if (auth.currentUser == null) {
                                        androidx.compose.runtime.LaunchedEffect(Unit) {
                                            navController.navigate("main") { popUpTo("main") { inclusive = true } }
                                        }
                                    } else {
                                        com.chroma.studio.chat.ChatScreen(
                                            chatViewModel = chatVm,
                                            storyViewModel = storyVm,
                                            onNavigateToUserProfile = { uid ->
                                                navController.navigate("userProfile/$uid")
                                            },
                                            onNavigateToCreateStory = {
                                                navController.navigate("story_editor")
                                            },
                                            onInsideChatChange = { inside ->
                                                isInsideChatTab = inside
                                            }
                                        )
                                    }
                                }
                                4 -> {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            // Optimistic fallback: Never display a blank screen! Render LoginScreen immediately
                            com.chroma.studio.ui.screens.LoginScreen(
                                onLoginSuccess = {
                                    prefs.edit().putBoolean("is_first_launch", false).apply()
                                    vm.refresh()
                                },
                                onCancel = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                        } else {
                            com.chroma.studio.ui.screens.UserProfileScreen(
                                userId = currentUser.uid,
                                repository = vm.repository,
                                chatViewModel = chatVm,
                                storyViewModel = storyVm,
                                showBackButton = false,
                                onRequireLogin = { navController.navigate("login") },
                                onLogout = handleLogout,
                                onNavigateToChat = { otherUserId ->
                                    chatVm.startChat(otherUserId) { roomId ->
                                        navController.navigate("chat/$roomId") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onNavigateToEditProfile = { navController.navigate("profile_identity") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onDownloadWork = onDownloadCommunityPost,
                                onNavigateToUserList = { targetUserId, type ->
                                    navController.navigate("user_list/$targetUserId/$type")
                                },
                                onRemixWork = onRemixCommunityPost,
                                onNavigateToOriginal = { authorId, origPostId ->
                                    if (origPostId != null) {
                                        navController.navigate("userProfile/$authorId?postId=$origPostId")
                                    } else {
                                        navController.navigate("userProfile/$authorId")
                                    }
                                },
                                onNavigateToCreateStory = { navController.navigate("story_editor") },
                                onStoryViewerChange = { isProfileStoryViewerActive = it },
                                onBack = { navController.popBackStack() }
                            )
                        }
                                }
                            }
                        }
                    }

                    composable("create_post") {
                        com.chroma.studio.ui.screens.CreatorPostOnboardingStudioScreen(
                            initialMode = com.chroma.studio.ui.screens.CreatorStudioMode.POST,
                            workRepository = vm.repository,
                            discoverRepository = remember { com.chroma.studio.data.DiscoverRepository() },
                            flashViewModel = flashVm,
                            onNavigateBack = { navController.popBackStack() },
                            onPostSuccess = {
                                navController.popBackStack()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        )
                    }

                    composable(
                        route = "creator_studio/{mode}?draftId={draftId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("mode") {
                                type = androidx.navigation.NavType.StringType
                                defaultValue = "post"
                            },
                            androidx.navigation.navArgument("draftId") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val modeParam = backStackEntry.arguments?.getString("mode") ?: "post"
                        val draftIdParam = backStackEntry.arguments?.getString("draftId")
                        val studioMode = when (modeParam.lowercase()) {
                            "flash" -> com.chroma.studio.ui.screens.CreatorStudioMode.FLASH
                            "work" -> com.chroma.studio.ui.screens.CreatorStudioMode.WORK
                            else -> com.chroma.studio.ui.screens.CreatorStudioMode.POST
                        }
                        com.chroma.studio.ui.screens.CreatorPostOnboardingStudioScreen(
                            initialMode = studioMode,
                            initialDraftId = draftIdParam,
                            initialImportedUris = flashImportedMediaUris,
                            workRepository = vm.repository,
                            discoverRepository = remember { com.chroma.studio.data.DiscoverRepository() },
                            flashViewModel = flashVm,
                            onNavigateBack = {
                                flashImportedMediaUris = emptyList()
                                navController.popBackStack()
                            },
                            onPostSuccess = {
                                flashImportedMediaUris = emptyList()
                                navController.popBackStack()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        )
                    }

                    composable("flash_projects_hub") {
                        com.chroma.studio.ui.screens.FlashProjectsHubScreen(
                            onBack = { navController.popBackStack() },
                            onImportMedia = { uris ->
                                flashImportedMediaUris = uris
                                flashPickerVideoUri = uris.firstOrNull()
                                navController.navigate("flash_video_editor")
                            },
                            onOpenDraft = { draft ->
                                flashPickerVideoUri = draft.videoUri?.let { Uri.parse(it) }
                                flashTrimStart = draft.flashTrimStartMs
                                flashTrimEnd = draft.flashTrimEndMs
                                navController.navigate("flash_video_editor?draftId=${draft.id}")
                            },
                            onOpenSavedVideo = { videoUri ->
                                flashPickerVideoUri = videoUri
                                navController.navigate("flash_video_editor")
                            }
                        )
                    }

                    composable("drafts_list") {
                        com.chroma.studio.ui.screens.DraftsListScreen(
                            workRepository = vm.repository,
                            onOpenDraft = { draft ->
                                if (draft.mode.equals("flash", ignoreCase = true) || draft.videoUri != null) {
                                    flashPickerVideoUri = draft.videoUri?.let { Uri.parse(it) }
                                    flashTrimStart = draft.flashTrimStartMs
                                    flashTrimEnd = draft.flashTrimEndMs
                                    navController.navigate("flash_video_editor?draftId=${draft.id}")
                                } else {
                                    navController.navigate("creator_studio/${draft.mode}?draftId=${draft.id}")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("flash_feed") {
                        FlashFeedScreen(
                            flashVm = flashVm,
                            commentVm = commentVm,
                            onNavigateToOldHome = { navController.popBackStack() },
                            onNavigateToProfile = { userId ->
                                navController.navigate("userProfile/$userId")
                            },
                            onNavigateToFlashPicker = {
                                flashRemixPostId = null
                                flashRemixAuthorUsername = null
                                navController.navigate("flash_picker")
                            },
                            onRemixRequested = { uri: android.net.Uri, postId: String, authorUsername: String ->
                                flashPickerVideoUri = uri
                                flashMediaType = FlashMediaType.VIDEO
                                flashRemixPostId = postId
                                flashRemixAuthorUsername = authorUsername
                                navController.navigate("flash_video_editor")
                            },
                            onRequireLogin = { navController.navigate("login") }
                        )
                    }

                    composable(
                        route = "publish_work/{workId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("workId") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val workId = backStackEntry.arguments?.getString("workId") ?: return@composable
                        val work = vm.works.find { it.id == workId }
                        if (work != null) {
                            com.chroma.studio.ui.screens.PostDetailsScreen(
                                imageUri = android.net.Uri.EMPTY,
                                selectedTemplate = "None",
                                aspectRatio = work.canvasShape,
                                work = work,
                                onBack = { navController.popBackStack() },
                                onPostSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                }
                            )
                        }
                    }

                    composable(
                        route = "post_details/{imageUri}/{template}/{aspectRatio}",
                        arguments = listOf(
                            androidx.navigation.navArgument("imageUri") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("template") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("aspectRatio") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
                        val encodedTemplate = backStackEntry.arguments?.getString("template") ?: "None"
                        val encodedRatio = backStackEntry.arguments?.getString("aspectRatio") ?: "4:5"
                        val imageUri = android.net.Uri.parse(java.net.URLDecoder.decode(encodedUri, "UTF-8"))
                        val template = java.net.URLDecoder.decode(encodedTemplate, "UTF-8")
                        val aspectRatio = java.net.URLDecoder.decode(encodedRatio, "UTF-8")
                        com.chroma.studio.ui.screens.PostDetailsScreen(
                            imageUri = imageUri,
                            selectedTemplate = template,
                            aspectRatio = aspectRatio,
                            onBack = { navController.popBackStack() },
                            onPostSuccess = {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = false }
                                }
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        )
                    }

                    composable("backgrounds") {

                        com.chroma.studio.ui.screens.BackgroundsScreen(
                            works = vm.works,
                            repository = vm.repository,
                            isCustomBackgroundActive = vm.customHomeBgLayersJson != null,
                            currentCustomBgLayersJson = vm.customHomeBgLayersJson,
                            onBack = { navController.popBackStack() },
                            onSetAsBackground = { work ->
                                prefs.edit()
                                    .putString("custom_home_background_layers", work.layersJson)
                                    .putString("custom_home_background_shape", work.canvasShape)
                                    .apply()
                                vm.refresh()
                                com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background updated")
                                navController.popBackStack()
                            },
                            onResetToDefault = {
                                prefs.edit()
                                    .remove("custom_home_background_layers")
                                    .remove("custom_home_background_shape")
                                    .apply()
                                vm.refresh()
                                com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background reset to default")
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("appearance_settings") {
                        val settingsManager = remember { com.chroma.studio.chat.data.SettingsManager.getInstance(this@HomeActivity) }
                        var currentBubbleStyle by remember { mutableStateOf<com.chroma.studio.chat.data.BubbleStyleType>(settingsManager.bubbleStyle) }
                        var currentGlobeStyle by remember { mutableStateOf<com.chroma.studio.chat.data.VoiceGlobeStyleType>(settingsManager.globeStyle) }
                        var currentCustomCorners by remember { mutableStateOf<com.chroma.studio.chat.data.CustomCornerConfig>(settingsManager.customCorners) }
                        var currentAnimationStyle by remember { mutableStateOf<com.chroma.studio.chat.data.BubbleAnimationStyle>(settingsManager.animationStyle) }

                        com.chroma.studio.chat.ui.components.AppearanceSettingsScreen(
                            currentBubbleStyle = currentBubbleStyle,
                            currentGlobeStyle = currentGlobeStyle,
                            accentColor = com.chroma.studio.chat.data.AccentColorType.BLUE,
                            currentCustomCorners = currentCustomCorners,
                            onUpdateCustomCorners = {
                                currentCustomCorners = it
                                settingsManager.customCorners = it
                            },
                            currentAnimationStyle = currentAnimationStyle,
                            onSelectAnimationStyle = {
                                currentAnimationStyle = it
                                settingsManager.animationStyle = it
                            },
                            onSelectBubbleStyle = {
                                currentBubbleStyle = it
                                settingsManager.bubbleStyle = it
                            },
                            onSelectGlobeStyle = {
                                currentGlobeStyle = it
                                settingsManager.globeStyle = it
                            },
                            onToggleAppearance = {},
                            onAccentCycle = {},
                            onOpenWallpaperPicker = {},
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        com.chroma.studio.ui.screens.AccountCenterScreen(
                            onNavigateToProfileIdentity = { navController.navigate("profile_identity") },
                            onNavigateToSecurity = { navController.navigate("security") },
                            onNavigateToPrivacy = { navController.navigate("privacy") },
                            onNavigateToNotifications = { navController.navigate("notifications") },
                            onNavigateToArchives = { navController.navigate("archives") },
                            onNavigateToActivityHistory = { navController.navigate("activity_history") },
                            onNavigateToPrivacyTerms = { navController.navigate("privacy_terms") },
                            onBack = { navController.popBackStack() },
                            onLogout = handleLogout
                        )
                    }

                    composable("profile_identity") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            var profile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
                            var works by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.chroma.studio.model.DiscoverPost>>(emptyList()) }
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()

                            androidx.compose.runtime.LaunchedEffect(currentUser.uid) {
                                profile = discoverVm.repository.getUserProfile(currentUser.uid)
                                works = discoverVm.repository.getWorksByUser(currentUser.uid)
                            }

                            if (profile != null) {
                                com.chroma.studio.ui.screens.ProfileIdentityScreen(
                                    profile = profile!!,
                                    works = works,
                                    onSave = { updatedProfile ->
                                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            try {
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(updatedProfile.uid)
                                                    .set(updatedProfile)
                                                    .await()
                                            } catch (e: Exception) {
                                                android.util.Log.e("HomeActivity", "Error updating profile", e)
                                            }
                                        }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    OscellionLoadingSpinner()
                                }
                            }
                        }
                    }

                    composable("security") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            var profile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()

                            androidx.compose.runtime.LaunchedEffect(currentUser.uid) {
                                profile = discoverVm.repository.getUserProfile(currentUser.uid)
                            }
                            if (profile != null) {
                                com.chroma.studio.ui.screens.SecurityScreen(
                                    profile = profile!!,
                                    onSave = { updatedProfile ->
                                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(updatedProfile.uid).set(updatedProfile).await()
                                        }
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    composable("privacy") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            var profile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()

                            androidx.compose.runtime.LaunchedEffect(currentUser.uid) {
                                profile = discoverVm.repository.getUserProfile(currentUser.uid)
                            }
                            if (profile != null) {
                                com.chroma.studio.ui.screens.PrivacyScreen(
                                    profile = profile!!,
                                    onSave = { updatedProfile ->
                                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(updatedProfile.uid).set(updatedProfile).await()
                                        }
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    composable("notifications_feed") {
                        com.chroma.studio.ui.screens.NotificationsFeedScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = { navController.navigate("notifications") },
                            onNavigateToProfile = { userId -> navController.navigate("userProfile/$userId") },
                            onNavigateToChat = { roomId -> navController.navigate("chat/$roomId") },
                            onNavigateToPost = { postId, authorId ->
                                if (!authorId.isNullOrBlank()) navController.navigate("userProfile/$authorId?postId=$postId")
                                else navController.navigate("flash_feed")
                            },
                            onNavigateToProject = { draftId ->
                                if (!draftId.isNullOrBlank()) navController.navigate("flash_video_editor?draftId=$draftId")
                                else navController.navigate("flash_projects_hub")
                            }
                        )
                    }

                    composable("notifications") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            var profile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()

                            androidx.compose.runtime.LaunchedEffect(currentUser.uid) {
                                profile = discoverVm.repository.getUserProfile(currentUser.uid)
                            }
                            if (profile != null) {
                                com.chroma.studio.ui.screens.NotificationsSettingsScreen(
                                    profile = profile!!,
                                    onSave = { updatedProfile ->
                                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(updatedProfile.uid).set(updatedProfile).await()
                                        }
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                    composable("archives") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()
                            com.chroma.studio.ui.screens.ArchivesScreen(
                                userId = currentUser.uid,
                                discoverVm = discoverVm,
                                repository = vm.repository,
                                onBack = { navController.popBackStack() },
                                onRequireLogin = { navController.navigate("login") }
                            )
                        }
                    }

                    composable("activity_history") {
                        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()
                            com.chroma.studio.ui.screens.ActivityHistoryScreen(
                                userId = currentUser.uid,
                                discoverVm = discoverVm,
                                repository = vm.repository,
                                onBack = { navController.popBackStack() },
                                onRequireLogin = { navController.navigate("login") },
                                onNavigateToUser = { targetUserId -> navController.navigate("userProfile/$targetUserId") },
                                onNavigateToPost = { postId, authorId ->
                                    if (!authorId.isNullOrBlank()) {
                                        navController.navigate("userProfile/$authorId?postId=$postId")
                                    } else {
                                        navController.navigate("flash_feed")
                                    }
                                }
                            )
                        }
                    }

                    // ── Flash Upload Flow Routes ──────────────────────────────────────
                    composable("flash_picker") {
                        FlashPickerScreen(
                            onVideoSelected = { uri ->
                                flashPickerVideoUri = uri
                                flashMediaType = FlashMediaType.VIDEO
                                navController.navigate("flash_video_editor")
                            },
                            onPhotosSelected = { uris ->
                                flashPickerPhotoUris = uris
                                flashMediaType = if (uris.size == 1) FlashMediaType.PHOTO else FlashMediaType.SLIDESHOW
                                navController.navigate("flash_photo_editor")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "flash_video_editor?draftId={draftId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("draftId") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val draftId = backStackEntry.arguments?.getString("draftId")
                        val draft = draftId?.let { com.chroma.studio.data.DraftPostRepository(this@HomeActivity).getDraftById(it) }
                        val flashDraft = draftId?.let { id -> flashVm.drafts.value.find { it.id == id } }
                        val toLoadUris = if (flashImportedMediaUris.isNotEmpty()) {
                            flashImportedMediaUris
                        } else {
                            val singleUri = draft?.videoUri?.let { Uri.parse(it) }
                                ?: flashDraft?.localUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                                ?: flashPickerVideoUri
                            if (singleUri != null) listOf(singleUri) else emptyList()
                        }
                        val uri = toLoadUris.firstOrNull()
                        if (uri == null) {
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                navController.navigate("flash_projects_hub") {
                                    popUpTo("flash_video_editor") { inclusive = true }
                                }
                            }
                            return@composable
                        }

                        // Unified Single Video Editor for ALL video editing (single & multi-clip)
                        com.chroma.studio.ui.screens.FinalCutProVideoEditorScreen(
                            initialUri = uri,
                            initialUris = toLoadUris,
                            draftId = draftId,
                            remixedFromAuthorUsername = flashRemixAuthorUsername,
                            onDone = { startMs, endMs, clips ->
                                flashImportedMediaUris = emptyList()
                                flashTrimStart = startMs
                                flashTrimEnd = endMs
                                flashPickerVideoUri = clips.firstOrNull() ?: uri
                                flashFilterName = "None"
                                flashBrightness = 0f
                                flashContrast = 1f
                                flashSaturation = 1f
                                flashHue = 0f
                                flashThumbnail = null
                                navController.navigate("flash_post")
                            },
                            onBack = {
                                flashImportedMediaUris = emptyList()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("flash_photo_editor") {
                        val uris = flashPickerPhotoUris.takeIf { it.isNotEmpty() } ?: run { navController.popBackStack(); return@composable }
                        FlashPhotoEditorScreen(
                            photoUris = uris,
                            onNext = { filterName, thumbnail, durationPerSlide ->
                                flashFilterName = filterName
                                flashThumbnail = thumbnail
                                flashDurationPerSlide = durationPerSlide
                                if (thumbnail != null) {
                                    try {
                                        val file = java.io.File(this@HomeActivity.cacheDir, "flash_edited_${System.currentTimeMillis()}.jpg")
                                        java.io.FileOutputStream(file).use { out ->
                                            thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
                                        }
                                        flashPickerPhotoUris = listOf(android.net.Uri.fromFile(file))
                                    } catch (_: Exception) {}
                                }
                                navController.navigate("flash_post")
                            },
                            onSaveDraft = { filterName ->
                                flashVm.saveDraft(
                                    com.chroma.studio.model.FlashDraft(
                                        localUri = uris.firstOrNull()?.toString() ?: "",
                                        mediaType = if (uris.size == 1) FlashMediaType.PHOTO else FlashMediaType.SLIDESHOW,
                                        filterName = filterName
                                    )
                                )
                                navController.navigate("main") { popUpTo("main") { inclusive = false } }
                                Toast.makeText(this@HomeActivity, "Draft saved!", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("flash_post") {
                        val uri = flashPickerVideoUri ?: flashPickerPhotoUris.firstOrNull() ?: run { navController.popBackStack(); return@composable }
                        FlashPostScreen(
                            flashVm = flashVm,
                            videoUri = uri,
                            thumbnailBitmap = flashThumbnail,
                            trimStartMs = flashTrimStart,
                            trimEndMs = flashTrimEnd,
                            filterName = flashFilterName,
                            brightness = flashBrightness,
                            contrast = flashContrast,
                            saturation = flashSaturation,
                            hue = flashHue,
                            mediaType = flashMediaType,
                            photoUris = flashPickerPhotoUris,
                            durationPerSlideMs = flashDurationPerSlide,
                            remixedFromPostId = flashRemixPostId,
                            remixedFromAuthorUsername = flashRemixAuthorUsername,
                            onSuccess = {
                                try {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    try {
                                        navController.popBackStack("main", inclusive = false)
                                    } catch (e2: Exception) {
                                        // fallback if popBackStack fails
                                    }
                                }
                                flashRemixPostId = null
                                flashRemixAuthorUsername = null
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("trash") {
                        com.chroma.studio.ui.screens.TrashScreen(
                            deletedWorks = vm.deletedWorks,
                            selectedWorkIds = vm.trashSelectedWorkIds,
                            selectionMode = vm.trashSelectionMode,
                            onToggleSelection = { id -> vm.toggleTrashSelection(id) },
                            onSelectAll = { vm.selectAllTrash() },
                            onClearSelection = { vm.clearTrashSelection() },
                            onDeleteSelected = { vm.permanentDeleteTrashSelected() },
                            onRestoreSelected = { vm.restoreTrashSelected() },
                            onEmptyTrash = { vm.emptyTrash() },
                            repository = vm.repository,
                            onRestore = { id -> vm.restore(id) },
                            onPermanentDelete = { id -> vm.permanentDelete(id) },
                            onBack = { navController.popBackStack() },
                            onRefresh = { vm.refresh() }
                        )
                    }
                    composable(
                        route = "userProfile/{userId}?postId={postId}",
                        arguments = listOf(
                            androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("postId") { type = androidx.navigation.NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        val postId = backStackEntry.arguments?.getString("postId")
                        val storyVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.StoryViewModel>()

                        com.chroma.studio.ui.screens.UserProfileScreen(
                            userId = userId,
                            initialPostId = postId,
                            repository = vm.repository,
                            chatViewModel = chatVm,
                            storyViewModel = storyVm,
                            onRequireLogin = { navController.navigate("login") },
                            onLogout = handleLogout,
                            onNavigateToChat = { otherUserId ->
                                chatVm.startChat(otherUserId) { roomId ->
                                    navController.navigate("chat/$roomId") {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateToEditProfile = { navController.navigate("profile_identity") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onDownloadWork = onDownloadCommunityPost,
                            onRemixWork = onRemixCommunityPost,
                            onNavigateToOriginal = { authorId, origPostId ->
                                if (origPostId != null) {
                                    navController.navigate("userProfile/$authorId?postId=$origPostId")
                                } else {
                                    navController.navigate("userProfile/$authorId")
                                }
                            },
                            onStoryViewerChange = { isProfileStoryViewerActive = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "user_profile/{userId}",
                        arguments = listOf(androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        navController.navigate("userProfile/$userId")
                    }
                    composable(
                        route = "chat/{roomId}",
                        arguments = listOf(androidx.navigation.navArgument("roomId") { type = androidx.navigation.NavType.StringType }),
                        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(350)) },
                        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(350)) },
                        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(350)) },
                        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(350)) }
                    ) { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
                        val storyVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.StoryViewModel>()
                        com.chroma.studio.chat.ChatScreen(
                            roomId = roomId,
                            initialRoomId = roomId,
                            chatViewModel = chatVm,
                            storyViewModel = storyVm,
                            onBack = { navController.popBackStack() },
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToUserProfile = { userId -> navController.navigate("userProfile/$userId") },
                            onNavigateToGroupInfo = { rId -> navController.navigate("group_info/$rId") },
                            onInsideChatChange = { isInsideChatTab = it }
                        )
                    }

                    composable(
                        route = "group_info/{roomId}",
                        arguments = listOf(androidx.navigation.navArgument("roomId") { type = androidx.navigation.NavType.StringType })
                    ) { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
                        com.chroma.studio.ui.screens.GroupInfoScreen(
                            chatViewModel = chatVm,
                            roomId = roomId,
                            onBack = { navController.popBackStack() },
                            onNavigateToProfile = { userId -> navController.navigate("userProfile/$userId") }
                        )
                    }

                    composable("create_group") {
                        com.chroma.studio.ui.screens.CreateGroupScreen(
                            chatViewModel = chatVm,
                            onBack = { navController.popBackStack() },
                            onGroupCreated = { roomId ->
                                navController.popBackStack()
                                navController.navigate("chat/$roomId")
                            }
                        )
                    }

                    composable(
                        route = "user_list/{userId}/{type}",
                        arguments = listOf(
                            androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        val type = backStackEntry.arguments?.getString("type") ?: "followers"
                        val discoverVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.DiscoverViewModel>()
                        com.chroma.studio.ui.screens.UserListScreen(
                            userId = userId,
                            type = type,
                            discoverVm = discoverVm,
                            onNavigateToUser = { uid -> navController.navigate("userProfile/$uid") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("story_editor") {
                        val storyVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.StoryViewModel>()
                        com.chroma.studio.ui.screens.StoryEditorScreen(
                            storyViewModel = storyVm,
                            onNavigateBack = { navController.popBackStack() },
                            onStoryPosted = {
                                navController.popBackStack()
                                com.chroma.studio.ui.components.ToastManager.showToast("Story published! 🌟")
                            }
                        )
                    }

                    composable("create_text_status") {
                        val storyVm = androidx.lifecycle.viewmodel.compose.viewModel<com.chroma.studio.viewmodel.StoryViewModel>()
                        com.chroma.studio.ui.screens.StoryEditorScreen(
                            storyViewModel = storyVm,
                            onNavigateBack = { navController.popBackStack() },
                            onStoryPosted = {
                                navController.popBackStack()
                                com.chroma.studio.ui.components.ToastManager.showToast("Story published! 🌟")
                            }
                        )
                    }
                }
            }
            com.chroma.studio.ui.components.ChromaToastHost()
            com.chroma.studio.ui.components.IosDynamicIslandHost(isNotchMode = false)
        }
        } // End OscellionBackground
    }
}

    override fun onResume() {
        super.onResume()
        // Refresh when returning from editor (user may have saved a new/updated work)
        vm.refresh()
    }
}

