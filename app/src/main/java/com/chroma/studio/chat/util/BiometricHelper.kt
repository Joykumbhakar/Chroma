package com.chroma.studio.chat.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handles biometric (and device-credential fallback) authentication for Chat Lock.
 *
 * Uses the AndroidX BiometricPrompt API — supports fingerprint, face, iris, and
 * PIN/pattern/password fallback across Android 8–15+.
 *
 * Security guarantee:
 *   - [onSuccess] is ONLY called after a verified, successful authentication.
 *   - [onCancel] / [onError] is called for every cancel/failure path — callers must NOT unlock.
 */
object BiometricHelper {
    private const val TAG = "BiometricHelper"

    private val activePromptCounter = AtomicInteger(0)

    /** True while a system biometric / device-credential prompt is currently displayed. */
    val isPromptShowing: Boolean
        get() = activePromptCounter.get() > 0

    /**
     * Returns true if the device has at least one biometric or screen-lock credential enrolled.
     */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val bioOk = manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            bioOk || (km?.isDeviceSecure == true)
        }
    }

    /**
     * Launches the system biometric / device-credential prompt.
     *
     * @param activity    Must be a [FragmentActivity] (HomeActivity / MainActivity).
     * @param title       Title shown in the prompt dialog.
     * @param subtitle    Subtitle shown below the title.
     * @param description Optional extra description line.
     * @param onSuccess   Called on the main thread after verified authentication.
     * @param onCancel    Called on the main thread if the user cancels/dismisses the prompt.
     * @param onError     Called on the main thread on hardware/enrollment/lockout errors.
     */
    @Suppress("DEPRECATION")
    fun authenticate(
        activity: android.app.Activity,
        title: String = "Unlock Conversation",
        subtitle: String = "Verify your identity to open this chat",
        description: String? = null,
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            Log.e(TAG, "Activity ${activity::class.java.simpleName} is not a FragmentActivity — cannot show BiometricPrompt")
            onError("Biometric authentication unavailable on this screen")
            return
        }

        if (fragmentActivity.isFinishing || fragmentActivity.isDestroyed) {
            onCancel()
            return
        }

        val biometricManager = BiometricManager.from(fragmentActivity)
        var useLegacyDeviceCredential = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val allowed = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            when (biometricManager.canAuthenticate(allowed)) {
                BiometricManager.BIOMETRIC_SUCCESS -> Unit
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.w(TAG, "No biometrics or screen lock enrolled — refusing to bypass chat lock")
                    onError("Set up a fingerprint, face unlock, or screen lock in device Settings first")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.w(TAG, "Biometric hardware unavailable")
                    onError("Biometric authentication is unavailable on this device")
                    return
                }
                else -> {
                    onError("Authentication unavailable")
                    return
                }
            }
        } else {
            val bioStatus = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
            val km = fragmentActivity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val hasDeviceLock = km?.isDeviceSecure == true
            if (bioStatus == BiometricManager.BIOMETRIC_SUCCESS) {
                // Allow device credential fallback on API 28-29 if screen lock is also configured
                useLegacyDeviceCredential = hasDeviceLock
            } else if (hasDeviceLock) {
                useLegacyDeviceCredential = true
            } else {
                Log.w(TAG, "No biometrics or screen lock enrolled (API < 30)")
                onError("Set up a fingerprint, face unlock, or screen lock in device Settings first")
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        var settled = false
        fun releasePromptGuard() {
            if (!settled) {
                settled = true
                activePromptCounter.updateAndGet { (it - 1).coerceAtLeast(0) }
            }
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                releasePromptGuard()
                Log.d(TAG, "Authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                releasePromptGuard()
                Log.d(TAG, "Authentication error [$errorCode]: $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    BiometricPrompt.ERROR_LOCKOUT ->
                        onError("Too many attempts. Try again in 30 seconds.")
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                        onError("Biometrics locked. Unlock your device with PIN/password first.")
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
                        onError("Set up a fingerprint, face unlock, or screen lock in device Settings first")
                    else -> onError(errString.toString().ifBlank { "Authentication failed" })
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // A single bad biometric attempt — system dialog retries automatically.
                Log.d(TAG, "Authentication attempt failed (will retry)")
            }
        }

        val prompt = BiometricPrompt(fragmentActivity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (!description.isNullOrBlank()) {
            promptInfoBuilder.setDescription(description)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else if (useLegacyDeviceCredential) {
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            promptInfoBuilder
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
        }

        try {
            activePromptCounter.incrementAndGet()
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            releasePromptGuard()
            Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}", e)
            onError("Could not launch authentication: ${e.message ?: "Unknown error"}")
        }
    }
}

/**
 * Traverses context wrappers to find the hosting [android.app.Activity].
 * Prevents issues where Compose wraps context in ContextThemeWrapper.
 */
fun Context.findActivity(): android.app.Activity? {
    if (this is android.app.Activity) return this
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
