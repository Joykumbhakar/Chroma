package com.chroma.studio.lite

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private companion object {
        const val HOME_URL = "https://oscellion.vercel.app"
        const val REQ_FILE_CHOOSER = 4101
        const val REQ_MEDIA_PERMS = 4102

        val IN_APP_HOSTS = setOf(
            "oscellion.vercel.app",
            "accounts.google.com",
            "oscellion-studio.firebaseapp.com",
            "chroma-studio-31376.firebaseapp.com"
        )

        const val BLOB_DOWNLOAD_SHIM = """
            (function(){
              if(window.__oscellionBlobHooked) return;
              window.__oscellionBlobHooked = true;
              document.addEventListener('click', function(ev){
                var el = ev.target && ev.target.closest ? ev.target.closest('a[download], a[href^="blob:"], a[href^="data:"]') : null;
                if(!el) return;
                var href = el.getAttribute('href') || '';
                if(!href.startsWith('blob:') && !href.startsWith('data:')) return;
                ev.preventDefault();
                var fname = el.getAttribute('download') || ('oscellion-export-' + Date.now());
                if(href.startsWith('data:')) {
                  var semi = href.indexOf(';base64,');
                  if(semi > 5) {
                    var mime = href.slice(5, semi);
                    var b64 = href.slice(semi + 8);
                    window.OscellionLite && window.OscellionLite.saveBase64File(b64, fname, mime);
                  }
                  return;
                }
                fetch(href).then(function(r){ return r.blob(); }).then(function(blob){
                  var reader = new FileReader();
                  reader.onloadend = function(){
                    var res = String(reader.result || '');
                    var idx = res.indexOf(';base64,');
                    if(idx > -1 && window.OscellionLite) {
                      window.OscellionLite.saveBase64File(res.slice(idx + 8), fname, blob.type || 'application/octet-stream');
                    }
                  };
                  reader.readAsDataURL(blob);
                }).catch(function(){});
              }, true);
            })();
        """
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineOverlay: LinearLayout
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var popupDialog: Dialog? = null
    private var popupWebView: WebView? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermission: PermissionRequest? = null
    private var mainFrameError = false
    private var lastIsDark = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySystemBarAppearance(isDark = true)

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#09090B"))
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#09090B"))
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(3)
            ).apply { gravity = Gravity.TOP }
            max = 100
            progressDrawable.setColorFilter(
                Color.parseColor("#0381FE"),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            visibility = View.GONE
        }

        offlineOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#09090B"))
            setPadding(dp(32), dp(32), dp(32), dp(32))
            visibility = View.GONE

            addView(TextView(context).apply {
                text = "Connection Interrupted"
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "Check your internet connection and tap Retry to reload Oscellion Lite."
                setTextColor(Color.parseColor("#A1A1AA"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(20))
            })
            addView(Button(context).apply {
                text = "Retry Connection"
                setBackgroundColor(Color.parseColor("#0381FE"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    mainFrameError = false
                    offlineOverlay.visibility = View.GONE
                    webView.reload()
                }
            })
            addView(Button(context).apply {
                text = "Go to Home Feed"
                setBackgroundColor(Color.parseColor("#18181B"))
                setTextColor(Color.parseColor("#E4E4E7"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                setOnClickListener {
                    mainFrameError = false
                    offlineOverlay.visibility = View.GONE
                    webView.loadUrl(HOME_URL)
                }
            })
        }

        rootLayout.addView(webView)
        rootLayout.addView(offlineOverlay)
        rootLayout.addView(progressBar)
        setContentView(rootLayout)

        configureWebView()

        val initialUrl = resolveIntentUrl(intent) ?: HOME_URL
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(initialUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyStandardWebSettings(target: WebView, stripWebViewMarker: Boolean = true) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(target, true)
        }

        target.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            // Strip Android WebView `; wv)` / `Version/4.0` markers so Google OAuth accepts in-app auth cleanly.
            val cleanBaseUa = userAgentString
                .replace("; wv)", ")")
                .replace("Version/4.0 ", "")
                .trim()
            userAgentString = if (stripWebViewMarker) {
                if (cleanBaseUa.contains("OscellionLite/")) cleanBaseUa else "$cleanBaseUa OscellionLite/1.4"
            } else {
                cleanBaseUa
            }
        }
    }

    private fun isInAppHost(host: String): Boolean {
        return host in IN_APP_HOSTS ||
            host.endsWith(".vercel.app") ||
            host.endsWith(".firebaseapp.com") ||
            host.endsWith(".google.com") ||
            host.endsWith(".googleusercontent.com") ||
            host.endsWith(".googleapis.com")
    }

    private fun handleUrlNavigation(urlString: String): Boolean {
        val uri = Uri.parse(urlString)
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "oscellion") {
            webView.loadUrl(mapOscellionDeepLink(uri))
            return true
        }
        if (scheme == "intent") {
            return handleIntentSchemeUrl(urlString)
        }
        if (scheme == "http" || scheme == "https") {
            val host = uri.host?.lowercase().orEmpty()
            if (isInAppHost(host)) {
                return false
            }
            openExternalUrl(urlString)
            return true
        }
        openExternalUrl(urlString)
        return true
    }

    private fun handleIntentSchemeUrl(intentUrl: String): Boolean {
        return try {
            val parsed = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME)
            if (parsed.resolveActivity(packageManager) != null) {
                startActivity(parsed)
                true
            } else {
                val fallback = parsed.getStringExtra("browser_fallback_url")
                if (!fallback.isNullOrBlank()) {
                    webView.loadUrl(fallback)
                }
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        applyStandardWebSettings(webView, stripWebViewMarker = true)

        webView.addJavascriptInterface(OscellionLiteBridge(), "OscellionLite")

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("data:")) {
                val semi = url.indexOf(";base64,")
                if (semi > 5) {
                    val resolvedMime = url.substring(5, semi)
                    val b64 = url.substring(semi + 8)
                    val fileName = URLUtil.guessFileName(url, contentDisposition, resolvedMime)
                    saveDecodedBase64ToDownloads(b64, fileName, resolvedMime)
                }
                return@setDownloadListener
            }
            if (url.startsWith("blob:")) {
                Toast.makeText(this, "Preparing export…", Toast.LENGTH_SHORT).show()
                return@setDownloadListener
            }
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }?.let {
                        addRequestHeader("Cookie", it)
                    }
                    setTitle(fileName)
                    setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                openExternalUrl(url)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                return handleUrlNavigation(uri.toString())
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                mainFrameError = false
                progressBar.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val code = error?.errorCode ?: 0
                if (
                    request?.isForMainFrame == true &&
                    code != ERROR_UNSUPPORTED_SCHEME &&
                    code != ERROR_UNKNOWN
                ) {
                    mainFrameError = true
                    offlineOverlay.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                if (!mainFrameError) {
                    offlineOverlay.visibility = View.GONE
                }
                CookieManager.getInstance().flush()
                view?.evaluateJavascript(BLOB_DOWNLOAD_SHIM, null)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress in 1..99) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                openOAuthPopupWindow(transport, resultMsg)
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                closeOAuthPopupWindow()
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val parsedMimes = parseAcceptMimeTypes(params?.acceptTypes)
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    if (parsedMimes.size == 1) {
                        type = parsedMimes.first()
                    } else {
                        type = "*/*"
                        if (parsedMimes.isNotEmpty()) {
                            putExtra(Intent.EXTRA_MIME_TYPES, parsedMimes.toTypedArray())
                        }
                    }
                    if (params?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                }
                return try {
                    startActivityForResult(
                        Intent.createChooser(intent, "Choose Media"),
                        REQ_FILE_CHOOSER
                    )
                    true
                } catch (_: Exception) {
                    filePathCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val needed = mutableListOf<String>()
                if (
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ) {
                    needed.add(Manifest.permission.RECORD_AUDIO)
                }
                if (
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources &&
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ) {
                    needed.add(Manifest.permission.CAMERA)
                }
                if (needed.isEmpty()) {
                    request.grant(request.resources)
                } else {
                    pendingWebPermission = request
                    requestPermissions(needed.toTypedArray(), REQ_MEDIA_PERMS)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                fullscreenView?.let { rootLayout.removeView(it) }
                fullscreenView = view
                fullscreenCallback = callback
                setFullscreenSystemUi(true)
                rootLayout.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() {
                fullscreenView?.let { rootLayout.removeView(it) }
                fullscreenView = null
                fullscreenCallback?.onCustomViewHidden()
                fullscreenCallback = null
                setFullscreenSystemUi(false)
                webView.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openOAuthPopupWindow(
        transport: WebView.WebViewTransport,
        resultMsg: Message
    ) {
        closeOAuthPopupWindow()

        val childWebView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }
        applyStandardWebSettings(childWebView, stripWebViewMarker = false)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#09090B"))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.parseColor("#111216"))
            addView(TextView(context).apply {
                text = "Oscellion Account Sign-In"
                setTextColor(Color.WHITE)
                textSize = 13.5f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Button(context).apply {
                text = "Close"
                setBackgroundColor(Color.parseColor("#27272A"))
                setTextColor(Color.WHITE)
                textSize = 12f
                setOnClickListener { closeOAuthPopupWindow() }
            })
        }

        container.addView(header)
        container.addView(
            childWebView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar).apply {
            setContentView(container)
            setOnCancelListener { closeOAuthPopupWindow() }
        }

        childWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme == "http" || scheme == "https") {
                    val host = uri.host?.lowercase().orEmpty()
                    if (isInAppHost(host)) return false
                }
                openExternalUrl(uri.toString())
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
            }
        }
        childWebView.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                closeOAuthPopupWindow()
            }
        }

        popupWebView = childWebView
        popupDialog = dialog
        dialog.show()

        transport.webView = childWebView
        resultMsg.sendToTarget()
    }

    private fun closeOAuthPopupWindow() {
        popupWebView?.apply {
            stopLoading()
            destroy()
        }
        popupWebView = null
        popupDialog?.let {
            if (it.isShowing) it.dismiss()
        }
        popupDialog = null
        CookieManager.getInstance().flush()
    }

    private fun parseAcceptMimeTypes(rawTypes: Array<String>?): List<String> {
        if (rawTypes.isNullOrEmpty()) return emptyList()
        val mimeMap = MimeTypeMap.getSingleton()
        val resolved = LinkedHashSet<String>()
        for (entry in rawTypes) {
            for (token in entry.split(",")) {
                val clean = token.trim().lowercase()
                if (clean.isEmpty()) continue
                if (clean.contains("/")) {
                    resolved.add(clean)
                } else if (clean.startsWith(".")) {
                    val ext = clean.removePrefix(".")
                    mimeMap.getMimeTypeFromExtension(ext)?.let { resolved.add(it) }
                }
            }
        }
        return resolved.toList()
    }

    private fun saveDecodedBase64ToDownloads(base64Data: String, fileName: String, mimeType: String) {
        try {
            val cleanBase64 = base64Data.substringAfter(",")
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank {
                "oscellion-export-${System.currentTimeMillis()}"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    Toast.makeText(this, "Saved $safeName to Downloads", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, safeName)
            outFile.writeBytes(bytes)
            Toast.makeText(this, "Saved $safeName to Downloads", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Could not save file on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        closeOAuthPopupWindow()
        webView.destroy()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val url = resolveIntentUrl(intent) ?: return
        mainFrameError = false
        offlineOverlay.visibility = View.GONE
        webView.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (popupDialog?.isShowing == true) {
            closeOAuthPopupWindow()
            return
        }
        if (fullscreenView != null) {
            webView.webChromeClient?.onHideCustomView()
            return
        }
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE_CHOOSER) {
            val cb = filePathCallback
            filePathCallback = null
            if (resultCode != RESULT_OK || data == null) {
                cb?.onReceiveValue(null)
                return
            }
            val clip = data.clipData
            val uris = if (clip != null && clip.itemCount > 0) {
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            } else if (data.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            cb?.onReceiveValue(uris)
            return
        }
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_MEDIA_PERMS) {
            val req = pendingWebPermission
            pendingWebPermission = null
            if (req != null) {
                val allowedResources = req.resources.filter { res ->
                    when (res) {
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        else -> true
                    }
                }.toTypedArray()
                if (allowedResources.isNotEmpty()) {
                    req.grant(allowedResources)
                } else {
                    req.deny()
                }
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun resolveIntentUrl(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.scheme.equals("oscellion", ignoreCase = true)) {
            mapOscellionDeepLink(data)
        } else {
            data.toString()
        }
    }

    private fun mapOscellionDeepLink(uri: Uri): String {
        val host = uri.host.orEmpty().trim('/')
        val cleanPath = uri.encodedPath.orEmpty().trimStart('/')
        val suffix = if (cleanPath.isEmpty()) "" else "/$cleanPath"
        return when (host) {
            "post", "work" -> "$HOME_URL/post$suffix"
            "flash", "reel" -> "$HOME_URL/flash$suffix"
            "chat" -> "$HOME_URL/chat$suffix"
            "u", "user" -> "$HOME_URL/u$suffix"
            "" -> HOME_URL
            else -> "$HOME_URL/$host$suffix"
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            // no browser handler installed
        }
    }

    private fun setFullscreenSystemUi(fullscreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            if (fullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.systemBars())
            } else {
                controller.show(WindowInsets.Type.systemBars())
                applySystemBarAppearance(lastIsDark)
            }
        } else {
            @Suppress("DEPRECATION")
            if (fullscreen) {
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                applySystemBarAppearance(lastIsDark)
            }
        }
    }

    private fun applySystemBarAppearance(isDark: Boolean) {
        lastIsDark = isDark
        val barColor = if (isDark) Color.parseColor("#09090B") else Color.WHITE
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            val flags =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            controller.setSystemBarsAppearance(if (isDark) 0 else flags, flags)
        } else {
            @Suppress("DEPRECATION")
            var vis = window.decorView.systemUiVisibility
            @Suppress("DEPRECATION")
            vis = if (isDark) {
                vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = vis
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    inner class OscellionLiteBridge {
        @JavascriptInterface
        fun setDarkSystemBars(isDark: Boolean) {
            runOnUiThread { applySystemBarAppearance(isDark) }
        }

        @JavascriptInterface
        fun vibrateHaptic() {
            runOnUiThread {
                webView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        @JavascriptInterface
        fun shareUrl(title: String, url: String) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title $url".trim())
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        @JavascriptInterface
        fun saveBase64File(base64Data: String, fileName: String, mimeType: String) {
            runOnUiThread {
                saveDecodedBase64ToDownloads(base64Data, fileName, mimeType)
            }
        }

        @JavascriptInterface
        fun authenticateBiometric(callbackName: String) {
            val safeCallback = callbackName.replace(Regex("[^a-zA-Z0-9_]"), "")
            if (safeCallback.isEmpty()) return
            runOnUiThread {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    deliverBiometricResult(safeCallback, false, "Use PIN on Android 8")
                    return@runOnUiThread
                }
                val builder = BiometricPrompt.Builder(this@MainActivity)
                    .setTitle("Unlock Oscellion Lite")
                    .setSubtitle("Verify fingerprint or face to continue")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setNegativeButton("Use PIN", mainExecutor) { _, _ ->
                        deliverBiometricResult(safeCallback, false, "Use PIN to unlock")
                    }
                }
                builder.build().authenticate(
                    CancellationSignal(),
                    mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            deliverBiometricResult(safeCallback, true, "")
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            deliverBiometricResult(
                                safeCallback,
                                false,
                                errString?.toString() ?: "Cancelled"
                            )
                        }

                        override fun onAuthenticationFailed() {
                            // keep prompt open until user succeeds or cancels
                        }
                    }
                )
            }
        }

        private fun deliverBiometricResult(cb: String, ok: Boolean, msg: String) {
            val escaped = msg.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript("window['$cb']?.($ok,'$escaped');", null)
        }
    }
}
