package com.nodus.launcher.home

object UpdateNoticePolicy {
    fun show(firstRun: Boolean, seenVersion: Int, installedVersion: Int) =
        !firstRun && installedVersion > 0 && seenVersion < installedVersion
}
