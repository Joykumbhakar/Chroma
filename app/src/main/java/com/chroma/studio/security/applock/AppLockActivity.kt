package com.chroma.studio.security.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.chroma.studio.ui.components.OscellionLockScreen
import com.chroma.studio.ui.theme.OscellionTheme

/**
 * High-Security Instant Shield Activity for Oscellion App Locker.
 * Displays the exact same OscellionLockScreen UI over locked applications.
 * If user unlocks, grants session access; if dismissed or back is pressed,
 * immediately redirects to the Android Home launcher and ensures the locked app
 * cannot be accessed or revealed under any circumstances.
 */
class AppLockActivity : ComponentActivity() {

    private var targetPackage: String = ""
    private var targetAppName: String = "the app"
    private var isUnlocked = false
    private var isRedirectingToHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make window edge-to-edge transparent overlay
        WindowCompat.setDecorFitsSystemWindows(window, false)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        targetAppName = resolveAppName(targetPackage)

        // Register hardware / predictive back callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleCancelOrBack()
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                handleCancelOrBack()
            }
        }

        setContent {
            OscellionTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OscellionLockScreen(
                        titleText = if (targetAppName != "the app") "Unlock $targetAppName" else "Unlock the app",
                        onBack = {
                            handleCancelOrBack()
                        },
                        onUnlockSuccess = {
                            isUnlocked = true
                            if (targetPackage.isNotBlank()) {
                                AppLockerManager.markPackageUnlocked(targetPackage)
                            }
                            finish()
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }

    private fun handleCancelOrBack() {
        if (isUnlocked) return
        redirectToHome()
    }

    private fun resolveAppName(pkg: String): String {
        if (pkg.isBlank()) return "the app"
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            "the app"
        }
    }

    private fun redirectToHome() {
        if (isRedirectingToHome) return
        isRedirectingToHome = true

        // 1. Immediately revoke session for target package so it stays locked
        if (targetPackage.isNotBlank()) {
            AppLockerManager.lockPackage(targetPackage)
        }
        OscellionAppLockService.resetInterception()

        // 2. Perform system-level Home via Accessibility Service (instant 0ms OS navigation)
        OscellionAppLockService.performHome()

        // 3. Always dispatch standard Android HOME intent with NEW_TASK as authoritative fallback
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            startActivity(homeIntent)
        } catch (_: Exception) {}

        // 4. Safely delay finish until Home has taken over to prevent revealing the underlying locked app
        window.decorView.postDelayed({
            if (!isUnlocked && !isFinishing) {
                finish()
                overridePendingTransition(0, 0)
            }
        }, 350)
    }

    override fun onStop() {
        super.onStop()
        // If the user navigates away or Home takes the foreground without unlocking,
        // finish this lock shield activity so it doesn't linger in memory
        if (!isUnlocked && !isFinishing) {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleCancelOrBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package_name"

        fun launch(context: Context, packageName: String) {
            val intent = Intent(context, AppLockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(intent)
        }
    }
}
