@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import java.io.FileOutputStream
import java.security.MessageDigest
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

sealed class AppUpdateInstallStage {
    data object Available : AppUpdateInstallStage()
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : AppUpdateInstallStage()
    data class PermissionRequired(val apkPath: String) : AppUpdateInstallStage()
    data class ReadyToInstallPopup(val apkPath: String) : AppUpdateInstallStage()
    data class Installing(val statusText: String = "Installing update…") : AppUpdateInstallStage()
    data class Error(
        val message: String,
        val downloadedApkPath: String? = null
    ) : AppUpdateInstallStage()
}

/**
 * Pure JVM-testable release comparison, parsing, hash verification, and notification gating rules.
 */
object AppUpdateCheckerLogic {

    private val json = Json { ignoreUnknownKeys = true }

    fun resolveDirectDownloadUrl(release: AppUpdateRelease?): String {
        if (release == null) return ""
        return release.directDownloadUrl.trim().ifBlank { release.downloadUrl.trim() }
    }

    fun isUpdateAvailable(release: AppUpdateRelease?, currentVersionCode: Int): Boolean {
        if (release == null) return false
        if (release.latestVersionCode <= currentVersionCode) return false
        return resolveDirectDownloadUrl(release).isNotBlank()
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

    fun matchesExpectedSha256(expectedSha256: String, actualSha256: String): Boolean {
        val expected = expectedSha256.trim()
        if (expected.isEmpty()) return true
        return expected.equals(actualSha256.trim(), ignoreCase = true)
    }

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
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
 * End-to-end self-update orchestrator without opening external browsers, file managers, or third-party installers:
 * 1) Real-time Firestore `app_config/app_release` + `/api/app-update` + FCM update push detection
 * 2) System notification + Dynamic Island alert + bottom sheet drawer (`Download` / `Download Later`)
 * 3) Direct in-app OkHttp streaming download into internal `filesDir/updates/` with PKZIP + SHA-256 verification
 * 4) Unknown-sources install permission check (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`) & auto-resume on return
 * 5) In-app "Install" confirmation pop-up committing directly to Android [PackageInstaller]
 */
object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val PREFS_NAME = "oscellion_app_update_prefs"
    private const val KEY_LAST_NOTIFIED_CODE = "last_notified_version_code"
    private const val KEY_DISMISSED_CODE = "dismissed_version_code"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val KEY_PENDING_APK_PATH = "pending_apk_path"
    private const val KEY_PENDING_APK_CODE = "pending_apk_code"
    private const val AUTO_CHECK_INTERVAL_MS = 5 * 60 * 1000L

    const val UPDATE_CHANNEL_ID = "oscellion_updates_v1"
    private const val UPDATE_NOTIFICATION_ID = 0x7501
    const val EXTRA_OPEN_APP_UPDATE = "extra_open_app_update"
    const val ACTION_INSTALL_STATUS = "com.chroma.studio.action.APP_UPDATE_INSTALL_STATUS"
    private const val UPDATE_API_URL = "https://oscellion.vercel.app/api/app-update"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val apiHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val downloadHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val listenerAttached = AtomicBoolean(false)
    private var releaseListener: ListenerRegistration? = null
    private var activeDownloadJob: Job? = null

    private val _availableRelease = MutableStateFlow<AppUpdateRelease?>(null)
    val availableRelease: StateFlow<AppUpdateRelease?> = _availableRelease.asStateFlow()

    private val _isUpdateSheetVisible = MutableStateFlow(false)
    val isUpdateSheetVisible: StateFlow<Boolean> = _isUpdateSheetVisible.asStateFlow()

    private val _installStage = MutableStateFlow<AppUpdateInstallStage>(AppUpdateInstallStage.Available)
    val installStage: StateFlow<AppUpdateInstallStage> = _installStage.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    fun startObserving(context: Context) {
        val appContext = context.applicationContext
        cleanupObsoleteDownloads(appContext)
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
                    val release = AppUpdateCheckerLogic.parseReleaseFromMap(snap?.data)
                        ?: return@addSnapshotListener
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
        val currentRelease = _availableRelease.value
        if (currentRelease == null && context != null) {
            checkForUpdates(context, forceCheck = true)
            return
        }
        if (currentRelease != null && context != null) {
            syncStageWithCachedApk(context.applicationContext, currentRelease)
        }
        _isUpdateSheetVisible.value = true
    }

    fun dismissForVersion(context: Context, versionCode: Int) {
        _isUpdateSheetVisible.value = false
        if (_installStage.value !is AppUpdateInstallStage.Downloading &&
            _installStage.value !is AppUpdateInstallStage.Installing
        ) {
            _installStage.value = AppUpdateInstallStage.Available
        }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_CODE, versionCode)
            .apply()
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _installStage.value = AppUpdateInstallStage.Available
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun buildUnknownSourcesPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Called from HomeActivity `onResume()` or `ActivityResultLauncher` after user returns
     * from Settings ("Allow from this source"). If APK is already downloaded & permission is
     * now granted, immediately displays the in-app "Install" confirmation pop-up.
     */
    fun onUnknownSourcesPermissionReturned(context: Context) {
        val appContext = context.applicationContext
        val release = _availableRelease.value ?: return
        val stage = _installStage.value
        val cachedApk = findCachedVerifiedApk(appContext, release) ?: return
        if (canRequestPackageInstalls(appContext)) {
            if (stage is AppUpdateInstallStage.PermissionRequired ||
                stage is AppUpdateInstallStage.Available
            ) {
                _installStage.value = AppUpdateInstallStage.ReadyToInstallPopup(cachedApk.absolutePath)
                _isUpdateSheetVisible.value = true
            }
        }
    }

    fun startInAppDownload(context: Context, release: AppUpdateRelease) {
        val appContext = context.applicationContext

        // If already downloaded & verified on disk, skip network download and advance directly!
        val cached = findCachedVerifiedApk(appContext, release)
        if (cached != null) {
            advanceAfterDownloadVerified(appContext, release, cached)
            return
        }

        val url = AppUpdateCheckerLogic.resolveDirectDownloadUrl(release)
        if (url.isBlank()) {
            _installStage.value = AppUpdateInstallStage.Error("Update download link is unavailable.")
            return
        }

        if (_installStage.value is AppUpdateInstallStage.Downloading) return

        val updatesDir = File(appContext.filesDir, "updates").apply { mkdirs() }
        val targetFile = apkFileForVersion(updatesDir, release.latestVersionCode)
        val partFile = File(updatesDir, "${targetFile.name}.part")
        if (partFile.exists()) partFile.delete()
        if (targetFile.exists()) targetFile.delete()

        _installStage.value = AppUpdateInstallStage.Downloading(
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = release.sizeBytes
        )

        activeDownloadJob = scope.launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Oscellion-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                downloadHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Download failed (HTTP ${response.code})")
                    }
                    val body = response.body
                        ?: throw IllegalStateException("Empty update response")

                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    if (contentType.contains("text/html")) {
                        throw IllegalStateException("Download link returned HTML instead of an APK package.")
                    }

                    val expectedTotal = body.contentLength().takeIf { it > 0L } ?: release.sizeBytes
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(32 * 1024)
                    var totalRead = 0L
                    var lastEmitMs = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(partFile).use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                totalRead += read

                                val now = System.currentTimeMillis()
                                if (now - lastEmitMs >= 120L) {
                                    lastEmitMs = now
                                    val ratio = if (expectedTotal > 0L) {
                                        (totalRead.toFloat() / expectedTotal.toFloat()).coerceIn(0f, 0.99f)
                                    } else {
                                        0f
                                    }
                                    _installStage.value = AppUpdateInstallStage.Downloading(
                                        progress = ratio,
                                        bytesDownloaded = totalRead,
                                        totalBytes = expectedTotal
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (!isValidApkFile(partFile)) {
                        partFile.delete()
                        throw IllegalStateException("Downloaded file is not a valid Android APK.")
                    }

                    val computedSha = digest.digest().joinToString("") { "%02X".format(it) }
                    if (!AppUpdateCheckerLogic.matchesExpectedSha256(release.sha256, computedSha)) {
                        partFile.delete()
                        throw IllegalStateException("APK integrity verification (SHA-256) failed.")
                    }

                    if (!partFile.renameTo(targetFile)) {
                        partFile.copyTo(targetFile, overwrite = true)
                        partFile.delete()
                    }

                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_PENDING_APK_PATH, targetFile.absolutePath)
                        .putInt(KEY_PENDING_APK_CODE, release.latestVersionCode)
                        .apply()

                    withContext(Dispatchers.Main) {
                        advanceAfterDownloadVerified(appContext, release, targetFile)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (partFile.exists()) partFile.delete()
                _installStage.value = AppUpdateInstallStage.Available
            } catch (e: Exception) {
                Log.w(TAG, "In-app APK download failed: ${e.message}")
                if (partFile.exists()) partFile.delete()
                _installStage.value = AppUpdateInstallStage.Error(
                    message = e.message ?: "Download failed. Check your connection and retry."
                )
            }
        }
    }

    private fun advanceAfterDownloadVerified(
        context: Context,
        release: AppUpdateRelease,
        apkFile: File
    ) {
        if (!canRequestPackageInstalls(context)) {
            _installStage.value = AppUpdateInstallStage.PermissionRequired(apkFile.absolutePath)
        } else {
            _installStage.value = AppUpdateInstallStage.ReadyToInstallPopup(apkFile.absolutePath)
        }
    }

    /**
     * Commits the downloaded APK directly via Android's [PackageInstaller] Session API
     * without launching external FileManager, Browser, or third-party APK installer apps.
     */
    fun commitInAppPackageInstall(context: Context, apkPath: String) {
        val appContext = context.applicationContext
        val apkFile = File(apkPath)
        if (!apkFile.exists() || !isValidApkFile(apkFile)) {
            _installStage.value = AppUpdateInstallStage.Error("Downloaded update file missing. Please re-download.")
            return
        }

        if (!canRequestPackageInstalls(appContext)) {
            _installStage.value = AppUpdateInstallStage.PermissionRequired(apkFile.absolutePath)
            return
        }

        _installStage.value = AppUpdateInstallStage.Installing("Preparing update package…")

        scope.launch {
            var session: PackageInstaller.Session? = null
            try {
                val packageInstaller = appContext.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setAppPackageName(appContext.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
                    }
                }

                val sessionId = packageInstaller.createSession(params)
                session = packageInstaller.openSession(sessionId)

                val totalBytes = apkFile.length().coerceAtLeast(1L)
                session.openWrite("oscellion-update.apk", 0, totalBytes).use { out ->
                    apkFile.inputStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            out.write(buf, 0, read)
                            copied += read
                            session.setStagingProgress((copied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    session.fsync(out)
                }

                _installStage.value = AppUpdateInstallStage.Installing("Installing update…")

                val callbackIntent = Intent(appContext, UpdateInstallReceiver::class.java).apply {
                    action = ACTION_INSTALL_STATUS
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

                val statusPendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    sessionId,
                    callbackIntent,
                    flags
                )

                session.commit(statusPendingIntent.intentSender)
                session.close()
                session = null
            } catch (e: Exception) {
                Log.e(TAG, "PackageInstaller commit failed", e)
                runCatching { session?.abandon() }
                _installStage.value = AppUpdateInstallStage.Error(
                    message = e.message ?: "Unable to start in-app installer.",
                    downloadedApkPath = apkFile.absolutePath
                )
            }
        }
    }

    internal fun handlePackageInstallerStatus(
        context: Context,
        status: Int,
        statusMessage: String,
        confirmIntent: Intent?
    ) {
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        _installStage.value = AppUpdateInstallStage.Error(
                            message = e.message ?: "Could not open install confirmation.",
                            downloadedApkPath = getCachedApkPath(context)
                        )
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                cleanupObsoleteDownloads(context, forceCleanAll = true)
                _installStage.value = AppUpdateInstallStage.Available
                _isUpdateSheetVisible.value = false
                scope.launch(Dispatchers.Main) {
                    ToastManager.showToast("Oscellion updated successfully!")
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _installStage.value = AppUpdateInstallStage.Error(
                    message = "Installation was cancelled.",
                    downloadedApkPath = getCachedApkPath(context)
                )
            }

            else -> {
                val friendly = when (status) {
                    PackageInstaller.STATUS_FAILURE_CONFLICT ->
                        "Package signature conflicts with installed build."
                    PackageInstaller.STATUS_FAILURE_STORAGE ->
                        "Not enough storage space to install this update."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "This APK is not compatible with your device."
                    PackageInstaller.STATUS_FAILURE_INVALID ->
                        "Update package is invalid. Please re-download."
                    else -> statusMessage.ifBlank { "Update installation failed (code $status)." }
                }
                _installStage.value = AppUpdateInstallStage.Error(
                    message = friendly,
                    downloadedApkPath = getCachedApkPath(context)
                )
            }
        }
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
            syncStageWithCachedApk(context, release)
            DynamicIslandManager.showStatus(
                title = "New Version Available • v${release.latestVersion}",
                message = release.summary.ifBlank {
                    "Tap Update to download inside Oscellion (${release.sizeLabel.ifBlank { "APK" }})"
                },
                icon = Lucide.Rocket,
                actionLabel = "Download",
                durationMs = if (isForce) 15000L else 9000L,
                secondaryActionLabel = if (isForce) null else "Later",
                onSecondaryAction = if (isForce) null else ({
                    dismissForVersion(context, release.latestVersionCode)
                }),
                onAction = {
                    syncStageWithCachedApk(context, release)
                    _isUpdateSheetVisible.value = true
                }
            )
            // Pop open the Bottom Sheet Drawer automatically when a new update arrives
            _isUpdateSheetVisible.value = true
        }
    }

    private fun syncStageWithCachedApk(context: Context, release: AppUpdateRelease) {
        val current = _installStage.value
        if (current is AppUpdateInstallStage.Downloading || current is AppUpdateInstallStage.Installing) {
            return
        }
        val cached = findCachedVerifiedApk(context, release)
        if (cached != null) {
            advanceAfterDownloadVerified(context, release, cached)
        } else {
            _installStage.value = AppUpdateInstallStage.Available
        }
    }

    private fun apkFileForVersion(dir: File, versionCode: Int): File =
        File(dir, "oscellion-update-c$versionCode.apk")

    private fun findCachedVerifiedApk(context: Context, release: AppUpdateRelease): File? {
        val updatesDir = File(context.filesDir, "updates")
        val candidate = apkFileForVersion(updatesDir, release.latestVersionCode)
        return if (isValidApkFile(candidate)) candidate else null
    }

    private fun getCachedApkPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_PENDING_APK_PATH, null) ?: return null
        val file = File(path)
        return if (isValidApkFile(file)) file.absolutePath else null
    }

    private fun cleanupObsoleteDownloads(context: Context, forceCleanAll: Boolean = false) {
        try {
            val updatesDir = File(context.filesDir, "updates")
            if (!updatesDir.exists()) return
            updatesDir.listFiles()?.forEach { f ->
                if (forceCleanAll || f.name.endsWith(".part")) {
                    f.delete()
                } else {
                    val code = Regex("""c(\d+)\.apk$""").find(f.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    if (code != null && code <= BuildConfig.VERSION_CODE) {
                        f.delete()
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun fetchReleaseFromApi(): AppUpdateRelease? {
        return try {
            val request = Request.Builder()
                .url("$UPDATE_API_URL?currentVersionCode=${BuildConfig.VERSION_CODE}")
                .header("Accept", "application/json")
                .build()
            apiHttpClient.newCall(request).execute().use { response ->
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
                "Version ${release.latestVersion} (${release.sizeLabel.ifBlank { "APK" }}) is ready to download."
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
                .setContentTitle(release.title.ifBlank { "New Version v${release.latestVersion} Available" })
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(0, "Download Update", pendingIntent)

            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post system update notification: ${e.message}")
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
}
