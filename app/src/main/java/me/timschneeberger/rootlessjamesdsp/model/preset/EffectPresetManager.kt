package me.timschneeberger.rootlessjamesdsp.model.preset

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * Manages user-created per-effect presets.
 * Each preset is stored as a JSON file in: files/effect_presets/<namespace>/<preset_name>.json
 */
object EffectPresetManager {

    @Serializable
    data class EffectPresetData(
        val name: String,
        val namespace: String,
        val values: Map<String, String>
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun presetsDir(context: Context, namespace: String): File {
        val dir = File(context.filesDir, "effect_presets/$namespace")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** List all user-created preset names for a given effect namespace */
    fun listPresets(context: Context, namespace: String): List<String> {
        val dir = presetsDir(context, namespace)
        return dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    /** Save the current SharedPreferences for the given namespace as a user preset */
    fun savePreset(context: Context, namespace: String, presetName: String): Boolean {
        try {
            val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
            val values = mutableMapOf<String, String>()
            for ((key, value) in prefs.all) {
                values[key] = value.toString()
            }
            val data = EffectPresetData(presetName, namespace, values)
            val file = File(presetsDir(context, namespace), "$presetName.json")
            file.writeText(json.encodeToString(data))
            Timber.d("Saved effect preset '$presetName' for namespace '$namespace'")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save effect preset '$presetName'")
            return false
        }
    }

    /** Load a user preset and apply it to the given SharedPreferences namespace */
    fun loadPreset(context: Context, namespace: String, presetName: String): Boolean {
        try {
            val file = File(presetsDir(context, namespace), "$presetName.json")
            if (!file.exists()) return false
            val data = json.decodeFromString<EffectPresetData>(file.readText())
            applyValues(context, namespace, data.values)
            Timber.d("Loaded effect preset '$presetName' for namespace '$namespace'")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load effect preset '$presetName'")
            return false
        }
    }

    /** Delete a user preset */
    fun deletePreset(context: Context, namespace: String, presetName: String): Boolean {
        val file = File(presetsDir(context, namespace), "$presetName.json")
        return if (file.exists()) {
            file.delete().also {
                Timber.d("Deleted effect preset '$presetName' for namespace '$namespace'")
            }
        } else false
    }

    /** Apply a map of key → value strings to the SharedPreferences of the given namespace */
    private fun applyValues(context: Context, namespace: String, values: Map<String, String>) {
        val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for ((key, value) in values) {
            putSmartValue(editor, key, value)
        }
        editor.apply()
    }

    /** Intelligently puts a value into SharedPreferences based on its string representation */
    private fun putSmartValue(editor: SharedPreferences.Editor, key: String, value: String) {
        when {
            isBooleanKey(key, value) -> editor.putBoolean(key, value.toBoolean())
            isFloatKey(key) -> editor.putFloat(key, value.toFloatOrNull() ?: 0f)
            else -> editor.putString(key, value)
        }
    }

    private fun isBooleanKey(key: String, value: String): Boolean {
        return key.endsWith("_enable") || value == "true" || value == "false"
    }

    private fun isFloatKey(key: String): Boolean {
        return key == "bass_max_gain" || key == "tube_drive" || key == "stereowide_mode" ||
                key == "compander_timeconstant" || key == "compander_granularity" ||
                key == "limiter_threshold" || key == "limiter_release" || key == "output_postgain"
    }
}
