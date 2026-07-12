package com.chroma.studio.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.chroma.studio.model.AnimStyle
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.model.ColorStop
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// ─── Custom type adapters so Compose types survive Gson ───

private object ColorAdapter : JsonSerializer<Color>, JsonDeserializer<Color> {
    override fun serialize(src: Color, typeOfSrc: Type, context: JsonSerializationContext) =
        context.serialize(src.value.toLong())

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
        Color(json.asLong.toULong())
}

private object OffsetAdapter : JsonSerializer<Offset>, JsonDeserializer<Offset> {
    override fun serialize(src: Offset, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonObject().also { it.addProperty("x", src.x); it.addProperty("y", src.y) }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Offset {
        val obj = json.asJsonObject
        return Offset(obj["x"].asFloat, obj["y"].asFloat)
    }
}

private val gson = GsonBuilder()
    .registerTypeAdapter(Color::class.java, ColorAdapter)
    .registerTypeAdapter(Offset::class.java, OffsetAdapter)
    .create()

private val layerListType: Type = object : TypeToken<List<GradientLayer>>() {}.type

// ─── Repository ───

class WorkRepository(context: Context) {

    private val prefs = context.getSharedPreferences("chroma_works", Context.MODE_PRIVATE)
    private val KEY_IDS = "work_ids"

    /** Load all non-deleted works, sorted newest first. */
    fun loadActiveWorks(): List<ChromaWork> {
        val ids = getIds()
        if (ids.isEmpty()) return emptyList()
        return ids.mapNotNull { id -> loadWork(id) }
            .filter { !it.isDeleted }
            .sortedByDescending { it.lastModifiedAt }
    }

    /** Load all deleted works (Trash), sorted newest first. */
    fun loadDeletedWorks(): List<ChromaWork> {
        val ids = getIds()
        if (ids.isEmpty()) return emptyList()
        return ids.mapNotNull { id -> loadWork(id) }
            .filter { it.isDeleted }
            .sortedByDescending { it.lastModifiedAt }
    }

    /** Save (create or update) a work. */
    fun save(work: ChromaWork) {
        val ids = getIds().toMutableList()
        if (!ids.contains(work.id)) ids.add(work.id)
        prefs.edit()
            .putString(KEY_IDS, ids.joinToString(","))
            .putString("work_${work.id}", gson.toJson(work))
            .apply()
    }

    /** Soft-delete a work by id (moves to trash). */
    fun softDelete(id: String) {
        val work = loadWork(id) ?: return
        save(work.copy(isDeleted = true, lastModifiedAt = System.currentTimeMillis()))
    }

    /** Restore a soft-deleted work. */
    fun restore(id: String) {
        val work = loadWork(id) ?: return
        save(work.copy(isDeleted = false, lastModifiedAt = System.currentTimeMillis()))
    }

    /** Permanently delete a work by id. */
    fun permanentDelete(id: String) {
        val ids = getIds().filter { it != id }
        prefs.edit()
            .putString(KEY_IDS, ids.joinToString(","))
            .remove("work_$id")
            .apply()
    }

    /** Serialize layers to JSON string. */
    fun serializeLayers(layers: List<GradientLayer>): String = gson.toJson(layers, layerListType)

    /** Deserialize layers from JSON string. */
    fun deserializeLayers(json: String): List<GradientLayer> {
        return try {
            gson.fromJson(json, layerListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── Private helpers ───

    private fun getIds(): List<String> {
        val raw = prefs.getString(KEY_IDS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    private fun loadWork(id: String): ChromaWork? {
        val json = prefs.getString("work_$id", null) ?: return null
        return try {
            gson.fromJson(json, ChromaWork::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
