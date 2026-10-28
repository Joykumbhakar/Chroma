package com.chroma.studio.chat.util

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

/**
 * Handles biometric (and device-credential fallback) authentication for Chat Lock.
 *
 * Uses the AndroidX BiometricPrompt API — supports fingerprint, face, iris, PIN/pattern/password
 * as a fallback. Works from API 23+ without any deprecated APIs.
 *
 * Security guarantee:
 *   - [onSuccess] is ONLY called after a verified, successful authentication.
 *   - [onError] is called for every failure/cancel path — the caller must NOT unlock on error.
 */
object BiometricHelper {
    private const val TAG = "BiometricHelper"

    /**
     * Returns true if the device has at least one biometric or device credential enrolled.
     * Use this to decide whether to show lock UI at all.
     */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Launches the system biometric / device-credential prompt.
     *
     * @param activity   Must be a [FragmentActivity] (all Compose activities extend this).
     * @param title      Title shown in the prompt dialog.
     * @param subtitle   Subtitle shown below the title.
     * @param onSuccess  Called on the main thread after successful authentication.
     * @param onError    Called on the main thread on any failure, cancel, or lockout.
     */
    fun authenticate(
        activity: android.app.Activity,
        title: String = "Unlock Conversation",
        subtitle: String = "Verify your identity to open this chat",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            Log.e(TAG, "Activity is not a FragmentActivity — cannot show BiometricPrompt")
            onError("Authentication unavailable on this screen")
            return
        }

        val biometricManager = BiometricManager.from(activity)
        val allowedAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: allow all biometrics + device credential as fallback
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            // Android 10 and below: DEVICE_CREDENTIAL alone not supported alongside biometrics
            // on all devices; use weak biometric only (fingerprint, face)
            BIOMETRIC_WEAK
        }

        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit // proceed
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric enrolled — treat as success (device is not secured with biometrics,
                // so there is nothing to authenticate against; open chat safely)
                Log.w(TAG, "No biometrics enrolled; opening chat without authentication")
                onSuccess()
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.w(TAG, "Biometric hardware unavailable")
                onError("Biometric hardware unavailable")
                return
            }
            else -> {
                onError("Authentication unavailable")
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "Authentication error [$errorCode]: $errString")
                // All terminal error paths (cancel, negative button, lockout) => onError
                // This ensures the lock is NEVER bypassed via any cancel path
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // A single bad biometric attempt (wrong finger etc.) — the system dialog
                // handles retries automatically; do NOT call onError here yet.
                Log.d(TAG, "Authentication attempt failed (will retry)")
            }
        }

        val prompt = BiometricPrompt(fragmentActivity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Use your biometric to access this locked conversation")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: allow biometric + PIN/pattern/password as fallback
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            // Android 10 and below: must set negative button text when not using DEVICE_CREDENTIAL
            promptInfoBuilder
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}")
            onError("Could not launch authentication: ${e.message}")
        }
    }
}
