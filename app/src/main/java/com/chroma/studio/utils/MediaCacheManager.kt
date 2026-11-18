package com.chroma.studio.utils

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object MediaCacheManager {
    private var cache: SimpleCache? = null

    @Synchronized
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getInstance(context: Context): SimpleCache {
        if (cache == null) {
            val appCtx = context.applicationContext
            val cacheDir = File(appCtx.cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(DevicePerformanceManager.mediaCacheBytes(appCtx))
            val databaseProvider = StandaloneDatabaseProvider(appCtx)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }
}
