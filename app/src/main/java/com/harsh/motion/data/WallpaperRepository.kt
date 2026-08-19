package com.harsh.motion.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.harsh.motion.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "motion_prefs")

/** Persists saved wallpaper configs and app settings. Lightweight JSON in DataStore. */
class WallpaperRepository(private val context: Context) {

    private val configsKey = stringPreferencesKey("configs_json")
    private val activeKey = stringPreferencesKey("active_config_id")
    private val themeKey = stringPreferencesKey("theme_mode")

    val configs: Flow<List<WallpaperConfig>> = context.dataStore.data.map { decode(it[configsKey]) }
    val activeConfigId: Flow<String?> = context.dataStore.data.map { it[activeKey] }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun save(config: WallpaperConfig) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[configsKey]).filterNot { it.id == config.id } + config
            prefs[configsKey] = encode(list)
        }
    }

    suspend fun delete(id: String) {
        context.dataStore.edit { prefs ->
            prefs[configsKey] = encode(decode(prefs[configsKey]).filterNot { it.id == id })
        }
    }

    suspend fun setActive(id: String) {
        context.dataStore.edit { it[activeKey] = id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    private fun encode(list: List<WallpaperConfig>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("photoUri", c.photoUri)
                    .put("effects", JSONArray(c.effects.map { it.name }))
                    .put("particleStyle", c.particleStyle.name)
                    .put("intensity", c.intensity.toDouble())
                    .put("scale", c.scale.toDouble())
                    .put("offsetX", c.offsetX.toDouble())
                    .put("offsetY", c.offsetY.toDouble())
                    .put("createdAt", c.createdAt),
            )
        }
        return arr.toString()
    }

    private fun decode(json: String?): List<WallpaperConfig> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val effects = (0 until o.getJSONArray("effects").length())
                    .mapNotNull { j ->
                        runCatching { EffectType.valueOf(o.getJSONArray("effects").getString(j)) }.getOrNull()
                    }.toSet()
                WallpaperConfig(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    photoUri = o.getString("photoUri"),
                    effects = effects.ifEmpty { setOf(EffectType.TILT_PARALLAX) },
                    particleStyle = runCatching { ParticleStyle.valueOf(o.getString("particleStyle")) }
                        .getOrDefault(ParticleStyle.SPARKLE),
                    intensity = o.optDouble("intensity", 0.6).toFloat(),
                    scale = o.optDouble("scale", 1.0).toFloat(),
                    offsetX = o.optDouble("offsetX", 0.0).toFloat(),
                    offsetY = o.optDouble("offsetY", 0.0).toFloat(),
                    createdAt = o.optLong("createdAt"),
                )
            }
        }.getOrDefault(emptyList())
    }
}
