@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.chroma.studio.BuildConfig
import com.chroma.studio.HomeActivity
import com.chroma.studio.ui.components.DynamicIslandManager
import com.chroma.studio.ui.components.ToastManager
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Rocket
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class AppUpdateRelease(
    val latestVersion: String,
    val latestVersionCode: Int,
    val minSupportedVersion: String = "1.0.0",
    val minSupportedVersionCode: Int = 1,
    val forceUpdate: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val downloadUrl: String = "",
    val directDownloadUrl: String = "",
    val pageUrl: String = "https://oscellion.vercel.app/download",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val sizeLabel: String = "",
    val sha256: String = "",
    val builtDateLabel: String = "",
    val publishedAt: Long = 0L
)

/**
 * Pure JVM-testable release comparison, parsing, and notification gating rules.
 */
object AppUpdateCheckerLogic {

    private val json = Json { ignoreUnknownKeys = true }

    fun isUpdateAvailable(release: AppUpdateRelease?, currentVersionCode: Int): Boolean {
        if (release == null) return false
        if (release.latestVersionCode <= currentVersionCode) return false
        return release.directDownloadUrl.isNotBlank() ||
            release.downloadUrl.isNotBlank() ||
            release.pageUrl.isNotBlank()
    }

    fun isForceUpdateRequired(release: AppUpdateRelease?, currentVersionCode: Int): Boolean {
        if (!isUpdateAvailable(release, currentVersionCode)) return false
        val target = release ?: return false
        return target.forceUpdate || currentVersionCode < target.minSupportedVersionCode
    }

    fun shouldTriggerUpdateNotification(
        release: AppUpdateRelease?,
        currentVersionCode: Int,
        lastNotifiedVersionCode: Int,
        dismissedVersionCode: Int,
        forceCheck: Boolean = false
    ): Boolean {
        if (!isUpdateAvailable(release, currentVersionCode)) return false
        val target = release ?: return false
        if (forceCheck) return true
        if (isForceUpdateRequired(target, currentVersionCode)) return true
        return target.latestVersionCode > lastNotifiedVersionCode &&
            target.latestVersionCode != dismissedVersionCode
    }

