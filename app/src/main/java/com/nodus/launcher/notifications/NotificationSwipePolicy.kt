package com.nodus.launcher.notifications

import kotlin.math.abs
import kotlin.math.min

object NotificationSwipePolicy {
    fun dismiss(distance: Float, velocity: Float, width: Float, density: Float, dismissible: Boolean): Boolean {
        if (!dismissible || width <= 0 || density <= 0 || abs(distance) < 16f * density) return false
        if (abs(velocity) > 900f * density && velocity * distance < 0f) return false
        return abs(distance) >= min(80f * density, width * .3f) || abs(velocity) >= 900f * density
    }
}
