package com.chroma.studio.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Global In-App Browser Manager.
 * Calling [openUrl] from anywhere in the app seamlessly slides up the lightweight in-app browser.
 */
object InAppBrowserManager {
    var activeUrl by mutableStateOf<String?>(null)
        private set

    fun openUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val cleanUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        activeUrl = cleanUrl
    }

    fun close() {
        activeUrl = null
    }
}

/**
 * Top-level Host composable that listens for [InAppBrowserManager.activeUrl]
 * and displays the Instagram-grade in-app browser overlay.
 */
@Composable
fun ChromaInAppBrowserHost() {
    val url = InAppBrowserManager.activeUrl
    AnimatedVisibility(
        visible = url != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(180))
    ) {
        if (url != null) {
            ChromaInAppBrowser(
                initialUrl = url,
                onDismiss = { InAppBrowserManager.close() }
            )
        }
    }
}

/**
 * Lightweight, high-performance Inbuilt In-App Browser matching Instagram / Apple HIG.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromaInAppBrowser(
    initialUrl: String,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var loadProgress by remember { mutableFloatStateOf(0.1f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Intercept hardware/system Back button: navigate web history first, then dismiss
    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onDismiss()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = loadProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "browserProgress"
    )

    val hostDomain = remember(currentUrl) {
        try {
            val uri = Uri.parse(currentUrl)
            uri.host?.removePrefix("www.") ?: currentUrl
        } catch (_: Exception) {
            currentUrl
        }
    }

    val isSecure = remember(currentUrl) {
        currentUrl.startsWith("https://")
    }

    fun shareUrl() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, pageTitle)
                putExtra(Intent.EXTRA_TEXT, currentUrl)
            }
            context.startActivity(Intent.createChooser(intent, "Share Link"))
        } catch (_: Exception) {
            ToastManager.showToast("Could not share link")
        }
    }

    fun openInExternalBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            ToastManager.showToast("No external browser available")
        }
    }

    fun copyLink() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
            ToastManager.showToast("Link copied to clipboard")
        } catch (_: Exception) {
            ToastManager.showToast("Failed to copy link")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            // ── Top Navigation Bar (Instagram Header) ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = statusBarTop, bottom = 6.dp, start = 8.dp, end = 8.dp)
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Close Button (X)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = colors.textMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Center: SSL Security Icon + Domain + Page Title
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { copyLink() }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSecure) {
                                    Icon(
                                        imageVector = Lucide.Lock,
                                        contentDescription = "Secure Connection",
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = hostDomain,
                                    color = colors.textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (pageTitle.isNotBlank() && pageTitle != hostDomain) {
                                Text(
                                    text = pageTitle,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Share & Overflow 3-Dots
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { shareUrl() }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Share2,
                                    contentDescription = "Share",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { showOverflowMenu = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.EllipsisVertical,
                                        contentDescription = "More",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF9F9F9))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open in Chrome", color = colors.textMain, fontSize = 14.sp) },
                                        leadingIcon = { Icon(Lucide.ExternalLink, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            openInExternalBrowser()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy Link", color = colors.textMain, fontSize = 14.sp) },
                                        leadingIcon = { Icon(Lucide.Copy, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            copyLink()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reload Page", color = colors.textMain, fontSize = 14.sp) },
                                        leadingIcon = { Icon(Lucide.RotateCw, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            showOverflowMenu = false
                                            webView?.reload()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color.Transparent)
                    ) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .background(Color(0xFF007AFF))
                            )
                        }
                    }
                }
            }

            // Hairline separator below header
            HorizontalDivider(
                color = if (colors.isDark) Color(0x22FFFFFF) else Color(0x15000000),
                thickness = 0.5.dp
            )

            // ── Main Web Content View ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress / 100f
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) {
                                        pageTitle = title
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val target = request?.url?.toString() ?: return false
                                    if (target.startsWith("http://") || target.startsWith("https://")) {
                                        currentUrl = target
                                        return false
                                    }
                                    // Handle custom app intents (tel:, mailto:, intent:, etc.)
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        ctx.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    hasError = false
                                    if (!url.isNullOrBlank()) currentUrl = url
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    if (!url.isNullOrBlank()) currentUrl = url
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        isLoading = false
                                    }
                                }
                            }

                            loadUrl(initialUrl)
                            webView = this
                        }
                    },
                    update = { view ->
                        webView = view
                    }
                )

                // Clean Apple-style Error State with Retry
                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.bg)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0xFFF2F2F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Globe,
                                    contentDescription = null,
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Unable to load page",
                                color = colors.textMain,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Check your connection and try again.",
                                color = Color(0xFF8E8E93),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF007AFF))
                                    .clickable {
                                        hasError = false
                                        webView?.reload()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Bottom Browser Controls Strip ─────────────────────────────────
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
            ) {
                HorizontalDivider(
                    color = if (colors.isDark) Color(0x22FFFFFF) else Color(0x15000000),
                    thickness = 0.5.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = navBarBottom)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(
                        onClick = { if (canGoBack) webView?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = if (canGoBack) colors.textMain else Color(0xFF8E8E93).copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { if (canGoForward) webView?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = "Forward",
                            tint = if (canGoForward) colors.textMain else Color(0xFF8E8E93).copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Refresh / Stop
                    IconButton(
                        onClick = {
                            if (isLoading) webView?.stopLoading()
                            else webView?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = if (isLoading) Lucide.X else Lucide.RotateCw,
                            contentDescription = if (isLoading) "Stop" else "Reload",
                            tint = colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Share
                    IconButton(onClick = { shareUrl() }) {
                        Icon(
                            imageVector = Lucide.Share2,
                            contentDescription = "Share",
                            tint = colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Open in Chrome / External
                    IconButton(onClick = { openInExternalBrowser() }) {
                        Icon(
                            imageVector = Lucide.Compass,
                            contentDescription = "Open in Browser",
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
