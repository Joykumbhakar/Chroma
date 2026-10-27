package com.chroma.studio.ai

import android.graphics.BitmapFactory
import android.util.Base64
import com.chroma.studio.model.GradientLayer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AIGenerator {
    private val gson = Gson()

    suspend fun generateGradientLayers(prompt: String, base64Image: String? = null): List<GradientLayer> = withContext(Dispatchers.IO) {
        val systemInstructionText = """
            You are an expert designer and color theorist. 
            The user wants to generate a gradient background.
            Output ONLY a raw JSON array of GradientLayer objects. Do not include markdown blocks like ```json, just the raw JSON.
            Example valid JSON output:
            [
              {
                "id": "layer_1",
                "name": "Background",
                "type": "MESH",
                "blendMode": "NORMAL",
                "opacity": 1.0,
                "visible": true,
                "animated": true,
                "blobs": [
                  {"x": 20, "y": 20, "width": 50, "height": 50, "feather": 100, "opacity": 1.0, "rotation": 0.0}
                ],
                "stops": [
                  {"id": "stop_1", "color": {"value": 4294901760}, "position": 0.0}
                ]
              }
            ]
            Important: "color": {"value": 4294901760} corresponds to Color(0xFFFF0000). Use typical ARGB int format where Alpha is FF (e.g. 4294901760 is Red).
            If the user provides an image, match its vibe, color palette, and general composition.
        """.trimIndent()

        // Initialize Firebase AI model with system instructions
        // Using the free Gemini Developer API backend (googleAI)
        val model = com.google.firebase.Firebase.ai(
            backend = com.google.firebase.ai.type.GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-1.5-flash",
            systemInstruction = content { text(systemInstructionText) }
        )

        // Build the prompt content
        val inputContent = content {
            if (base64Image != null) {
                try {
                    val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    image(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            text(prompt)
        }

        // Generate content
        val response = model.generateContent(inputContent)
        var generatedText = response.text ?: throw Exception("Empty response from Gemini")
        
        // Clean up possible markdown
        generatedText = generatedText.trim()
        if (generatedText.startsWith("```json")) {
            generatedText = generatedText.substring(7)
        }
        if (generatedText.startsWith("```")) {
            generatedText = generatedText.substring(3)
        }
        if (generatedText.endsWith("```")) {
            generatedText = generatedText.substring(0, generatedText.length - 3)
        }
        generatedText = generatedText.trim()

        val typeToken = object : com.google.gson.reflect.TypeToken<List<GradientLayer>>() {}.type
        val layers: List<GradientLayer> = gson.fromJson(generatedText, typeToken)
        
        layers
    }

    suspend fun transcribeAudio(context: android.content.Context, audioUrlOrUri: String): String = withContext(Dispatchers.IO) {
        try {
            val audioBytes = if (audioUrlOrUri.startsWith("http://") || audioUrlOrUri.startsWith("https://")) {
                java.net.URL(audioUrlOrUri).readBytes()
            } else if (audioUrlOrUri.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(audioUrlOrUri))?.readBytes() ?: ByteArray(0)
            } else if (audioUrlOrUri.startsWith("file://")) {
                java.io.File(android.net.Uri.parse(audioUrlOrUri).path ?: audioUrlOrUri).readBytes()
            } else {
                java.io.File(audioUrlOrUri).readBytes()
            }
            if (audioBytes.isEmpty()) return@withContext "Audio recording is empty."

            val model = com.google.firebase.Firebase.ai(
                backend = com.google.firebase.ai.type.GenerativeBackend.googleAI()
            ).generativeModel(
                modelName = "gemini-1.5-flash",
                systemInstruction = content { text("Transcribe this voice recording accurately into text. Return ONLY the plain transcribed text without markdown or commentary.") }
            )
            val response = model.generateContent(
                com.google.firebase.ai.type.Content(
                    role = "user",
                    parts = listOf(
                        com.google.firebase.ai.type.InlineDataPart(audioBytes, "audio/mp4"),
                        com.google.firebase.ai.type.TextPart("Transcribe this voice note accurately:")
                    )
                )
            )
            val result = response.text?.trim()
            if (!result.isNullOrBlank()) result else "Voice note (no clear speech detected)"
        } catch (e: Exception) {
            "Voice note: ${e.localizedMessage ?: "Transcription unavailable"}"
        }
    }
}
