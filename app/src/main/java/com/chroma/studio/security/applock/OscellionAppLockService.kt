package com.chroma.studio.security.applock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility engine for Oscellion App Locker.
 *
 * Instant cover: on the first window event of a locked app we add a solid
 * TYPE_ACCESSIBILITY_OVERLAY window (a few ms, no extra permission) so the app
 * is never visible while AppLockActivity starts. AppLockActivity removes the
 * cover via [hideCover] once its first frame is on screen.
 */
class OscellionAppLockService : AccessibilityService() {

    private var lastInterceptionTime = 0L
    private var lastInterceptedPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var cover: View? = null
    private val coverFailsafe = Runnable { hideCoverInternal() }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                AppLockerManager.lockAllSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        hideCoverInternal()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return

        // If user returned to home or launcher, clear shield and reset interception cache so re-launch is instant
        if (targetPackage.contains("launcher", ignoreCase = true) ||
            targetPackage.contains("home", ignoreCase = true) ||
            targetPackage == "com.android.systemui") {
            AppLockerManager.shieldPackage = null
            lastInterceptedPackage = null
            AppLockerManager.onForegroundPackageChanged(targetPackage)
            hideCoverInternal()
            return
        }

        // Ignore our own package and transient system UI. These must not count as
        // "the user left the app", otherwise they revoke the unlock session.
        if (isIgnoredPackage(targetPackage)) return

        // Only real activity windows count as a foreground change. Popups, dialogs
        // and toasts report android.widget.* / android.view.* class names.
        val cls = event.className?.toString().orEmpty()
        val isActivityWindow = !(cls.startsWith("android.widget.") ||
                cls.startsWith("android.view.") ||
                cls.startsWith("android.app.Dialog"))
        if (isActivityWindow) {
            AppLockerManager.onForegroundPackageChanged(targetPackage)
        }

        // Not a locked app (home screen, other apps): make sure no cover is left up.
        if (!AppLockerManager.isPackageLocked(this, targetPackage)) {
            hideCoverInternal()
            return
        }

        // Already unlocked in this session.
        if (AppLockerManager.isPackageSessionUnlocked(targetPackage)) {
            hideCoverInternal()
            return
        }

        // Shield is already up for this package: do nothing (no relaunch, no flicker).
        if (AppLockerManager.shieldPackage == targetPackage) return

        // Debounce identical launches.
        val now = System.currentTimeMillis()
        if (targetPackage == lastInterceptedPackage && now - lastInterceptionTime < 350L) {
            return
        }
        lastInterceptionTime = now
        lastInterceptedPackage = targetPackage

        // 1) Cover immediately, 2) start the real shield.
        showCover()
        AppLockActivity.launch(this, targetPackage)
    }

    override fun onInterrupt() {}

    // ------------------------------------------------------------------ cover

    private fun isIgnoredPackage(pkg: String): Boolean =
        pkg == packageName ||
        pkg == "android" ||
        pkg == "com.android.systemui" ||
        pkg == "com.google.android.permissioncontroller" ||
        pkg == "com.android.permissioncontroller" ||
        pkg.contains("inputmethod") ||
        pkg.contains("keyboard")

    private fun showCover() {
        if (cover != null) return
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = View(this).apply { setBackgroundColor(COVER_COLOR) }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            )
            if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            wm.addView(view, lp)
            cover = view
            // Failsafe: never leave a black screen stuck if the shield fails to appear.
            handler.removeCallbacks(coverFailsafe)
            handler.postDelayed(coverFailsafe, COVER_FAILSAFE_MS)
        } catch (_: Exception) {
            cover = null
        }
    }

    private fun hideCoverInternal() {
        handler.removeCallbacks(coverFailsafe)
        val v = cover ?: return
        cover = null
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {}
    }

    companion object {
        private const val COVER_COLOR = 0xFF030407.toInt() // matches OscellionLockScreen background
        private const val COVER_FAILSAFE_MS = 3000L

        @Volatile private var instance: OscellionAppLockService? = null

        /** Called by AppLockActivity once its first frame is drawn (or after unlock). */
        fun hideCover() {
            val svc = instance ?: return
            svc.handler.post { svc.hideCoverInternal() }
        }

        fun performHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun resetInterception() {
            instance?.lastInterceptedPackage = null
        }
    }
}
