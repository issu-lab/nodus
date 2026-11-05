package com.nodus.launcher.util

/** Portable allowlist. Runtime state, permissions, histories and private paths never belong in a backup. */
object ConfigSchema {
    val booleans = setOf("show_clock", "show_date", "show_battery", "show_signal", "hide_status_bar", "show_weather", "weather_text", "use_dots", "dot_uniform", "compact_header", "terminal_enabled")
    val numbers = mutableMapOf(
        "visible_apps_count" to 1..500, "home_top_padding" to 0..400, "home_spacing" to 0..32,
        "settings_text" to 13..24, "home_text" to 12..28, "hub_text" to 12..24,
        "home_hub_text" to 10..26, "home_hub_padding" to 0..32, "dot_size" to 4..16,
        "dot_color" to Int.MIN_VALUE..Int.MAX_VALUE
    ).apply {
        for ((key, range) in mapOf("clock_size" to 18..64, "date_size" to 10..28, "weather_size" to 10..28, "battery_size" to 10..28, "signal_size" to 12..32)) {
            put(key, range); put("standard_$key", range); put("compact_$key", range)
        }
    }.toMap()
    val enums = mapOf("font" to setOf("default", "system", "custom"), "music_destination" to setOf("home", "hub", "both", "none"), "calendar_destination" to setOf("home", "hub", "both", "none"), "home_hub_indicator" to setOf("pulse", "fixed", "off"))
    val strings = enums.keys + setOf("calendar_ids", "favorite_order", "terminal_user", "terminal_device")
    val keys = booleans + numbers.keys + strings
    fun validate(values: Map<String, Any>) {
        require(values.keys.all { it in keys })
        values.forEach { (key, value) -> when {
            key in booleans -> require(value is Boolean)
            key in numbers -> require(value is Int && value in numbers.getValue(key))
            key in strings -> {
                require(value is String && value.length <= 32768)
                enums[key]?.let { require(value in it) }
                if (key == "terminal_user" || key == "terminal_device") require(validPrompt(value))
                if (key == "calendar_ids") require(value == "*" || value.isEmpty() || value.split(',').all { it.toLongOrNull() != null })
                if (key == "favorite_order") require(value.isEmpty() || value.split('\n').let { ids -> ids.size <= 500 && ids.distinct().size == ids.size && ids.all { it.matches(Regex("[A-Za-z0-9_.]+")) } })
            }
        } }
        val favorites = (values["favorite_order"] as? String).orEmpty().split('\n').count { it.isNotEmpty() }
        require(favorites <= (values["visible_apps_count"] as? Int ?: 8))
    }
    fun validPrompt(value: String) = value.length <= 40 && value.all { it.isLetterOrDigit() || it in "._-" }
}
