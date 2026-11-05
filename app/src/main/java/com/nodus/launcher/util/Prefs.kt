package com.nodus.launcher.util

import android.content.Context
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.home.HeaderSizes
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "nodus_prefs")

data class UiSettings(
    val terminalEnabled: Boolean = true, val terminalUser: String = "issu", val terminalDevice: String = "",
    val count: Int = 8, val padding: Int = 0, val homeSpacing: Int = 20,
    val clock: Boolean = true, val date: Boolean = true, val battery: Boolean = true,
    val signal: Boolean = true, val hideStatusBar: Boolean = false, val weather: Boolean = false, val weatherText: Boolean = false,
    val music: String = "home", val calendar: String = "hub", val calendarIds: String = "*",
    val dots: Boolean = true, val dotSize: Int = 8, val dotUniform: Boolean = false,
    val dotColor: Int = 0xFFF800FF.toInt(), val font: String = "default", val fontFile: String = "",
    val settingsText: Int = 16, val homeText: Int = 16, val hubText: Int = 13,
    val homeHubText: Int = 13, val homeHubPadding: Int = 12, val homeHubIndicator: String = "pulse",
    val compactHeader: Boolean = false, val clockSize: Int = 42, val dateSize: Int = 14,
    val weatherSize: Int = 14, val batterySize: Int = 16, val signalSize: Int = 18
) {
    companion object {
        fun onHome(destination: String) = destination == "home" || destination == "both"
        fun inHub(destination: String) = destination == "hub" || destination == "both"
    }
}

data class LaunchState(val firstRun: Boolean, val seenVersion: Int)

