package com.nodus.launcher.home

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

object HomePolicy {
    data class App(val id: String, val label: String)
    fun select(apps: List<App>, favorites: List<String>, counts: Map<String, Int>, limit: Int): List<String> {
        val available = apps.distinctBy { it.id }.associateBy { it.id }
        val first = favorites.distinct().filter { it in available }
        val rest = available.values.filter { it.id !in first }
            .sortedWith(compareByDescending<App> { counts[it.id] ?: 0 }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.id })
        return (first + rest.map { it.id }).take(limit.coerceAtLeast(0))
    }
    fun favoriteUpdate(current: List<String>, id: String, limit: Int): List<String>? = when {
        id in current -> current - id
        current.size < limit -> current + id
        else -> null
    }
    fun validLimit(limit: Int, favorites: Int) = limit >= 1 && limit >= favorites
    fun countOpens(events: List<Pair<String, Int>>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        var foreground: String? = null
        for ((pkg, type) in events) {
            if (type == 1) {
                if (foreground != pkg) result[pkg] = (result[pkg] ?: 0) + 1
                foreground = pkg
            } // Foreground switches, rather than internal activity transitions, count as app openings.
        }
        return result
    }
    fun eventEnd(end: Long, allDay: Boolean, zone: ZoneId): Long =
        if (allDay) Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli() else end
    fun eventStart(begin: Long, allDay: Boolean, zone: ZoneId): Long = eventEnd(begin, allDay, zone)
    fun visibleToday(begin: Long, end: Long, allDay: Boolean, now: Long, zone: ZoneId): Boolean {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val start = eventStart(begin, allDay, zone)
        val finish = eventEnd(end, allDay, zone)
        return finish > now && start < dayEnd && finish > dayStart
    }
    fun spacing(gap: Int, baseline: Float): Float = baseline * gap.coerceIn(0, 32) / 20f

    data class Placement(val top: Float, val scale: Float)
    fun fixedPlacement(viewport: Float, content: Float, requested: Float): Placement {
        val available = viewport.coerceAtLeast(0f)
        val height = content.coerceAtLeast(0f)
        val scale = if (height > available && height > 0f) available / height else 1f
        return Placement(requested.coerceIn(0f, (available - height * scale).coerceAtLeast(0f)), scale)
    }

}
