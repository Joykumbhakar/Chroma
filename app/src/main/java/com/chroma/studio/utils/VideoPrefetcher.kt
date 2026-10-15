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

@UnstableApi
object VideoPrefetcher {
    suspend fun prefetchVideo(context: Context, url: String) {
        withContext(Dispatchers.IO) {
            try {
                val cache = MediaCacheManager.getInstance(context)
                val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
                
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    
                val dataSource = cacheDataSourceFactory.createDataSource()
                
                // Prefetch first 2MB (2 * 1024 * 1024 bytes)
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(0)
                    .setLength(2 * 1024 * 1024L)
                    .build()
                    
                val cacheWriter = CacheWriter(
                    dataSource,
                    dataSpec,
                    null,
                    null
                )
                
                cacheWriter.cache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
