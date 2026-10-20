package com.chroma.studio.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.chroma.studio.ui.screens.FcpAspectRatio
import com.chroma.studio.ui.screens.FcpClip
import com.chroma.studio.ui.screens.FcpCustomAudio
import com.chroma.studio.ui.screens.FcpTransitionType
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * High-performance Timeline Exporter for Final Cut Pro Video Editor.
 *
 * Handles:
 * 1. Multi-clip timelines (cut / split clips, newly imported clips, multiple media sources)
 * 2. Precision in/out trimming per clip ([FcpClip.trimStartSec] to [FcpClip.trimEndSec])
 * 3. Playback speed adjustment per clip ([FcpClip.speed])
 * 4. Hardware color adjustments (exposure, contrast, saturation)
 * 5. Seamless transitions between clips (Dip to Black, Dip to White, Dissolve, etc.)
 * 6. Uniform aspect ratio scaling and letterboxing (9:16, 16:9, 1:1, 21:9)
 * 7. Audio mixing for custom soundtracks / extracted audio
 * 8. Automatic fallback to hardware [VideoProcessor.concatVideoClips] if needed
 */
object FcpTimelineExporter {

    private const val TAG = "FcpTimelineExporter"

    suspend fun exportTimeline(
        context: Context,
        clips: List<FcpClip>,
        customAudio: FcpCustomAudio?,
        aspectRatio: FcpAspectRatio,
        res: String,
        fps: String,
        onProgress: (Float, String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            Log.e(TAG, "exportTimeline: clips list is empty")
            return@withContext null
        }

        val outputFile = File(context.cacheDir, "pro_export_${System.currentTimeMillis()}.mp4")

        try {
            onProgress(0.10f, "Analyzing timeline & ${clips.size} clip(s)...")

            // 1. Calculate target dimensions based on resolution & aspect ratio
            val baseDim = when (res.lowercase()) {
                "720p" -> 720
                "4k" -> 2160
                else -> 1080
            }
            val (targetW, targetH) = when (aspectRatio) {
                FcpAspectRatio.VERTICAL_9_16 -> Pair(baseDim, ((baseDim * 16f) / 9f).toInt())
                FcpAspectRatio.WIDESCREEN_16_9 -> Pair(((baseDim * 16f) / 9f).toInt(), baseDim)
                FcpAspectRatio.SQUARE_1_1 -> Pair(baseDim, baseDim)
                FcpAspectRatio.CINEMATIC_21_9 -> Pair(((baseDim * 21f) / 9f).toInt(), baseDim)
            }
            val finalW = (targetW / 2) * 2
            val finalH = (targetH / 2) * 2
            val targetFps = when {
                fps.contains("60") -> 60
                fps.contains("24") -> 24
                else -> 30
            }

            // 2. Resolve media files to local disk
            onProgress(0.18f, "Resolving video inputs...")
            val localFileCache = mutableMapOf<String, File>()

            fun getLocalFile(uri: Uri, isAudio: Boolean = false): File? {
                val key = uri.toString()
                localFileCache[key]?.let { return it }

                if (uri.scheme == "file") {
                    val f = File(uri.path ?: "")
                    if (f.exists()) {
                        localFileCache[key] = f
                        return f
                    }
                }

                val ext = if (isAudio) ".mp3" else ".mp4"
                val tempFile = File(context.cacheDir, "fcp_input_${UUID.randomUUID().toString().take(8)}$ext")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        localFileCache[key] = tempFile
                        return tempFile
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache uri $uri", e)
                }
                return null
            }

            // Map each clip to a local file
            val clipFiles = clips.mapIndexed { idx, clip ->
                val f = getLocalFile(clip.uri)
                if (f == null) {
                    Log.e(TAG, "Clip $idx file could not be resolved: ${clip.uri}")
                    return@withContext null
                }
                f
            }

            val customAudioFile = customAudio?.let { getLocalFile(it.uri, isAudio = true) }

            // 3. Inspect audio presence for each clip
            val hasAudioMap = clips.indices.associateWith { i ->
                val clip = clips[i]
                val f = clipFiles[i]
                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                if (isImage) {
                    false
                } else {
                    try {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(f.absolutePath)
                            val audioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                            audioStr != null && audioStr == "yes"
                        } finally {
                            try { retriever.release() } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) { false }
                }
            }

            // 4. Build FFmpeg command
            onProgress(0.30f, "Building timeline composition...")
            val cmdArgs = mutableListOf<String>()

            // Add input arguments
            clips.indices.forEach { i ->
                val clip = clips[i]
                val f = clipFiles[i]
                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                if (isImage) {
                    val dur = clip.trimmedDurationSec
                    cmdArgs.add("-loop")
                    cmdArgs.add("1")
                    cmdArgs.add("-t")
                    cmdArgs.add(String.format(Locale.US, "%.3f", dur))
                    cmdArgs.add("-framerate")
                    cmdArgs.add(targetFps.toString())
                }
                cmdArgs.add("-i")
                cmdArgs.add(f.absolutePath)
            }

            val customAudioInputIndex = if (customAudioFile != null) {
                cmdArgs.add("-i")
                cmdArgs.add(customAudioFile.absolutePath)
                clips.size
            } else null

            // Build filter graph
            val filterGraph = StringBuilder()
            val concatVideoLabels = mutableListOf<String>()
            val concatAudioLabels = mutableListOf<String>()

            clips.indices.forEach { i ->
                val clip = clips[i]
                val sTime = clip.trimStartSec.coerceAtLeast(0f)
                val eTime = if (clip.trimEndSec > sTime + 0.1f) clip.trimEndSec else (sTime + 1f)
                val dur = (eTime - sTime).coerceAtLeast(0.1f)
                val speed = clip.speed.coerceIn(0.25f, 4.0f)
                val effectiveDur = dur / speed

                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                // Video filter: trim, speed, scale, pad, fps
                val vFilter = StringBuilder()
                if (!isImage) {
                    vFilter.append(String.format(Locale.US, "trim=start=%.3f:end=%.3f,setpts=PTS-STARTPTS", sTime, eTime))
                    if (speed != 1.0f) {
                        vFilter.append(String.format(Locale.US, ",setpts=%.4f*(PTS-STARTPTS)", 1.0f / speed))
                    }
                    vFilter.append(",")
                }

                // Uniform scale and letterboxing
                vFilter.append("scale=$finalW:$finalH:force_original_aspect_ratio=decrease,")
                vFilter.append("pad=$finalW:$finalH:(ow-iw)/2:(oh-ih)/2:black,")
                vFilter.append("setsar=1,fps=$targetFps")

                // Color adjustments
                if (clip.exposure != 0f || clip.contrast != 100f || clip.saturation != 100f) {
                    val b = String.format(Locale.US, "%.3f", clip.exposure / 100f)
                    val c = String.format(Locale.US, "%.3f", clip.contrast / 100f)
                    val s = String.format(Locale.US, "%.3f", clip.saturation / 100f)
                    vFilter.append(",eq=brightness=$b:contrast=$c:saturation=$s")
                }

                // Fade transition out at end of clip if transitionToNext is set
                val transToNext = clip.transitionToNext
                val nextTransDur = clip.transitionDurationSec.coerceIn(0.1f, 1.5f)
                val halfTrans = (nextTransDur / 2f).coerceAtMost(effectiveDur / 2f)

                if (i < clips.size - 1 && transToNext != FcpTransitionType.NONE && halfTrans > 0.05f) {
                    val fadeStart = (effectiveDur - halfTrans).coerceAtLeast(0f)
                    when (transToNext) {
                        FcpTransitionType.DIP_TO_BLACK -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f", fadeStart, halfTrans))
                        }
                        FcpTransitionType.DIP_TO_WHITE, FcpTransitionType.FILM_BLOOM -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:color=white", fadeStart, halfTrans))
                        }
                        else -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f", fadeStart, halfTrans))
                        }
                    }
                }

                // Fade transition in at start of clip if previous clip had a transition
                if (i > 0) {
                    val prevTrans = clips[i - 1].transitionToNext
                    val prevTransDur = clips[i - 1].transitionDurationSec.coerceIn(0.1f, 1.5f)
                    val prevHalfTrans = (prevTransDur / 2f).coerceAtMost(effectiveDur / 2f)
                    if (prevTrans != FcpTransitionType.NONE && prevHalfTrans > 0.05f) {
                        when (prevTrans) {
                            FcpTransitionType.DIP_TO_BLACK -> {
                                vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f", prevHalfTrans))
                            }
                            FcpTransitionType.DIP_TO_WHITE, FcpTransitionType.FILM_BLOOM -> {
                                vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:color=white", prevHalfTrans))
                            }
                            else -> {
                                vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f", prevHalfTrans))
                            }
                        }
                    }
                }

                filterGraph.append("[$i:v]$vFilter[v_clip_$i];")
                concatVideoLabels.add("[v_clip_$i]")

                // Audio filter: trim, speed, or silent track
                val hasAudio = hasAudioMap[i] == true
                if (hasAudio) {
                    val aFilter = StringBuilder()
                    aFilter.append(String.format(Locale.US, "atrim=start=%.3f:end=%.3f,asetpts=PTS-STARTPTS", sTime, eTime))
                    if (speed != 1.0f) {
                        val tempo = speed.coerceIn(0.5f, 2.0f)
                        aFilter.append(String.format(Locale.US, ",atempo=%.3f", tempo))
                    }
                    filterGraph.append("[$i:a]$aFilter[a_clip_$i];")
                    concatAudioLabels.add("[a_clip_$i]")
                } else {
                    // Generate silent audio of exact matching duration
                    filterGraph.append(String.format(Locale.US, "anullsrc=channel_layout=stereo:sample_rate=44100:d=%.3f[a_clip_$i];", effectiveDur))
                    concatAudioLabels.add("[a_clip_$i]")
                }
            }

            var finalVideoStream: String
            var finalAudioStream: String

            if (clips.size == 1) {
                // Single clip timeline
                finalVideoStream = "[v_clip_0]"
                finalAudioStream = "[a_clip_0]"
            } else {
                // Multi-clip timeline: concatenate all video and audio tracks
                val nClips = clips.size
                val concatBuilder = StringBuilder()
                for (i in 0 until nClips) {
                    concatBuilder.append(concatVideoLabels[i])
                    concatBuilder.append(concatAudioLabels[i])
                }
                concatBuilder.append("concat=n=$nClips:v=1:a=1[v_concat][a_concat];")
                filterGraph.append(concatBuilder.toString())
                finalVideoStream = "[v_concat]"
                finalAudioStream = "[a_concat]"
            }

            // Handle custom soundtrack if present
            if (customAudioInputIndex != null && customAudio != null) {
                val vol = customAudio.volume.coerceIn(0f, 1f)
                val muteOrig = customAudio.muteOriginalVideo

                if (muteOrig) {
                    filterGraph.append(String.format(Locale.US, "[$customAudioInputIndex:a]volume=%.3f[a_custom];", vol))
                    finalAudioStream = "[a_custom]"
                } else {
                    filterGraph.append(String.format(Locale.US, "[$customAudioInputIndex:a]volume=%.3f[a_custom];", vol))
                    filterGraph.append("$finalAudioStream[a_custom]amix=inputs=2:duration=first:dropout_transition=2[a_mixed];")
                    finalAudioStream = "[a_mixed]"
                }
            }

            val finalGraph = filterGraph.toString().trimEnd(';')
            cmdArgs.add("-filter_complex")
            cmdArgs.add(finalGraph)
            cmdArgs.add("-map")
            cmdArgs.add(finalVideoStream)
            cmdArgs.add("-map")
            cmdArgs.add(finalAudioStream)

            // Encoding flags: ultrafast x264, standard AAC
            cmdArgs.add("-c:v")
            cmdArgs.add("libx264")
            cmdArgs.add("-preset")
            cmdArgs.add("ultrafast")
            cmdArgs.add("-crf")
            cmdArgs.add("23")
            cmdArgs.add("-pix_fmt")
            cmdArgs.add("yuv420p")

            cmdArgs.add("-c:a")
            cmdArgs.add("aac")
            cmdArgs.add("-b:a")
            cmdArgs.add("192k")

            cmdArgs.add("-y")
            cmdArgs.add(outputFile.absolutePath)

            onProgress(0.45f, "Rendering timeline & transitions...")
            val cmd = cmdArgs.toTypedArray()
            Log.d(TAG, "Executing FFmpeg: ${cmd.joinToString(" ")}")

            val returnCode = RxFFmpegInvoke.getInstance().runCommand(cmd, null)
            Log.d(TAG, "FFmpeg returned code: $returnCode")

            if (returnCode == 0 && outputFile.exists() && outputFile.length() > 0L) {
                onProgress(0.90f, "Render complete!")
                return@withContext outputFile
            } else {
                Log.w(TAG, "FFmpeg render returned $returnCode or output file empty; attempting native fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "exportTimeline encountered exception", e)
        }

        // Native fallback if FFmpeg fails:
        onProgress(0.60f, "Using hardware direct muxer...")
        try {
            val success = VideoProcessor.concatVideoClips(context, clips, outputFile)
            if (success && outputFile.exists() && outputFile.length() > 0L) {
                onProgress(0.90f, "Render complete!")
                return@withContext outputFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native fallback failed", e)
        }

        outputFile.delete()
        null
    }
}