    fun parseReleaseFromJson(rawJson: String): AppUpdateRelease? {
        return try {
            val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return null
            val latestVersion = root.str("latestVersion").ifBlank { return null }
            val latestVersionCode = root.int("latestVersionCode") ?: return null
            if (latestVersionCode <= 0) return null

            val highlights = (root["highlights"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            val downloadUrl = root.str("downloadUrl")
            val directDownloadUrl = root.str("directDownloadUrl").ifBlank { downloadUrl }

            AppUpdateRelease(
                latestVersion = latestVersion,
                latestVersionCode = latestVersionCode,
                minSupportedVersion = root.str("minSupportedVersion").ifBlank { "1.0.0" },
                minSupportedVersionCode = (root.int("minSupportedVersionCode") ?: 1).coerceAtLeast(1),
                forceUpdate = root.bool("forceUpdate"),
                title = root.str("title").ifBlank { "Oscellion v$latestVersion is available" },
                summary = root.str("summary"),
                highlights = highlights,
                downloadUrl = downloadUrl.ifBlank { directDownloadUrl },
                directDownloadUrl = directDownloadUrl,
                pageUrl = root.str("pageUrl").ifBlank { "https://oscellion.vercel.app/download" },
                fileName = root.str("fileName").ifBlank { "Oscellion-v$latestVersion-release.apk" },
                sizeBytes = root.long("sizeBytes") ?: 0L,
                sizeLabel = root.str("sizeLabel"),
                sha256 = root.str("sha256"),
                builtDateLabel = root.str("builtDateLabel"),
                publishedAt = root.long("publishedAt") ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseReleaseFromMap(data: Map<String, Any?>?): AppUpdateRelease? {
        if (data == null || data.isEmpty()) return null
        val latestVersion = (data["latestVersion"] as? String)?.trim().orEmpty()
        val latestVersionCode = (data["latestVersionCode"] as? Number)?.toInt()
            ?: (data["latestVersionCode"] as? String)?.toIntOrNull()
            ?: return null
        if (latestVersion.isBlank() || latestVersionCode <= 0) return null

        val highlights = (data["highlights"] as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val downloadUrl = (data["downloadUrl"] as? String)?.trim().orEmpty()
        val directDownloadUrl = (data["directDownloadUrl"] as? String)?.trim().orEmpty()
            .ifBlank { downloadUrl }

        return AppUpdateRelease(
            latestVersion = latestVersion,
            latestVersionCode = latestVersionCode,
            minSupportedVersion = (data["minSupportedVersion"] as? String)?.trim().orEmpty()
                .ifBlank { "1.0.0" },
            minSupportedVersionCode = ((data["minSupportedVersionCode"] as? Number)?.toInt() ?: 1)
                .coerceAtLeast(1),
            forceUpdate = (data["forceUpdate"] as? Boolean)
                ?: ((data["forceUpdate"] as? String)?.equals("true", ignoreCase = true) == true),
            title = (data["title"] as? String)?.trim().orEmpty()
                .ifBlank { "Oscellion v$latestVersion is available" },
            summary = (data["summary"] as? String)?.trim().orEmpty(),
            highlights = highlights,
            downloadUrl = downloadUrl.ifBlank { directDownloadUrl },
            directDownloadUrl = directDownloadUrl,
            pageUrl = (data["pageUrl"] as? String)?.trim().orEmpty()
                .ifBlank { "https://oscellion.vercel.app/download" },
            fileName = (data["fileName"] as? String)?.trim().orEmpty()
                .ifBlank { "Oscellion-v$latestVersion-release.apk" },
            sizeBytes = (data["sizeBytes"] as? Number)?.toLong() ?: 0L,
            sizeLabel = (data["sizeLabel"] as? String)?.trim().orEmpty(),
            sha256 = (data["sha256"] as? String)?.trim().orEmpty(),
            builtDateLabel = (data["builtDateLabel"] as? String)?.trim().orEmpty(),
            publishedAt = (data["publishedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun JsonObject.str(key: String): String {
        val el: JsonElement = this[key] ?: return ""
        return (el as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    }

    private fun JsonObject.int(key: String): Int? {
        val prim = this[key] as? JsonPrimitive ?: return null
        return prim.intOrNull ?: prim.contentOrNull?.toIntOrNull()
    }

    private fun JsonObject.long(key: String): Long? {
        val prim = this[key] as? JsonPrimitive ?: return null
        return prim.longOrNull ?: prim.contentOrNull?.toLongOrNull()
    }

    private fun JsonObject.bool(key: String): Boolean {
        val prim = this[key] as? JsonPrimitive ?: return false
        return prim.booleanOrNull ?: prim.contentOrNull?.equals("true", ignoreCase = true) ?: false
    }
}

/**
 * Coordinates automatic & manual app version checks across:
 * 1) Realtime Firestore `app_config/app_release` listener (instant notification even while app is open)
 * 2) HTTPS release manifest API (`https://oscellion.vercel.app/api/app-update`)
 * 3) Incoming FCM push payloads (`type = "app_update"`)
 *
 * Surfaces a System Notification, Dynamic Island banner, and one-tap APK downloader/installer sheet.
 */
object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val PREFS_NAME = "oscellion_app_update_prefs"
    private const val KEY_LAST_NOTIFIED_CODE = "last_notified_version_code"
    private const val KEY_DISMISSED_CODE = "dismissed_version_code"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val AUTO_CHECK_INTERVAL_MS = 5 * 60 * 1000L

    const val UPDATE_CHANNEL_ID = "oscellion_updates_v1"
    private const val UPDATE_NOTIFICATION_ID = 0x7501
    const val EXTRA_OPEN_APP_UPDATE = "extra_open_app_update"
    private const val UPDATE_API_URL = "https://oscellion.vercel.app/api/app-update"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val listenerAttached = AtomicBoolean(false)
    private var releaseListener: ListenerRegistration? = null

    private val _availableRelease = MutableStateFlow<AppUpdateRelease?>(null)
    val availableRelease: StateFlow<AppUpdateRelease?> = _availableRelease.asStateFlow()

    private val _isUpdateSheetVisible = MutableStateFlow(false)
    val isUpdateSheetVisible: StateFlow<Boolean> = _isUpdateSheetVisible.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadStatusText = MutableStateFlow<String?>(null)
    val downloadStatusText: StateFlow<String?> = _downloadStatusText.asStateFlow()

    fun startObserving(context: Context) {
        val appContext = context.applicationContext
        attachRealtimeReleaseListener(appContext)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckMs = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        if (System.currentTimeMillis() - lastCheckMs >= AUTO_CHECK_INTERVAL_MS) {
            checkForUpdates(appContext, forceCheck = false)
        }
    }

    private fun attachRealtimeReleaseListener(appContext: Context) {
        if (!listenerAttached.compareAndSet(false, true)) return
        try {
            releaseListener = FirebaseFirestore.getInstance()
                .collection("app_config")
                .document("app_release")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w(TAG, "Realtime app_release listener error: ${err.message}")
                        return@addSnapshotListener
                    }
                    val release = AppUpdateCheckerLogic.parseReleaseFromMap(snap?.data) ?: return@addSnapshotListener
                    processResolvedRelease(
                        context = appContext,
                        release = release,
                        forceCheck = false,
                        showSystemNotification = true
                    )
                }
        } catch (e: Exception) {
            listenerAttached.set(false)
            Log.w(TAG, "Unable to attach realtime release listener: ${e.message}")
        }
    }

    fun checkForUpdates(context: Context, forceCheck: Boolean = false) {
        val appContext = context.applicationContext
        if (_isChecking.value) return

        scope.launch {
            _isChecking.value = true
            try {
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                    .apply()

                val firestoreRelease = runCatching {
                    val snap = FirebaseFirestore.getInstance()
                        .collection("app_config")
                        .document("app_release")
                        .get()
                        .await()
                    AppUpdateCheckerLogic.parseReleaseFromMap(snap.data)
                }.getOrNull()

                val apiRelease = fetchReleaseFromApi()

                val bestRelease = listOfNotNull(firestoreRelease, apiRelease)
                    .maxByOrNull { it.latestVersionCode }

                if (bestRelease != null &&
                    AppUpdateCheckerLogic.isUpdateAvailable(bestRelease, BuildConfig.VERSION_CODE)
                ) {
                    processResolvedRelease(
                        context = appContext,
                        release = bestRelease,
                        forceCheck = forceCheck,
                        showSystemNotification = true
                    )
                } else {
                    _availableRelease.value = null
                    if (forceCheck) {
                        withContext(Dispatchers.Main) {
                            ToastManager.showToast("You're on the latest version (v${BuildConfig.VERSION_NAME})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}")
                if (forceCheck) {
                    withContext(Dispatchers.Main) {
                        ToastManager.showToast("Could not check for updates right now")
                    }
                }
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun handleRemotePushPayload(context: Context, data: Map<String, String>) {
        val appContext = context.applicationContext
        val release = AppUpdateCheckerLogic.parseReleaseFromMap(data)
        if (release != null) {
            processResolvedRelease(
                context = appContext,
                release = release,
                forceCheck = false,
                showSystemNotification = true
            )
        }
        checkForUpdates(appContext, forceCheck = false)
    }

    fun openUpdateSheet(context: Context? = null) {
        if (_availableRelease.value == null && context != null) {
            checkForUpdates(context, forceCheck = true)
            return
        }
        _isUpdateSheetVisible.value = true
    }

    fun dismissForVersion(context: Context, versionCode: Int) {
        _isUpdateSheetVisible.value = false
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_CODE, versionCode)
            .apply()
    }

    fun hideSheet() {
        _isUpdateSheetVisible.value = false
    }

    private fun processResolvedRelease(
        context: Context,
        release: AppUpdateRelease,
        forceCheck: Boolean,
        showSystemNotification: Boolean
    ) {
        if (!AppUpdateCheckerLogic.isUpdateAvailable(release, BuildConfig.VERSION_CODE)) {
            if (_availableRelease.value?.latestVersionCode == release.latestVersionCode) {
                _availableRelease.value = null
            }
            return
        }

        _availableRelease.value = release

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_CODE, 0)
        val dismissed = prefs.getInt(KEY_DISMISSED_CODE, 0)

        val shouldNotify = AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
            release = release,
            currentVersionCode = BuildConfig.VERSION_CODE,
            lastNotifiedVersionCode = lastNotified,
            dismissedVersionCode = dismissed,
            forceCheck = forceCheck
        )

        if (!shouldNotify) return

        prefs.edit().putInt(KEY_LAST_NOTIFIED_CODE, release.latestVersionCode).apply()

        val isForce = AppUpdateCheckerLogic.isForceUpdateRequired(release, BuildConfig.VERSION_CODE)

        if (showSystemNotification && !forceCheck) {
            postSystemUpdateNotification(context, release)
        }

        scope.launch(Dispatchers.Main) {
            DynamicIslandManager.showStatus(
                title = "Update Available • v${release.latestVersion}",
                message = release.summary.ifBlank {
                    "New Oscellion build ready (${release.sizeLabel.ifBlank { "APK" }})"
                },
                icon = Lucide.Rocket,
                actionLabel = "Update",
                durationMs = if (isForce) 15000L else 9000L,
                secondaryActionLabel = if (isForce) null else "Later",
                onSecondaryAction = if (isForce) null else ({
                    dismissForVersion(context, release.latestVersionCode)
                }),
                onAction = {
                    _isUpdateSheetVisible.value = true
                }
            )
            if (isForce || forceCheck) {
                _isUpdateSheetVisible.value = true
            }
        }
    }

    private fun fetchReleaseFromApi(): AppUpdateRelease? {
        return try {
            val request = Request.Builder()
                .url("$UPDATE_API_URL?currentVersionCode=${BuildConfig.VERSION_CODE}")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                AppUpdateCheckerLogic.parseReleaseFromJson(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureUpdateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(UPDATE_CHANNEL_ID) != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts whenever a new version of Oscellion is released"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }
        nm.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun postSystemUpdateNotification(context: Context, release: AppUpdateRelease) {
        try {
            ensureUpdateNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val openIntent = Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_APP_UPDATE, true)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(
                context,
                UPDATE_NOTIFICATION_ID,
                openIntent,
                pendingFlags
            )

            val contentText = release.summary.ifBlank {
                "Version ${release.latestVersion} (${release.sizeLabel.ifBlank { "APK" }}) is ready to install."
            }

            val smallIconRes = try {
                val resId = context.resources.getIdentifier(
                    "ic_oscellion_notification",
                    "drawable",
                    context.packageName
                )
                if (resId != 0) resId else context.applicationInfo.icon
            } catch (_: Exception) {
                context.applicationInfo.icon
            }

            val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setContentTitle(release.title.ifBlank { "Oscellion v${release.latestVersion} is available" })
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(0, "Update Now", pendingIntent)

            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post system update notification: ${e.message}")
        }
    }

    fun downloadAndInstallApk(context: Context, release: AppUpdateRelease) {
        val appContext = context.applicationContext
        val rawUrl = release.directDownloadUrl.ifBlank { release.downloadUrl }
        if (rawUrl.isBlank()) {
            openDownloadPageInBrowser(appContext, release.pageUrl)
            return
        }

        if (_downloadProgress.value != null) {
            ToastManager.showToast("Update download already in progress…")
            return
        }

        val safeFileName = release.fileName
            .ifBlank { "Oscellion-v${release.latestVersion}-release.apk" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null) {
            openDownloadPageInBrowser(appContext, release.pageUrl.ifBlank { rawUrl })
            return
        }
        val destFile = File(downloadDir, safeFileName)
        if (destFile.exists()) {
            destFile.delete()
        }

        _downloadProgress.value = 0f
        _downloadStatusText.value = "Starting download…"

        scope.launch {
            try {
                val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(rawUrl))
                    .setTitle("Oscellion v${release.latestVersion}")
                    .setDescription("Downloading app update (${release.sizeLabel.ifBlank { "APK" }})")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(
                        appContext,
                        Environment.DIRECTORY_DOWNLOADS,
                        safeFileName
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val downloadId = dm.enqueue(request)
                var finished = false
                var succeeded = false

                while (!finished) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                            )
                            val downloaded = cursor.getLong(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            )
                            val total = cursor.getLong(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            )

                            when (status) {
                                DownloadManager.STATUS_RUNNING -> {
                                    if (total > 0L) {
                                        val ratio = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                        _downloadProgress.value = ratio
                                        _downloadStatusText.value = "Downloading ${(ratio * 100).toInt()}%"
                                    } else {
                                        _downloadStatusText.value = "Downloading…"
                                    }
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    _downloadStatusText.value = "Download paused…"
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    finished = true
                                    succeeded = true
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    finished = true
                                    succeeded = false
                                }
                            }
                        } else {
                            finished = true
                        }
                    }
                    if (!finished) {
                        delay(300L)
                    }
                }

                if (!succeeded || !isValidApkFile(destFile)) {
                    _downloadProgress.value = null
                    _downloadStatusText.value = null
                    withContext(Dispatchers.Main) {
                        ToastManager.showToast("Opening Oscellion download page…")
                        openDownloadPageInBrowser(appContext, release.pageUrl.ifBlank { rawUrl })
                    }
                    return@launch
                }

                _downloadProgress.value = 1f
                _downloadStatusText.value = "Installing update…"

                withContext(Dispatchers.Main) {
                    launchApkInstaller(appContext, destFile)
                    _downloadProgress.value = null
                    _downloadStatusText.value = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Download/install failed, falling back to browser: ${e.message}")
                _downloadProgress.value = null
                _downloadStatusText.value = null
                withContext(Dispatchers.Main) {
                    ToastManager.showToast("Opening browser to download update…")
                    openDownloadPageInBrowser(appContext, release.pageUrl.ifBlank { rawUrl })
                }
            }
        }
    }

    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { input ->
                val b0 = input.read()
                val b1 = input.read()
                b0 == 0x50 && b1 == 0x4B // 'P''K' ZIP/APK magic header
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    fun openDownloadPageInBrowser(context: Context, url: String) {
        val target = url.ifBlank { "https://oscellion.vercel.app/download" }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            ToastManager.showToast("Visit $target to update")
        }
    }
}
