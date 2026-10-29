package com.nodus.launcher.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.nodus.launcher.home.HomePolicy
import org.json.JSONObject
import java.time.LocalDate

object RecentApps {
    fun hasAccess(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    fun dailyCounts(context: Context): Map<String, Int> {
        if (!hasAccess(context)) return emptyMap()
        val cache = context.getSharedPreferences("daily_app_usage", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (cache.getString("date", null) == today) {
            try {
                val json = JSONObject(cache.getString("counts", "{}")!!)
                return json.keys().asSequence().associateWith { json.getInt(it) }
            } catch (_: Exception) { /* Rebuild an invalid cache. */ }
        }
        return try {
            val end = System.currentTimeMillis()
            val events = (context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
                .queryEvents(end - 7L * 24 * 60 * 60 * 1000, end)
            val sequence = mutableListOf<Pair<String, Int>>()
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    sequence += event.packageName to event.eventType
                }
            }
            val counts = HomePolicy.countOpens(sequence)
            cache.edit().putString("date", today).putString("counts", JSONObject(counts).toString()).apply()
            counts
        } catch (_: Exception) { emptyMap() }
    }
}
