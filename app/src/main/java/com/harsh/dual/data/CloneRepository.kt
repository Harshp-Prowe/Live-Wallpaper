package com.harsh.dual.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.harsh.dual.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "dual_prefs")

/**
 * Persists clone metadata and UI settings. This is only a catalogue — the actual
 * cloned app data is owned by the work profile / OS, never stored here.
 */
class CloneRepository(private val context: Context) {

    private val clonesKey = stringPreferencesKey("clones_json")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val onboardedKey = stringPreferencesKey("onboarded")
    private val appLockKey = stringPreferencesKey("app_lock")

    val clones: Flow<List<Clone>> = context.dataStore.data.map { prefs ->
        decode(prefs[clonesKey])
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] == "true" }
    val appLock: Flow<Boolean> = context.dataStore.data.map { it[appLockKey] == "true" }

    suspend fun addClone(clone: Clone) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[clonesKey]).filterNot { it.packageName == clone.packageName } + clone
            prefs[clonesKey] = encode(list)
        }
    }

    suspend fun removeClone(packageName: String) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[clonesKey]).filterNot { it.packageName == packageName }
            prefs[clonesKey] = encode(list)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it[clonesKey] = "[]" }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[onboardedKey] = "true" }
    }

    suspend fun setAppLock(enabled: Boolean) {
        context.dataStore.edit { it[appLockKey] = enabled.toString() }
    }

    private fun encode(list: List<Clone>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("pkg", c.packageName)
                    .put("label", c.label)
                    .put("createdAt", c.createdAt)
                    .put("enabled", c.enabled),
            )
        }
        return arr.toString()
    }

    private fun decode(json: String?): List<Clone> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Clone(
                    packageName = o.getString("pkg"),
                    label = o.getString("label"),
                    createdAt = o.optLong("createdAt"),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }.getOrDefault(emptyList())
    }
}
