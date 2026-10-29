package com.nodus.launcher.util

import android.content.Context
import com.nodus.launcher.home.HomePolicy
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "nodus_prefs")

data class UiSettings(
    val count: Int = 8, val padding: Int = 0, val homeSpacing: Int = 20,
    val clock: Boolean = true, val date: Boolean = true, val battery: Boolean = true,
    val signal: Boolean = true, val hideStatusBar: Boolean = false, val weather: Boolean = false, val weatherText: Boolean = false,
    val music: String = "home", val calendar: String = "hub", val calendarIds: String = "*",
    val dots: Boolean = true, val dotSize: Int = 8, val dotUniform: Boolean = false,
    val dotColor: Int = 0xFFF800FF.toInt(), val font: String = "default", val fontFile: String = "",
    val homeText: Int = 16, val hubText: Int = 13
) {
    companion object {
        fun onHome(destination: String) = destination == "home" || destination == "both"
        fun inHub(destination: String) = destination == "hub" || destination == "both"
    }
}

class Prefs(context: Context) {
    private val store = context.applicationContext.dataStore
    private val data = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val settings = data.map { p -> UiSettings(
        count = (p[intPreferencesKey("visible_apps_count")] ?: 8).coerceAtLeast(1),
        padding = (p[intPreferencesKey("home_top_padding")] ?: 0).coerceIn(0, 400),
        homeSpacing = (p[intPreferencesKey("home_spacing")] ?: 20).coerceIn(0, 32),
        clock = p[booleanPreferencesKey("show_clock")] ?: true,
        date = p[booleanPreferencesKey("show_date")] ?: true,
        battery = p[booleanPreferencesKey("show_battery")] ?: true,
        signal = p[booleanPreferencesKey("show_signal")] ?: true,
        hideStatusBar = p[booleanPreferencesKey("hide_status_bar")] ?: false,
        weather = p[booleanPreferencesKey("show_weather")] ?: false,
        weatherText = p[booleanPreferencesKey("weather_text")] ?: false,
        music = p[stringPreferencesKey("music_destination")] ?: "home",
        calendar = p[stringPreferencesKey("calendar_destination")] ?: "hub",
        calendarIds = p[stringPreferencesKey("calendar_ids")] ?: "*",
        dots = p[booleanPreferencesKey("use_dots")] ?: true,
        dotSize = (p[intPreferencesKey("dot_size")] ?: 8).coerceIn(4, 16),
        dotUniform = p[booleanPreferencesKey("dot_uniform")] ?: false,
        dotColor = p[intPreferencesKey("dot_color")] ?: 0xFFF800FF.toInt(),
        font = p[stringPreferencesKey("font")] ?: "default",
        fontFile = p[stringPreferencesKey("font_file")] ?: "",
        homeText = (p[intPreferencesKey("home_text")] ?: 16).coerceIn(12, 28),
        hubText = (p[intPreferencesKey("hub_text")] ?: 13).coerceIn(12, 24)
    ) }
    private fun ordered(p: Preferences): List<String> = p[stringPreferencesKey("favorite_order")]
        ?.split('\n')?.filter { it.isNotBlank() }?.distinct()
        ?: p[stringSetPreferencesKey("favorites")]?.sorted().orEmpty()
    val favoriteOrder = data.map(::ordered)
    val firstRun = data.map { it[booleanPreferencesKey("first_run")] ?: true }
    suspend fun bool(key: String, value: Boolean) { store.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Int) { store.edit { it[intPreferencesKey(key)] = value } }
    suspend fun text(key: String, value: String) { store.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun setFirstRun(value: Boolean) = bool("first_run", value)
    suspend fun setVisibleAppsCount(count: Int): Boolean {
        var accepted = false
        store.edit { p ->
            if (HomePolicy.validLimit(count, ordered(p).size)) {
                p[intPreferencesKey("visible_apps_count")] = count; accepted = true
            }
        }
        return accepted
    }
    suspend fun toggleFavorite(pkg: String): Boolean {
        var accepted = false
        store.edit { p ->
            val current = ordered(p)
            val limit = p[intPreferencesKey("visible_apps_count")] ?: 8
            val next = HomePolicy.favoriteUpdate(current, pkg, limit)
            if (next != null) {
                p[stringPreferencesKey("favorite_order")] = next.joinToString("\n")
                p[stringSetPreferencesKey("favorites")] = next.toSet(); accepted = true
            }
        }
        return accepted
    }
    suspend fun reorder(ids: List<String>) {
        store.edit { p ->
            val current = ordered(p)
            if (ids.size == current.size && ids.toSet() == current.toSet()) {
                p[stringPreferencesKey("favorite_order")] = ids.joinToString("\n")
            }
        }
    }
    suspend fun reconcile(installed: Map<String, String>) {
        store.edit { p ->
            val retained = ordered(p).filter { it in installed }
            val next = if (p[stringPreferencesKey("favorite_order")] == null) {
                retained.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { installed[it].orEmpty() })
            } else retained
            p[stringPreferencesKey("favorite_order")] = next.joinToString("\n")
            p[stringSetPreferencesKey("favorites")] = next.toSet()
        }
    }
    suspend fun resetAppearance() {
        store.edit { p ->
            listOf("use_dots", "dot_uniform").forEach { p.remove(booleanPreferencesKey(it)) }
            listOf("dot_size", "dot_color", "home_text", "hub_text").forEach { p.remove(intPreferencesKey(it)) }
            listOf("font", "font_file").forEach { p.remove(stringPreferencesKey(it)) }
        }
    }
}
