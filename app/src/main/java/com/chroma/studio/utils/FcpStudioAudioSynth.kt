package com.chroma.studio.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.chroma.studio.ui.components.FcpStudioSfxItem
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared math & procedural audio utility for Flash / FinalCutPro Video Editor.
 *
 * Guarantees exact 1-to-1 parity between live Compose/ExoPlayer preview and FFmpeg export,
 * and synthesizes real playable WAV sound effects for the Studio SFX Soundboard.
 */
object FcpStudioAudioSynth {
    private const val TAG = "FcpStudioAudioSynth"
    private const val SAMPLE_RATE = 44100

    @Volatile
    private var previewPlayer: MediaPlayer? = null

    /**
     * Exact exposure brightness offset (0..255 scale) shared by Android ColorMatrix preview
     * and FFmpeg `eq=brightness=...` export filter.
     */
    fun exposureOffset255(exposure: Float): Float = exposure.coerceIn(-100f, 100f) * 1.4f

    /**
     * Exact FFmpeg `eq=brightness=...` normalized shift `[-1.0, 1.0]` matching preview.
     */
    fun ffmpegExposureBrightness(exposure: Float): Float =
        (exposureOffset255(exposure) / 255f).coerceIn(-0.9f, 0.9f)

    /**
     * Computes exact preview-to-export text/overlay coordinate scale factor.
     * Matches Compose dp units inside the actual preview viewport width instead of assuming 360dp.
     */
    fun computeExportTextScale(exportWidthPx: Int, previewWidthPx: Int, displayDensity: Float): Float {
        val safeDensity = displayDensity.coerceAtLeast(1f)
        val previewWidthDp = if (previewWidthPx > 0) {
            (previewWidthPx.toFloat() / safeDensity).coerceAtLeast(120f)
        } else {
            360f
        }
        return exportWidthPx.toFloat() / previewWidthDp
    }

    /**
     * Parses duration seconds for a built-in SFX preset string like `"0.8s"`.
     */
    fun parseSfxDurationSec(durationText: String): Float {
        return durationText.trim().removeSuffix("s").toFloatOrNull()?.coerceIn(0.2f, 5.0f) ?: 0.8f
    }

    /**
     * Generates (or reuses cached) 16-bit PCM WAV file for a Studio SFX item.
     */
    fun getOrCreateSfxWav(context: Context, sfx: FcpStudioSfxItem): File? {
        val outFile = File(context.cacheDir, "fcp_sfx_${sfx.id}.wav")
        if (outFile.exists() && outFile.length() > 44L) {
            return outFile
        }
        val durationSec = parseSfxDurationSec(sfx.durationText)
        val numSamples = (SAMPLE_RATE * durationSec).roundToInt().coerceAtLeast(1)
        val pcm = ShortArray(numSamples)
        val rng = Random(sfx.id.hashCode())

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE.toFloat()
            val norm = i.toFloat() / numSamples.toFloat()
            val sample: Float = when (sfx.id) {
                "whoosh" -> {
                    val env = sin(norm * PI).toFloat()
                    val carrier = sin(2.0 * PI * (180.0 + 520.0 * norm) * t).toFloat() * 0.25f
                    val noise = (rng.nextFloat() * 2f - 1f) * 0.65f
                    (carrier + noise) * env * 0.7f
                }
                "camera" -> {
                    val click1 = exp(-t * 95f) * sin(2.0 * PI * 1400.0 * t).toFloat()
                    val click2 = if (t > 0.08f) exp(-(t - 0.08f) * 110f) * sin(2.0 * PI * 950.0 * (t - 0.08f)).toFloat() else 0f
                    val mech = exp(-t * 60f) * (rng.nextFloat() * 2f - 1f) * 0.45f
                    ((click1 + click2) * 0.65f + mech) * 0.85f
                }
                "sub_hit" -> {
                    val env = exp(-t * 3.8f) * (1f - exp(-t * 140f))
                    val sweepHz = 110f * exp(-t * 4.5f) + 42f
                    val punch = sin(2.0 * PI * sweepHz * t).toFloat()
                    val overtone = sin(2.0 * PI * sweepHz * 2.0 * t).toFloat() * 0.2f * exp(-t * 12f)
                    (punch + overtone) * env * 0.9f
                }
                "chime" -> {
                    val env = exp(-t * 4.2f) * (1f - exp(-t * 180f))
                    val f1 = sin(2.0 * PI * sfx.toneHz * t).toFloat() * 0.65f
                    val f2 = sin(2.0 * PI * (sfx.toneHz * 1.5) * t).toFloat() * 0.25f
                    val f3 = sin(2.0 * PI * (sfx.toneHz * 2.0) * t).toFloat() * 0.12f
                    (f1 + f2 + f3) * env * 0.8f
                }
                "vinyl" -> {
                    val hum = sin(2.0 * PI * 110.0 * t).toFloat() * 0.08f
                    val crackle = if (rng.nextFloat() > 0.97f) (rng.nextFloat() * 2f - 1f) * 0.7f else (rng.nextFloat() * 2f - 1f) * 0.06f
                    val env = if (norm < 0.05f) norm / 0.05f else if (norm > 0.9f) (1f - norm) / 0.1f else 1f
                    (hum + crackle) * env * 0.75f
                }
                else -> { // pop
                    val env = exp(-t * 22f) * (1f - exp(-t * 220f))
                    val freq = 420f + 460f * exp(-t * 18f)
                    sin(2.0 * PI * freq * t).toFloat() * env * 0.85f
                }
            }
            pcm[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        }

        return try {
            writeWav16BitMono(outFile, pcm, SAMPLE_RATE)
            outFile
        } catch (t: Throwable) {
            Log.e(TAG, "Failed writing SFX wav ${sfx.id}", t)
            null
        }
    }

    /**
     * Plays an audible live preview of the synthesized Studio SFX using Android MediaPlayer.
     */
    fun previewSfx(context: Context, sfx: FcpStudioSfxItem): Boolean {
        val wavFile = getOrCreateSfxWav(context, sfx) ?: return false
        return try {
            previewPlayer?.let { p ->
                try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
                try { p.release() } catch (_: Throwable) {}
            }
            previewPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(wavFile))
                setOnCompletionListener { mp ->
                    try { mp.release() } catch (_: Throwable) {}
                    if (previewPlayer === mp) previewPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    try { mp.release() } catch (_: Throwable) {}
                    if (previewPlayer === mp) previewPlayer = null
                    true
                }
                prepare()
                start()
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "SFX preview playback error: ${t.message}")
            false
        }
    }

    private fun writeWav16BitMono(outFile: File, pcm: ShortArray, sampleRate: Int) {
        val dataBytes = pcm.size * 2
        val byteRate = sampleRate * 2
        val header = ByteArray(44)

        // "RIFF"
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, 36 + dataBytes)
        // "WAVE"
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // Subchunk1Size
        writeShortLE(header, 20, 1) // PCM
        writeShortLE(header, 22, 1) // Mono
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, 2) // BlockAlign
        writeShortLE(header, 34, 16) // BitsPerSample
        // "data"
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, dataBytes)

        FileOutputStream(outFile).use { fos ->
            fos.write(header)
            val dataBuf = ByteArray(dataBytes)
            for (i in pcm.indices) {
                val s = pcm[i].toInt()
                dataBuf[i * 2] = (s and 0xFF).toByte()
                dataBuf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
            }
            fos.write(dataBuf)
        }
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, v: Int) {
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((v shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, v: Int) {
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
    }
}
