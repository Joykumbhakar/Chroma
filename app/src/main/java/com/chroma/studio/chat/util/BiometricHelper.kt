package com.chroma.studio.chat.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handles biometric (fingerprint, face, iris) and device-credential (PIN/pattern/password)
 * authentication for App Lock, Chat Lock, Hidden Chats, and sensitive security actions.
 *
 * Security guarantees:
 *   - [onSuccess] is ONLY invoked after verified system authentication.
 *   - [onCancel] / [onError] is invoked on all cancel/error paths — callers must never bypass.
 *   - Automatically waits until the hosting [FragmentActivity] is RESUMED before showing
 *     [BiometricPrompt] so prompts are never silently dropped during transitions/splash exit.
 */
object BiometricHelper {
    private const val TAG = "BiometricHelper"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val activePromptCounter = AtomicInteger(0)

    /** True while a system biometric / device-credential prompt is currently displayed. */
    val isPromptShowing: Boolean
        get() = activePromptCounter.get() > 0

    /**
     * Returns true if the device has at least one biometric or screen-lock credential enrolled.
     */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = km?.isDeviceSecure == true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ||
                manager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ||
                manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS ||
                isDeviceSecure
        } else {
            manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS || isDeviceSecure
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
        subtitle: String = "Verify your identity to continue",
        description: String? = null,
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                authenticate(activity, title, subtitle, description, onSuccess, onCancel, onError)
            }
            return
        }

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
        val km = fragmentActivity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val hasDeviceLock = km?.isDeviceSecure == true

        // Determine the best supported authenticator configuration without crashing on OEM quirks
        var api30Authenticators = 0
        var useLegacyDeviceCredential = false
        var useLegacyBiometricOnly = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val combinedStatus = biometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            val credOnlyStatus = biometricManager.canAuthenticate(DEVICE_CREDENTIAL)
            val bioOnlyStatus = biometricManager.canAuthenticate(BIOMETRIC_WEAK)

            when {
                combinedStatus == BiometricManager.BIOMETRIC_SUCCESS -> {
                    api30Authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                }
                bioOnlyStatus == BiometricManager.BIOMETRIC_SUCCESS -> {
                    api30Authenticators = BIOMETRIC_WEAK
                }
                credOnlyStatus == BiometricManager.BIOMETRIC_SUCCESS || hasDeviceLock -> {
                    api30Authenticators = DEVICE_CREDENTIAL
                }
                combinedStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                    bioOnlyStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.w(TAG, "No biometrics or screen lock enrolled — refusing to bypass lock")
                    onError("Set up a fingerprint, face unlock, or screen lock in device Settings first")
                    return
                }
                else -> {
                    Log.w(TAG, "Biometric hardware unavailable (combined=$combinedStatus, cred=$credOnlyStatus, bio=$bioOnlyStatus)")
                    onError("Biometric authentication is unavailable on this device")
                    return
                }
            }
        } else {
            val bioStatus = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
            when {
                hasDeviceLock -> {
                    // On API 28-29, setDeviceCredentialAllowed(true) supports both biometrics and PIN/pattern/password
                    useLegacyDeviceCredential = true
                }
                bioStatus == BiometricManager.BIOMETRIC_SUCCESS -> {
                    useLegacyBiometricOnly = true
                }
                else -> {
                    Log.w(TAG, "No biometrics or screen lock enrolled (API < 30)")
                    onError("Set up a fingerprint, face unlock, or screen lock in device Settings first")
                    return
                }
            }
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (!description.isNullOrBlank()) {
            promptInfoBuilder.setDescription(description)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(api30Authenticators)
            if ((api30Authenticators and DEVICE_CREDENTIAL) == 0) {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }
        } else if (useLegacyDeviceCredential) {
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else if (useLegacyBiometricOnly) {
            promptInfoBuilder
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
        }

        val promptInfo = try {
            promptInfoBuilder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid BiometricPrompt.PromptInfo: ${e.message}", e)
            onError("Could not initialize biometric prompt: ${e.message ?: "Unknown error"}")
            return
        }

        fun launchNow() {
            if (fragmentActivity.isFinishing || fragmentActivity.isDestroyed) {
                onCancel()
                return
            }

            val executor = ContextCompat.getMainExecutor(fragmentActivity)
            var settled = false
            var destroyObserver: LifecycleEventObserver? = null

            fun releasePromptGuard() {
                if (!settled) {
                    settled = true
                    destroyObserver?.let { fragmentActivity.lifecycle.removeObserver(it) }
                    destroyObserver = null
                    activePromptCounter.updateAndGet { (it - 1).coerceAtLeast(0) }
                }
            }

            destroyObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    releasePromptGuard()
                }
            }.also { fragmentActivity.lifecycle.addObserver(it) }

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

            try {
                val prompt = BiometricPrompt(fragmentActivity, executor, callback)
                activePromptCounter.incrementAndGet()
                prompt.authenticate(promptInfo)
            } catch (e: Exception) {
                releasePromptGuard()
                Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}", e)
                onError("Could not launch authentication: ${e.message ?: "Unknown error"}")
            }
        }

        // Wait for RESUMED state so Android 14/15 never drops the prompt during activity transitions
        if (fragmentActivity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            launchNow()
        } else {
            val resumeObserver = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            source.lifecycle.removeObserver(this)
                            launchNow()
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            source.lifecycle.removeObserver(this)
                            onCancel()
                        }
                        else -> Unit
                    }
                }
            }
            fragmentActivity.lifecycle.addObserver(resumeObserver)
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
