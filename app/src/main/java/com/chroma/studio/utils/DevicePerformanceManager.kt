package com.chroma.studio.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * Central hardware-tier detector and adaptive resource-budget manager so Oscellion
 * runs smoothly at 60 FPS with zero OOM crashes on 2 GB – 4 GB low-end Android devices.
 */
object DevicePerformanceManager {

    data class DeviceProfile(
        val totalRamMb: Long,
        val cpuCores: Int,
        val isLowRamFlag: Boolean,
        val isUltraLowRam: Boolean,
        val isLowEndDevice: Boolean
    )

    @Volatile
    private var cachedProfile: DeviceProfile? = null

    fun getProfile(context: Context): DeviceProfile {
        cachedProfile?.let { return it }
        synchronized(this) {
            cachedProfile?.let { return it }
            val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1024L)
            val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
            val isLowRamFlag = am?.isLowRamDevice == true
            val isUltraLowRam = isLowRamFlag || totalRamMb <= 3072L
            val isLowEndDevice = isLowRamFlag || totalRamMb <= 4096L || cpuCores <= 4

            return DeviceProfile(
                totalRamMb = totalRamMb,
                cpuCores = cpuCores,
                isLowRamFlag = isLowRamFlag,
                isUltraLowRam = isUltraLowRam,
                isLowEndDevice = isLowEndDevice
            ).also { cachedProfile = it }
        }
    }

    fun isLowEndDevice(context: Context): Boolean = getProfile(context).isLowEndDevice

    fun isUltraLowRam(context: Context): Boolean = getProfile(context).isUltraLowRam

    fun coilMemoryCachePercent(context: Context): Double {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 0.10
            p.isLowEndDevice -> 0.14
            else -> 0.20
        }
    }

    fun coilDiskCacheBytes(context: Context): Long {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 60L * 1024 * 1024
            p.isLowEndDevice -> 100L * 1024 * 1024
            else -> 180L * 1024 * 1024
        }
    }

    fun mediaCacheBytes(context: Context): Long {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 80L * 1024 * 1024
            p.isLowEndDevice -> 150L * 1024 * 1024
            else -> 250L * 1024 * 1024
        }
    }

    fun firestoreCacheBytes(context: Context): Long {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 32L * 1024 * 1024
            p.isLowEndDevice -> 48L * 1024 * 1024
            else -> 80L * 1024 * 1024
        }
    }

    fun videoPrefetchBytes(context: Context): Long {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 512L * 1024
            p.isLowEndDevice -> 768L * 1024
            else -> 1024L * 1024
        }
    }

    /**
     * Real-time GPU backdrop blur (Haze) requires RenderEffect (Android 12+) and
     * mid/high-tier GPU fill-rate; disable on low-end / older devices for 60 FPS scrolling.
     */
    fun shouldUseRealtimeBlur(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isLowEndDevice(context)
    }

    fun maxPhotoExportDim(context: Context): Int {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 1440
            p.isLowEndDevice -> 2048
            else -> 4096
        }
    }

    fun maxVideoPhotoInputDim(context: Context, targetW: Int, targetH: Int): Int {
        val p = getProfile(context)
        val cap = when {
            p.isUltraLowRam -> 1280
            p.isLowEndDevice -> 1440
            else -> 2160
        }
        return maxOf(targetW, targetH).coerceAtMost(cap).coerceAtLeast(720)
    }

    fun ffmpegThreads(context: Context): Int {
        val p = getProfile(context)
        return when {
            p.isUltraLowRam -> 2
            p.isLowEndDevice -> 3
            else -> p.cpuCores.coerceIn(2, 4)
        }
    }

    /**
     * Compact ExoPlayer LoadControl that replaces the 50-second default buffer
     * with a low-memory buffer tuned to the device tier.
     */
    @androidx.annotation.OptIn(UnstableApi::class)
    fun createOptimizedLoadControl(
        context: Context,
        forEditor: Boolean = false
    ): DefaultLoadControl {
        val p = getProfile(context)
        return when {
            forEditor && p.isLowEndDevice -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_500, 5_000, 350, 750)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            forEditor -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_000, 8_000, 400, 900)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            p.isUltraLowRam -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_000, 6_000, 500, 1_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            p.isLowEndDevice -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_500, 10_000, 600, 1_200)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            else -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(3_000, 15_000, 750, 1_500)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }
    }

    /**
     * OOM-safe two-pass URI bitmap decoder using [BitmapFactory.Options.inSampleSize].
     * Never allocates the full camera sensor image in Java heap.
     */
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        preferRgb565: Boolean = false
    ): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var inSampleSize = 1
            val safeW = reqWidth.coerceAtLeast(1)
            val safeH = reqHeight.coerceAtLeast(1)
            if (srcH > safeH || srcW > safeW) {
                val halfH = srcH / 2
                val halfW = srcW / 2
                while ((halfH / inSampleSize) >= safeH && (halfW / inSampleSize) >= safeW) {
                    inSampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = if (preferRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
