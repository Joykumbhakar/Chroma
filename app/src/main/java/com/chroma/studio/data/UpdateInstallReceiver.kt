package com.chroma.studio.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

/**
 * Internal receiver handling Android [PackageInstaller] session commit callbacks
 * during in-app self-updates (never delegates to external browsers or file managers).
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AppUpdateManager.ACTION_INSTALL_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

        Log.d("UpdateInstallReceiver", "PackageInstaller status=$status msg=$statusMessage")
        AppUpdateManager.handlePackageInstallerStatus(
            context = context.applicationContext,
            status = status,
            statusMessage = statusMessage,
            confirmIntent = confirmIntent
        )
    }
}
