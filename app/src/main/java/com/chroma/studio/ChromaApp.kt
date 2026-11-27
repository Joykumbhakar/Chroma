@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.chroma.studio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.chroma.studio.utils.DevicePerformanceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChromaApp : Application(), ImageLoaderFactory {

    private val appInitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun newImageLoader(): ImageLoader {
        val isLowEnd = DevicePerformanceManager.isLowEndDevice(this)
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(DevicePerformanceManager.coilMemoryCachePercent(this))
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("gallery_thumbnails_cache"))
                    .maxSizeBytes(DevicePerformanceManager.coilDiskCacheBytes(this))
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(if (isLowEnd) 120 else 180)
            .build()
    }

    companion object {
        lateinit var instance: ChromaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Prime device performance tier once early
        val perfProfile = DevicePerformanceManager.getProfile(this)
        Log.i(
            "ChromaApp",
            "Device tier: ${perfProfile.totalRamMb}MB RAM, ${perfProfile.cpuCores} cores, lowEnd=${perfProfile.isLowEndDevice}, ultraLow=${perfProfile.isUltraLowRam}"
        )

        try {
            com.chroma.studio.ui.components.AppSpringPhysics.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            com.chroma.studio.ui.components.OscellionLuxuryBrandController.warmup(this)
        } catch (e: Exception) {
            Log.w("ChromaApp", "Luxury brand warmup error: ${e.message}")
        }

        try {
            // Initialize Firebase (with offline-safe default app fallback if google-services.json is absent)
            if (FirebaseApp.getApps(this).isEmpty()) {
                val initialized = FirebaseApp.initializeApp(this)
                if (initialized == null && FirebaseApp.getApps(this).isEmpty()) {
                    val fallbackOptions = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:000000000000:android:0000000000000000000000")
                        .setApiKey("AIzaSyDummyKeyForOfflineFallbackMode00000")
                        .setProjectId("oscellion-studio-local")
                        .build()
                    FirebaseApp.initializeApp(this, fallbackOptions)
                }
            }
        } catch (e: Exception) {
            Log.e("ChromaApp", "Failed to initialize Firebase", e)
        }

        if (!BuildConfig.DEBUG) {
            try {
                // Initialize Firebase App Check for secure Gemini/Vertex AI access in production releases
                val appCheck = FirebaseAppCheck.getInstance()
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            } catch (e: Exception) {
                Log.w("ChromaApp", "App Check initialization bypassed: ${e.message}")
            }
        }

        try {
            // Configure Firestore offline cache sized to the device's RAM/storage tier
            val firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                        .setSizeBytes(DevicePerformanceManager.firestoreCacheBytes(this))
                        .build()
                )
                .build()
            com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings
        } catch (e: Exception) {
            Log.w("ChromaApp", "Firestore settings error: ${e.message}")
        }

        try {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        } catch (e: Exception) {
            Log.e("ChromaApp", "Failed to register AppLifecycleObserver", e)
        }

        try {
            ChatNotificationBuilder.ensureNotificationChannel(this)
        } catch (e: Exception) {
            Log.e("ChromaApp", "Failed to create notification channel", e)
        }
        createNotificationChannel()

        // Defer disk/crypto/network-heavy startup work off the Main thread for fast cold start
        appInitScope.launch {
            try {
                com.chroma.studio.data.OnlineStatusManager.initialize()
            } catch (e: Exception) {
                Log.w("ChromaApp", "OnlineStatusManager error: ${e.message}")
            }
            try {
                com.chroma.studio.data.mutation.FollowingLocalStorageManager.preload(this@ChromaApp)
                com.chroma.studio.data.mutation.OptimisticFollowManager.initialize(this@ChromaApp)
            } catch (e: Exception) {
                Log.w("ChromaApp", "OptimisticFollowManager error: ${e.message}")
            }
            try {
                com.chroma.studio.data.FcmPushSender.init(this@ChromaApp)
                com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
            } catch (e: Exception) {
                Log.w("ChromaApp", "FCM init or token sync error: ${e.message}")
            }
            try {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    com.chroma.studio.chat.data.ChatLocalCacheManager.warmupOnStartup(this@ChromaApp, uid)
                }
            } catch (e: Exception) {
                Log.w("ChromaApp", "Chat warmup error: ${e.message}")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            evictMemoryCaches()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        evictMemoryCaches()
    }

    private fun evictMemoryCaches() {
        try {
            Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Exception) {}
        try {
            com.chroma.studio.ui.screens.FcpThumbnailMemoryCache.clear()
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "fcm_default_channel"
            val channelName = "Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Default channel for Firebase Cloud Messaging"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