class Prefs(context: Context) {
    private val appContext = context.applicationContext
    private val version = context.installedVersion().code
    private val store = context.applicationContext.dataStore
    private val data = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    val settings = data.map { p -> UiSettings(
        terminalEnabled = p[booleanPreferencesKey("terminal_enabled")] ?: true,
        terminalUser = p[stringPreferencesKey("terminal_user")] ?: "issu",
        terminalDevice = p[stringPreferencesKey("terminal_device")] ?: "",
        settingsText = (p[intPreferencesKey("settings_text")] ?: 16).coerceIn(13, 24),
        homeHubText = (p[intPreferencesKey("home_hub_text")] ?: 13).coerceIn(10, 26),
        homeHubPadding = (p[intPreferencesKey("home_hub_padding")] ?: 12).coerceIn(0, 32),
        homeHubIndicator = (p[stringPreferencesKey("home_hub_indicator")] ?: "pulse").takeIf { it in setOf("off", "fixed", "pulse") } ?: "pulse",
        count = (p[intPreferencesKey("visible_apps_count")] ?: 8).coerceAtLeast(1),
        padding = (p[intPreferencesKey("home_top_padding")] ?: 0).coerceIn(0, 400),
        homeSpacing = (p[intPreferencesKey("home_spacing")] ?: 20).coerceIn(0, 32),
        compactHeader = p[booleanPreferencesKey("compact_header")] ?: false,
        clockSize = HeaderSizes.read(p[booleanPreferencesKey("compact_header")] ?: false, "clock_size") { p[intPreferencesKey(it)] },
        dateSize = HeaderSizes.read(p[booleanPreferencesKey("compact_header")] ?: false, "date_size") { p[intPreferencesKey(it)] },
        weatherSize = HeaderSizes.read(p[booleanPreferencesKey("compact_header")] ?: false, "weather_size") { p[intPreferencesKey(it)] },
        batterySize = HeaderSizes.read(p[booleanPreferencesKey("compact_header")] ?: false, "battery_size") { p[intPreferencesKey(it)] },
        signalSize = HeaderSizes.read(p[booleanPreferencesKey("compact_header")] ?: false, "signal_size") { p[intPreferencesKey(it)] },
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
    val launchState = data.map { LaunchState(it[booleanPreferencesKey("first_run")] ?: true,
        it[intPreferencesKey("last_seen_version")] ?: 0) }
    suspend fun markVersionSeen(version: Int) { store.edit { p ->
        p[intPreferencesKey("last_seen_version")] = maxOf(p[intPreferencesKey("last_seen_version")] ?: 0, version)
    } }
    val firstRun = data.map { it[booleanPreferencesKey("first_run")] ?: true }
    suspend fun bool(key: String, value: Boolean) { store.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Int) {
        store.edit { p ->
            val stored = if (key in HeaderSizes.keys) HeaderSizes.key(p[booleanPreferencesKey("compact_header")] ?: false, key) else key
            p[intPreferencesKey(stored)] = value
        }
    }
    suspend fun text(key: String, value: String) { store.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun setFirstRun(value: Boolean) {
        store.edit { p ->
            p[booleanPreferencesKey("first_run")] = value
            if (!value) p[intPreferencesKey("last_seen_version")] = maxOf(p[intPreferencesKey("last_seen_version")] ?: 0, version)
        }
    }
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
    suspend fun headerPreset(compact: Boolean) {
        store.edit { p ->
            val previous = p[booleanPreferencesKey("compact_header")] ?: false
            if (previous != compact) {
                val snapshot = HeaderSizes.snapshot(previous) { p[intPreferencesKey(it)] }
                snapshot.forEach { (key, value) -> p[intPreferencesKey(key)] = value }
                HeaderSizes.keys.forEach { p.remove(intPreferencesKey(it)) }
                p[booleanPreferencesKey("compact_header")] = compact
            }
        }
    }
    suspend fun resetHeader() {
        store.edit { p ->
            HeaderSizes.keys.forEach { name ->
                p.remove(intPreferencesKey(name))
                p.remove(intPreferencesKey(HeaderSizes.key(false, name)))
                p.remove(intPreferencesKey(HeaderSizes.key(true, name)))
            }
            p[booleanPreferencesKey("compact_header")] = false
        }
    }
    suspend fun resetAppearance() {
        store.edit { p ->
            listOf("use_dots", "dot_uniform").forEach { p.remove(booleanPreferencesKey(it)) }
            listOf("dot_size", "dot_color", "home_text", "hub_text", "settings_text").forEach { p.remove(intPreferencesKey(it)) }
            listOf("font", "font_file").forEach { p.remove(stringPreferencesKey(it)) }
        }
    }

    suspend fun exportSnapshot(): ConfigBackup.Snapshot {
        val p = data.first()
        val values = mutableMapOf<String, Any>(
            "terminal_enabled" to true, "terminal_user" to "issu", "terminal_device" to "",
            "visible_apps_count" to 8, "home_top_padding" to 0, "home_spacing" to 20,
            "show_clock" to true, "show_date" to true, "show_battery" to true, "show_signal" to true,
            "hide_status_bar" to false, "show_weather" to false, "weather_text" to false,
            "music_destination" to "home", "calendar_destination" to "hub", "calendar_ids" to "*",
            "use_dots" to true, "dot_size" to 8, "dot_uniform" to false, "dot_color" to 0xFFF800FF.toInt(),
            "font" to "default", "home_text" to 16, "hub_text" to 13, "settings_text" to 16,
            "home_hub_text" to 13, "home_hub_padding" to 12, "home_hub_indicator" to "pulse", "compact_header" to false)
        values.putAll(p.asMap().filterKeys { it.name in ConfigSchema.keys }.mapKeys { it.key.name })
        // Materialize defaults too: a backup must not depend on a future release changing them.
        val activeCompact = p[booleanPreferencesKey("compact_header")] ?: false
        for (compact in listOf(false, true)) values.putAll(HeaderSizes.snapshot(compact) { key ->
            if (key in HeaderSizes.keys && compact != activeCompact) null else p[intPreferencesKey(key)]
        })
        HeaderSizes.keys.forEach { values.remove(it) }
        values["favorite_order"] = ordered(p).joinToString("\n")
        val font = if (values["font"] == "custom") {
            val file = File(appContext.filesDir, p[stringPreferencesKey("font_file")] ?: error("font"))
            require(file.canonicalFile.parentFile == File(appContext.filesDir, "fonts").canonicalFile)
            require(file.length() in 4..8L * 1024 * 1024); file.readBytes()
        } else null
        ConfigSchema.validate(values)
        return ConfigBackup.Snapshot(values, font)
    }
    suspend fun importSnapshot(snapshot: ConfigBackup.Snapshot) {
        ConfigSchema.validate(snapshot.values)
        require((snapshot.values["font"] == "custom") == (snapshot.font != null))
        val fontFile = snapshot.font?.let { ConfigBackup.checkedFont(appContext, it) }
        try {
            withContext(NonCancellable) { store.edit { p ->
                p.asMap().keys.filter { it.name in ConfigSchema.keys || it.name == "font_file" || it.name == "favorites" }.forEach { p.remove(it) }
                snapshot.values.forEach { (key, value) -> when (value) {
                    is Boolean -> p[booleanPreferencesKey(key)] = value
                    is Int -> p[intPreferencesKey(key)] = value
                    is String -> p[stringPreferencesKey(key)] = value
                } }
                p[stringSetPreferencesKey("favorites")] = (snapshot.values["favorite_order"] as? String).orEmpty().split('\n').filter { it.isNotEmpty() }.toSet()
                fontFile?.let { p[stringPreferencesKey("font_file")] = "fonts/${it.name}" }
            } }
        } catch (e: Exception) { fontFile?.delete(); throw e }
    }
}
