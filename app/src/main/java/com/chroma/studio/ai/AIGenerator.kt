package com.chroma.studio.ai

import com.chroma.studio.BuildConfig
import com.chroma.studio.model.GradientLayer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.json.JSONArray

object AIGenerator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generateGradientLayers(prompt: String, base64Image: String? = null): List<GradientLayer> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "dummy_api_key") {
            throw Exception("Gemini API key is missing. Please add it to local.properties.")
        }

        val url = "$API_URL?key=$apiKey"

        val systemInstruction = """
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

        val jsonPayload = JSONObject()
        val systemInstructionObj = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", systemInstruction)
                })
            }
            put("parts", partsArray)
        }
        jsonPayload.put("system_instruction", systemInstructionObj)
        
        val partsArray = JSONArray()
        
        // Add text prompt
        partsArray.put(JSONObject().apply {
            put("text", prompt)
        })

        // Add image if exists
        if (base64Image != null) {
            partsArray.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })
            })
        }
        
        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", partsArray)
            })
        }
        jsonPayload.put("contents", contentsArray)

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API Error: ${response.code} ${response.message}\n${response.body?.string()}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val responseJson = JSONObject(responseBody)
        
        val candidates = responseJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw Exception("No candidates returned from Gemini.")
        }
        
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val responseParts = content?.optJSONArray("parts")
        
        var generatedText = responseParts?.getJSONObject(0)?.optString("text", "") ?: ""
        
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
}
