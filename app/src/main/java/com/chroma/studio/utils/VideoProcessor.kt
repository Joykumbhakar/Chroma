package com.chroma.studio.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Video/audio processing helpers for the Flash Feed.
 * Uses built-in Android [MediaExtractor], [MediaMuxer], and [MediaMetadataRetriever] APIs
 * — no additional AAR dependencies required.
 */
object VideoProcessor {

    private const val TAG = "VideoProcessor"
    private const val MAX_DURATION_MS = 30_000L

    // ── Duration ─────────────────────────────────────────────────────────────

    suspend fun getVideoDurationMs(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            dur
        } catch (e: Exception) {
            Log.e(TAG, "getVideoDurationMs failed", e)
            0L
        }
    }

    // ── Thumbnail extraction ──────────────────────────────────────────────────

    suspend fun extractThumbnail(context: Context, uri: Uri, positionMs: Long = 0L): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(
                positionMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "extractThumbnail failed", e)
            null
        }
    }

    /** Save a [Bitmap] thumbnail to a temp JPEG file and return its Uri. */
    suspend fun saveThumbnailToCache(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "flash_thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "saveThumbnailToCache failed", e)
            null
        }
    }

    // ── Trim ─────────────────────────────────────────────────────────────────

    /**
     * Trims a video to [startMs]..[endMs] (capped at 30 s total).
     * Returns the output [File], or null on failure.
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long
    ): File? = withContext(Dispatchers.IO) {
        val clampedEnd = endMs.coerceAtMost(startMs + MAX_DURATION_MS)
        val outputFile = File(context.cacheDir, "flash_trim_${System.currentTimeMillis()}.mp4")

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val trackIndexMap = mutableMapOf<Int, Int>() // extractor track → muxer track

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val muxerTrack = muxer.addTrack(format)
                    trackIndexMap[i] = muxerTrack
                }
            }

            muxer.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val buffer = ByteBuffer.allocate(1024 * 1024) // 1 MB buffer

            for ((extTrack, muxTrack) in trackIndexMap) {
                extractor.selectTrack(extTrack)
                extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                while (true) {
                    bufferInfo.offset = 0
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs > clampedEnd * 1000L) break

                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTimeUs - startMs * 1000L
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxTrack, buffer, bufferInfo)
                    extractor.advance()
                }
                extractor.unselectTrack(extTrack)
            }

            muxer.stop()
            outputFile
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "trimVideo failed", e)
            outputFile.delete()
            null
        } finally {
            extractor?.release()
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    // ── Compress ─────────────────────────────────────────────────────────────

    /**
     * Lightweight compression: re-encodes the video at reduced bitrate (default 1.5 Mbps).
     * For most user-shot videos this reduces file size 60–80 % while keeping good quality.
     */
    suspend fun compressVideo(
        context: Context,
        inputUri: Uri,
        targetBitrateKbps: Int = 1500
    ): File? = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "flash_compressed_${System.currentTimeMillis()}.mp4")
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)

            // Find video track
            var videoTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    inputFormat = fmt
                    break
                }
            }

            if (videoTrackIndex < 0 || inputFormat == null) {
                Log.e(TAG, "No video track found for compression")
                return@withContext null
            }

            val width = inputFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            val frameRate = runCatching { inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE) }.getOrDefault(30)

            // Output format at reduced bitrate, max 720p width
            val outWidth = if (width > 720) 720 else width
            val outHeight = if (width > 720) (height * 720f / width).toInt().let { if (it % 2 == 0) it else it + 1 } else height

            val outputFormat = MediaFormat.createVideoFormat("video/avc", outWidth, outHeight).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateKbps * 1000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }

            encoder = MediaCodec.createEncoderByType("video/avc")
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, inputSurface, null, 0)
            decoder.start()

            extractor.selectTrack(videoTrackIndex)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerStarted = false
            var muxerTrack = -1

            val bufferInfo = MediaCodec.BufferInfo()
            val TIMEOUT_US = 10_000L
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                // Feed extractor data into decoder
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Pull encoder output
                val outIdx = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            muxerTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        val buf = encoder.getOutputBuffer(outIdx)!!
                        if (muxerStarted && bufferInfo.size > 0) {
                            muxer.writeSampleData(muxerTrack, buf, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }

                // Pull decoder output → render to encoder surface
                val decOutIdx = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (decOutIdx >= 0) {
                    val render = bufferInfo.size > 0
                    decoder.releaseOutputBuffer(decOutIdx, render)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encoder.signalEndOfInputStream()
                    }
                }
            }

            outputFile
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "compressVideo failed — returning original", e)
            // Return null so caller falls back to the un-compressed trimmed file
            null
        } finally {
            extractor?.release()
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }

    // ── Slideshow (photos → mp4) ──────────────────────────────────────────────

    /**
     * Creates a slideshow MP4 from up to 10 [imageUris].
     * Each image is shown for [durationPerSlideMs] ms. Total capped at 30 s.
     */
    suspend fun createSlideshow(
        context: Context,
        imageUris: List<Uri>,
        durationPerSlideMs: Long = 3000L
    ): File? = withContext(Dispatchers.IO) {
        val capped = imageUris.take(10)
        val totalDurationMs = (durationPerSlideMs * capped.size).coerceAtMost(MAX_DURATION_MS)
        val actualDurationPerSlide = totalDurationMs / capped.size.coerceAtLeast(1)

        val outputFile = File(context.cacheDir, "flash_slideshow_${System.currentTimeMillis()}.mp4")
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val width = 720
            val height = 1280
            val frameRate = 30
            val bitrate = 2_000_000 // 2 Mbps for crisp stills

            val outputFormat = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }

            encoder = MediaCodec.createEncoderByType("video/avc")
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrack = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val TIMEOUT_US = 10_000L

            // Draw each image onto the encoder surface
            var presentationTimeUs = 0L
            val framesPerSlide = (frameRate * actualDurationPerSlide / 1000L).toInt().coerceAtLeast(1)

            for (imgUri in capped) {
                var bitmap: android.graphics.Bitmap? = null
                try {
                    val inputStream = context.contentResolver.openInputStream(imgUri) ?: continue
                    val rawBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (rawBitmap == null) continue

                    // Scale to fill 720×1280 with letterboxing
                    bitmap = android.graphics.Bitmap.createScaledBitmap(rawBitmap, width, height, true)
                    rawBitmap.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Slideshow: failed to decode image $imgUri", e)
                    continue
                }

                for (frame in 0 until framesPerSlide) {
                    val canvas = inputSurface.lockHardwareCanvas() ?: run {
                        // Fallback to software canvas
                        android.graphics.Canvas(android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888))
                    }
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    try {
                        inputSurface.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}

                    // Drain encoder output
                    val outIdx = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                muxerTrack = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                        outIdx >= 0 -> {
                            bufferInfo.presentationTimeUs = presentationTimeUs
                            val buf = encoder.getOutputBuffer(outIdx)!!
                            if (muxerStarted && bufferInfo.size > 0) {
                                muxer.writeSampleData(muxerTrack, buf, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIdx, false)
                        }
                    }
                    presentationTimeUs += 1_000_000L / frameRate
                }
                bitmap?.recycle()
            }

            // Signal EOS
            encoder.signalEndOfInputStream()
            while (true) {
                val outIdx = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIdx >= 0) {
                    val buf = encoder.getOutputBuffer(outIdx)!!
                    if (muxerStarted && bufferInfo.size > 0) muxer.writeSampleData(muxerTrack, buf, bufferInfo)
                    encoder.releaseOutputBuffer(outIdx, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        muxerTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            outputFile
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "createSlideshow failed", e)
            outputFile.delete()
            null
        } finally {
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }
}
