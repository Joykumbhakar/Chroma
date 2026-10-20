package com.chroma.studio.utils

import android.content.Context
import android.net.Uri
import io.microshow.rxffmpeg.RxFFmpegInvoke
import com.chroma.studio.ui.screens.Clip
import com.chroma.studio.ui.screens.FilterType
import com.chroma.studio.ui.screens.MediaType
import com.chroma.studio.ui.screens.ProjectState
import com.chroma.studio.ui.screens.BlendMode
import com.chroma.studio.ui.screens.TextAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object FFmpegExporter {

    suspend fun exportProject(context: Context, state: ProjectState): Uri? = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "pro_export_${UUID.randomUUID()}.mp4")
        
        val inputs = mutableListOf<String>()
        val mediaClips = state.clips.filter { it.type != MediaType.TEXT && it.uri != null }
        val hasAudioMap = mutableMapOf<String, Boolean>()
        
        // Setup inputs
        mediaClips.forEach { clip ->
            val uriStr = clip.uri ?: ""
            val path = if (uriStr.startsWith("content://")) {
                val cached = File(context.cacheDir, "temp_in_${UUID.randomUUID()}.mp4")
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { input ->
                    cached.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                cached.absolutePath
            } else {
                uriStr.replace("file://", "")
            }
            
            if (clip.type == MediaType.VIDEO) {
                val hasAudio = try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(path)
                    val audioStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    retriever.release()
                    audioStr != null && audioStr == "yes"
                } catch (e: Exception) { false }
                hasAudioMap[clip.id] = hasAudio
            }
            if (clip.type == MediaType.IMAGE || clip.type == MediaType.STICKER) {
                inputs.add("-loop")
                inputs.add("1")
                // Frame rate for images
                inputs.add("-framerate")
                inputs.add("30")
            }
            inputs.add("-i")
            inputs.add(path)
        }
        
        if (inputs.isEmpty()) return@withContext null
        
        val filterGraph = StringBuilder()
        
        val mainClips = mediaClips.filter { it.trackId == 1 }.sortedBy { it.startTime }
        if (mainClips.isEmpty()) return@withContext null

        val overlays = mediaClips.filter { it.trackId != 1 }
        
        // Helper to get Adjustments filter string
        fun getAdjustmentsFilter(clip: Clip): String {
            val adj = clip.adjustments
            val b = String.format(java.util.Locale.US, "%.3f", adj.brightness)
            val c = String.format(java.util.Locale.US, "%.3f", adj.contrast)
            val s = String.format(java.util.Locale.US, "%.3f", adj.saturation)
            var eq = "eq=brightness=$b:contrast=$c:saturation=$s"
            
            if (adj.hue != 0f) {
                val h = String.format(java.util.Locale.US, "%.3f", adj.hue)
                eq += ",hue=h=$h"
            }
            
            if (adj.exposure != 0f) {
                val gamma = 1.0 - (adj.exposure * 0.4)
                val g = String.format(java.util.Locale.US, "%.3f", gamma)
                eq += ",eq=gamma=$g"
            }
            
            if (clip.filter != FilterType.NONE) {
                val filterStr = when(clip.filter) {
                    FilterType.GRAYSCALE -> ",hue=s=0"
                    FilterType.SEPIA -> ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131:0:0:0:0:1"
                    FilterType.INVERT -> ",negate"
                    FilterType.WARM -> ",colorbalance=rs=.2"
                    FilterType.COOL -> ",colorbalance=bs=.2"
                    FilterType.CINEMATIC -> ",colorchannelmixer=1.1:0:0:0:0:1:0:0:0:0:0.9:0:0:0:0:1"
                    FilterType.FADE -> ",colorchannelmixer=0.9:0:0:0:0:0.9:0:0:0:0:0.9:0:0:0:0:1"
                    FilterType.VIVID -> ",eq=saturation=1.8"
                    FilterType.MATTE -> ",colorchannelmixer=0.85:0:0:0:0:0.85:0:0:0:0:0.85:0:0:0:0:1"
                    FilterType.NEON -> ",colorchannelmixer=0.8:0.2:0:0:0:0.9:0.1:0:0.2:0:1.0:0:0:0:0:1"
                    FilterType.SUNSET -> ",colorchannelmixer=1.3:0:0:0:0:0.9:0:0:0:0:0.7:0:0:0:0:1"
                    FilterType.NOIR -> ",colorchannelmixer=0.33:0.33:0.33:0:0.33:0.33:0.33:0:0.33:0.33:0.33:0:0:0:0:1"
                    else -> ""
                }
                eq += filterStr
            }
            
            if (clip.chromaKeyEnabled) {
                val r = (clip.chromaKeyColor.red * 255).toInt()
                val g = (clip.chromaKeyColor.green * 255).toInt()
                val b = (clip.chromaKeyColor.blue * 255).toInt()
                val hexColor = String.format("%02X%02X%02X", r, g, b)
                val similarity = String.format(java.util.Locale.US, "%.3f", clip.chromaKeyIntensity)
                val spill = String.format(java.util.Locale.US, "%.3f", clip.chromaKeySpill)
                eq += ",colorkey=0x$hexColor:$similarity:$spill"
            }
            
            return eq
        }

        // 1. Process and Concat Main Clips
        val concatVideoLabels = mutableListOf<String>()
        val concatAudioLabels = mutableListOf<String>()
        
        mainClips.forEachIndexed { i, clip ->
            val inputIdx = mediaClips.indexOf(clip)
            val eq = getAdjustmentsFilter(clip)
            val sTime = String.format(java.util.Locale.US, "%.3f", clip.sourceStartTime)
            val dur = String.format(java.util.Locale.US, "%.3f", clip.duration)
            
            val vTrim = "trim=start=$sTime:duration=$dur,setpts=PTS-STARTPTS"
            val vFilter = if (eq.isNotEmpty()) "$vTrim,$eq" else vTrim
            filterGraph.append("[$inputIdx:v]$vFilter[v_main_$i];")
            concatVideoLabels.add("[v_main_$i]")
            
            if (clip.type == MediaType.VIDEO && hasAudioMap[clip.id] == true) {
                val vol = String.format(java.util.Locale.US, "%.3f", clip.volume)
                var aTrim = "atrim=start=$sTime:duration=$dur,asetpts=PTS-STARTPTS,volume=$vol"
                
                if (clip.audioFadeIn > 0f) {
                    val fadeInDur = String.format(java.util.Locale.US, "%.3f", clip.audioFadeIn)
                    aTrim += ",afade=t=in:st=0:d=$fadeInDur"
                }
                if (clip.audioFadeOut > 0f) {
                    val fadeOutStart = String.format(java.util.Locale.US, "%.3f", clip.duration - clip.audioFadeOut)
                    val fadeOutDur = String.format(java.util.Locale.US, "%.3f", clip.audioFadeOut)
                    aTrim += ",afade=t=out:st=$fadeOutStart:d=$fadeOutDur"
                }
                
                filterGraph.append("[$inputIdx:a]$aTrim[a_main_$i];")
                concatAudioLabels.add("[a_main_$i]")
            } else {
                filterGraph.append("anullsrc=channel_layout=stereo:sample_rate=44100:d=$dur[a_main_$i];")
                concatAudioLabels.add("[a_main_$i]")
            }
        }
        
        val hasMainAudio = concatAudioLabels.isNotEmpty()
        
        var currentVideoStream = ""
        var currentMainAudioStream = ""
        
        if (concatVideoLabels.size == 1) {
            // Bypass concat filter if there's only 1 clip to avoid n=1 SIGSEGV in older FFmpeg
            currentVideoStream = concatVideoLabels[0]
            currentMainAudioStream = if (hasMainAudio) concatAudioLabels[0] else ""
        } else {
            val concatFilter = java.lang.StringBuilder()
            for (i in concatVideoLabels.indices) {
                concatFilter.append(concatVideoLabels[i])
                if (hasMainAudio && i < concatAudioLabels.size) concatFilter.append(concatAudioLabels[i])
            }
            
            val aParam = if (hasMainAudio) 1 else 0
            concatFilter.append("concat=n=${concatVideoLabels.size}:v=1:a=$aParam[v_concat]")
            if (hasMainAudio) concatFilter.append("[a_concat]")
            filterGraph.append("$concatFilter;")
            
            currentVideoStream = "[v_concat]"
            currentMainAudioStream = if (hasMainAudio) "[a_concat]" else ""
        }
        
        // 2. Handle overlays with Adjustments, Scaling, Position, Blend Mode, and Timing
        var overlayIndex = 1
        overlays.filter { it.type != MediaType.AUDIO }.forEach { overlayClip ->
            val inputIdx = mediaClips.indexOf(overlayClip)
            val x = (overlayClip.transform.x).toInt()
            val y = (overlayClip.transform.y).toInt()
            val scale = String.format(java.util.Locale.US, "%.3f", overlayClip.transform.scale)
            val opacity = String.format(java.util.Locale.US, "%.3f", overlayClip.transform.opacity)
            
            val overlayEq = getAdjustmentsFilter(overlayClip)
            
            val sTime = String.format(java.util.Locale.US, "%.3f", overlayClip.sourceStartTime)
            val dur = String.format(java.util.Locale.US, "%.3f", overlayClip.duration)
            val overlayTrim = "trim=start=$sTime:duration=$dur,setpts=PTS-STARTPTS"
            
            // Apply eq, trim, and scale to overlay
            filterGraph.append("[$inputIdx:v]$overlayTrim,$overlayEq,scale=trunc(iw*$scale/2)*2:trunc(ih*$scale/2)*2[overlay_scaled$overlayIndex];")
            
            var sourceStream = "[overlay_scaled$overlayIndex]"
            if (overlayClip.transform.opacity < 1.0f) {
                filterGraph.append("${sourceStream}colorchannelmixer=aa=$opacity[overlay_alpha$overlayIndex];")
                sourceStream = "[overlay_alpha$overlayIndex]"
            }
            
            val startT = String.format(java.util.Locale.US, "%.3f", overlayClip.startTime)
            val endT = String.format(java.util.Locale.US, "%.3f", overlayClip.startTime + overlayClip.duration)
            val blendStr = "overlay=$x:$y:enable='between(t,$startT,$endT)'"
            filterGraph.append("${currentVideoStream}[$sourceStream]$blendStr[out$overlayIndex];")
            
            currentVideoStream = "[out$overlayIndex]"
            overlayIndex++
        }
        
        // 3. Handle Text Layers
        val textClips = state.clips.filter { it.type == MediaType.TEXT }
        textClips.forEachIndexed { index, textClip ->
            val text = textClip.text.replace("'", "").replace(":", "\\:").replace(",", "\\,")
            val x = "(w-text_w)/2 + ${textClip.transform.x.toInt()}"
            val y = "(h-text_h)/2 + ${textClip.transform.y.toInt()}"
            val fontSize = (60 * textClip.transform.scale).toInt()
            
            val startT = String.format(java.util.Locale.US, "%.3f", textClip.startTime)
            val endT = String.format(java.util.Locale.US, "%.3f", textClip.startTime + textClip.duration)
            
            val hexColor = String.format("%02X%02X%02X", 
                (textClip.textColor.red * 255).toInt(), 
                (textClip.textColor.green * 255).toInt(), 
                (textClip.textColor.blue * 255).toInt()
            )
            var fontColorStr = "0x${hexColor}"
            var animY = y
            
            when (textClip.textAnimation) {
                TextAnimation.FADE_UP -> {
                    animY = "($y+50-50*min((t-$startT)*2\\,1.0))"
                }
                TextAnimation.BOUNCE -> {
                    animY = "($y-50*abs(sin((t-$startT)*3.14/0.5))*max(0\\,1.0-(t-$startT)/1.5))"
                }
                TextAnimation.TYPEWRITER -> {
                    // fallback to a quick fade since native typewriter requires multi-layer filters
                    animY = y
                }
                TextAnimation.NONE -> {}
            }
            
            filterGraph.append("${currentVideoStream}drawtext=text='$text':fontfile=/system/fonts/Roboto-Regular.ttf:fontcolor=$fontColorStr:fontsize=$fontSize:x=$x:y=$animY:enable='between(t,$startT,$endT)'[text_out$index];")
            currentVideoStream = "[text_out$index]"
        }
        
        val cmdArgs = mutableListOf<String>()
        cmdArgs.add("ffmpeg")
        cmdArgs.add("-y")
        cmdArgs.addAll(inputs)
        
        var hasAudio = false
        var currentAudioStream = ""
        
        // 4. Mix Audio
        val bgAudioClips = mediaClips.filter { it.type == MediaType.AUDIO }
        if (hasMainAudio || bgAudioClips.isNotEmpty()) {
            hasAudio = true
            val amixInputs = java.lang.StringBuilder()
            var audioIndex = 0
            
            if (hasMainAudio) {
                amixInputs.append(currentMainAudioStream)
                audioIndex++
            }
            
            bgAudioClips.forEach { clip ->
                val inputIdx = mediaClips.indexOf(clip)
                val vol = String.format(java.util.Locale.US, "%.3f", clip.volume)
                val sTime = String.format(java.util.Locale.US, "%.3f", clip.sourceStartTime)
                val dur = String.format(java.util.Locale.US, "%.3f", clip.duration)
                val delayMs = (clip.startTime * 1000).toInt()
                
                var aTrim = "atrim=start=$sTime:duration=$dur,asetpts=PTS-STARTPTS,volume=$vol"
                
                if (clip.audioFadeIn > 0f) {
                    val fadeInDur = String.format(java.util.Locale.US, "%.3f", clip.audioFadeIn)
                    aTrim += ",afade=t=in:st=0:d=$fadeInDur"
                }
                if (clip.audioFadeOut > 0f) {
                    val fadeOutStart = String.format(java.util.Locale.US, "%.3f", clip.duration - clip.audioFadeOut)
                    val fadeOutDur = String.format(java.util.Locale.US, "%.3f", clip.audioFadeOut)
                    aTrim += ",afade=t=out:st=$fadeOutStart:d=$fadeOutDur"
                }
                
                aTrim += ",adelay=$delayMs|$delayMs"
                filterGraph.append("[$inputIdx:a]$aTrim[a_bg$audioIndex];")
                amixInputs.append("[a_bg$audioIndex]")
                audioIndex++
            }
            
            if (audioIndex > 1) {
                filterGraph.append("${amixInputs}amix=inputs=$audioIndex:duration=longest[a_out];")
                currentAudioStream = "[a_out]"
            } else if (audioIndex == 1) {
                currentAudioStream = if (hasMainAudio) currentMainAudioStream else "[a_bg0]"
            }
        }
        
        val finalRes = state.exportSettings.resolution
        val fps = state.exportSettings.fps
        filterGraph.append("${currentVideoStream}scale=-2:$finalRes,fps=$fps[v_final];")
        currentVideoStream = "[v_final]"

        if (filterGraph.isNotEmpty()) {
            val finalGraph = filterGraph.toString().trimEnd(';')
            cmdArgs.add("-filter_complex")
            cmdArgs.add(finalGraph)
            cmdArgs.add("-map")
            cmdArgs.add(currentVideoStream)
            if (hasAudio) {
                cmdArgs.add("-map")
                cmdArgs.add(currentAudioStream)
            }
        }
        
        cmdArgs.add("-c:v")
        cmdArgs.add(state.exportSettings.codec) // e.g. libx264 or libx265
        cmdArgs.add("-preset")
        cmdArgs.add("ultrafast")
        cmdArgs.add("-crf")
        cmdArgs.add("28")
        if (hasAudio) {
            cmdArgs.add("-c:a")
            cmdArgs.add("aac")
        }
        
        // Add duration limit to prevent infinite encoding when using -loop 1
        cmdArgs.add("-t")
        val maxDur = state.clips.maxOfOrNull { it.startTime + it.duration } ?: 0f
        cmdArgs.add(String.format(java.util.Locale.US, "%.3f", maxDur))
        
        cmdArgs.add(outputFile.absolutePath)
        
        val cmd = cmdArgs.toTypedArray()
        android.util.Log.e("FFmpegExporter", "Executing FFmpeg cmd: ${cmdArgs.joinToString(" ")}")
        
        val rc = RxFFmpegInvoke.getInstance().runCommand(cmd, null)
        if (rc == 0) {
            return@withContext Uri.fromFile(outputFile)
        }
        
        return@withContext null
    }
}
