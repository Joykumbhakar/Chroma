package com.chroma.studio.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
object VideoPrefetcher {
    private val inFlightUrls = ConcurrentHashMap.newKeySet<String>()

    suspend fun prefetchVideo(context: Context, url: String) {
        if (url.isBlank() || !inFlightUrls.add(url)) return
        withContext(Dispatchers.IO) {
            try {
                val appCtx = context.applicationContext
                val cache = MediaCacheManager.getInstance(appCtx)
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(8_000)

                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                val dataSource = cacheDataSourceFactory.createDataSource()
                val prefetchBytes = DevicePerformanceManager.videoPrefetchBytes(appCtx)

                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(0)
                    .setLength(prefetchBytes)
                    .build()

                val cacheWriter = CacheWriter(
                    dataSource,
                    dataSpec,
                    null,
                    null
                )
                cacheWriter.cache()
            } catch (_: Exception) {
                // Best-effort prefetch; ignore transient network/cancellation errors
            } finally {
                inFlightUrls.remove(url)
            }
        }
    }
}
