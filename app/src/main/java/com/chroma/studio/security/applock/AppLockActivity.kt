package com.chroma.studio.security.applock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.chroma.studio.ui.components.OscellionLockScreen
import com.chroma.studio.ui.theme.OscellionTheme
import kotlinx.coroutines.delay

/**
 * Instant Shield Activity for Oscellion App Locker.
 * Unlock grants session access; back / dismiss sends the user to the Home screen
 * (never back to the locked app).
 */
class AppLockActivity : ComponentActivity() {

    private var targetPackage: String = ""
    private var targetAppName by mutableStateOf("the app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Opaque from the very first frame so the locked app never shows through.
        window.setBackgroundDrawable(ColorDrawable(0xFF030407.toInt()))

        applyIntent(intent)

        setContent {
            OscellionTheme(darkTheme = true) {
                BackHandler(enabled = true) { redirectToHome() }

                // Remove the service's instant cover only after our first frame is up.
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    delay(60)
                    OscellionAppLockService.hideCover()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    OscellionLockScreen(
                        titleText = if (targetAppName != "the app") "Unlock $targetAppName" else "Unlock the app",
                        onBack = { redirectToHome() },
                        onUnlockSuccess = {
                            if (targetPackage.isNotBlank()) {
                                AppLockerManager.markPackageUnlocked(targetPackage)
                            }
                            AppLockerManager.shieldPackage = null
                            OscellionAppLockService.hideCover()
                            finish()
                        }
                    )
                }
            }
        }
    }

    // singleInstance: a second launch (another locked app) arrives here, not in onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        targetPackage = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        targetAppName = resolveAppName(targetPackage)
        AppLockerManager.shieldPackage = targetPackage.ifBlank { null }
    }

    override fun onDestroy() {
        if (AppLockerManager.shieldPackage == targetPackage) {
            AppLockerManager.shieldPackage = null
        }
        super.onDestroy()
    }

    private fun resolveAppName(pkg: String): String {
        if (pkg.isBlank()) return "the app"
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            "the app"
        }
    }

    private fun redirectToHome() {
        AppLockerManager.shieldPackage = null
        OscellionAppLockService.performHome()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(homeIntent)
        } catch (_: Exception) {}
        finish()
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
                // singleInstance already guarantees one instance; CLEAR_TOP is unnecessary.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(intent)
        }
    }
}
