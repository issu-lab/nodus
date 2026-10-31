package com.nodus.launcher.home

/** Stable profile keys; legacy flat values migrate into the profile active before switching. */
object HeaderSizes {
    val keys = listOf("clock_size", "date_size", "weather_size", "battery_size", "signal_size")
    private val ranges = listOf(18..64, 10..28, 10..28, 10..28, 12..32)
    fun key(compact: Boolean, name: String) = (if (compact) "compact_" else "standard_") + name
    fun read(compact: Boolean, name: String, get: (String) -> Int?): Int {
        val index = keys.indexOf(name)
        require(index >= 0)
        val defaults = if (compact) listOf(24, 12, 12, 12, 14) else listOf(42, 14, 14, 16, 18)
        return (get(key(compact, name)) ?: get(name) ?: defaults[index]).coerceIn(ranges[index])
    }
    fun snapshot(compact: Boolean, get: (String) -> Int?) = keys.associate { key(compact, it) to read(compact, it, get) }
}
