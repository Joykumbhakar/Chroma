package com.chroma.studio.chat.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.util.Log

object BiometricHelper {
    private const val TAG = "BiometricHelper"

    fun canAuthenticate(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    fun authenticate(
        activity: Activity,
        title: String = "Unlock Conversation",
        subtitle: String = "Verify your biometric identity to open this chat",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Use Face ID or fingerprint to unveil locked conversation")
                    .setNegativeButton("Cancel", activity.mainExecutor) { _, _ ->
                        onError("Authentication cancelled")
                    }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    activity.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            activity.runOnUiThread { onSuccess() }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            activity.runOnUiThread {
                                if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                                    errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                                    errorCode == 13 /* BIOMETRIC_ERROR_NEGATIVE_BUTTON */) {
                                    onError("Authentication cancelled")
                                } else {
                                    fallbackToKeyguard(activity, title, subtitle, onSuccess, onError)
                                }
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                        }
                    }
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "Native BiometricPrompt failed, falling back to keyguard: ${e.message}")
            }
        }

        fallbackToKeyguard(activity, title, subtitle, onSuccess, onError)
    }

    private fun fallbackToKeyguard(
        activity: Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager != null && keyguardManager.isDeviceSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle)
            if (intent != null) {
                try {
                    activity.startActivity(intent)
                    onSuccess()
                } catch (e: Exception) {
                    onSuccess()
                }
            } else {
                onSuccess()
            }
        } else {
            onSuccess()
        }
    }
}
