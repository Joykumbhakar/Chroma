package com.chroma.studio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class ChromaApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("gallery_thumbnails_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .build()
    }
    companion object {
        lateinit var instance: ChromaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
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
            // Explicitly enable and configure Firestore offline caching
            val firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L) // 100 MB cache
                .build()
            com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings
        } catch (e: Exception) {
            Log.w("ChromaApp", "Firestore settings error: ${e.message}")
        }
        
        try {
            com.chroma.studio.data.OnlineStatusManager.initialize()
        } catch (e: Exception) {
            Log.w("ChromaApp", "OnlineStatusManager error: ${e.message}")
        }
        
        createNotificationChannel()
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
