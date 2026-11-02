package com.nodus.launcher.util

import android.content.Context
import android.os.Build

data class InstalledVersion(val code: Int, val name: String)

@Suppress("DEPRECATION")
fun Context.installedVersion(): InstalledVersion {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else info.versionCode
    return InstalledVersion(code, info.versionName.orEmpty())
}
