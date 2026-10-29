package com.nodus.launcher.widgets

import kotlin.math.roundToInt

/** Reject incomplete broadcasts and unsupported property sentinels, not valid 0%. */
object BatteryReading {
    fun resolve(level: Int, scale: Int, capacity: Int?, previous: Int?): Int? {
        if (scale > 0 && level in 0..scale) return (100.0 * level / scale).roundToInt()
        return capacity?.takeIf { it in 0..100 } ?: previous?.takeIf { it in 0..100 }
    }
}
