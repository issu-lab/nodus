package com.nodus.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.nodus.launcher.model.AppInfo

class AppRepository(private val context: Context) {

    fun loadAllApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                    icon = resolveInfo.loadIcon(pm),
                    isFavorite = false
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
