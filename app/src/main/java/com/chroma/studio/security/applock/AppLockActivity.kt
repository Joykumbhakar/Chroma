package com.chroma.studio.security.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
 * If user unlocks, grants session access; if dismissed, immediately redirects to Home screen.
 */
class AppLockActivity : ComponentActivity() {

    private var targetPackage: String = ""
    private var targetAppName: String = "the app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make window edge-to-edge transparent overlay
        WindowCompat.setDecorFitsSystemWindows(window, false)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        targetAppName = resolveAppName(targetPackage)

        setContent {
            OscellionTheme(darkTheme = true) {
                // Intercept back button -> send user to Android Home launcher
                BackHandler(enabled = true) {
                    redirectToHome()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    OscellionLockScreen(
                        titleText = if (targetAppName != "the app") "Unlock $targetAppName" else "Unlock the app",
                        onUnlockSuccess = {
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
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
        overridePendingTransition(0, 0)
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
