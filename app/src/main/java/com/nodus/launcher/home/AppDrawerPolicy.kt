package com.nodus.launcher.home

/** Progress uses the full usable viewport, velocities are pixels/second. */
object AppDrawerPolicy {
    fun drag(progress: Float, deltaY: Float, height: Float): Float =
        if (height <= 0f) progress.coerceIn(0f, 1f) else (progress - deltaY / height).coerceIn(0f, 1f)

    fun closes(progress: Float, velocityY: Float, height: Float, density: Float): Boolean {
        if (height <= 0f || density <= 0f) return false
        val distance = (1f - progress.coerceIn(0f, 1f)) * height
        if (distance < 24f * density) return false
        if (velocityY <= -650f * density) return false
        if (velocityY >= 650f * density) return true
        return distance >= minOf(96f * density, height * .2f)
    }

    fun opens(progress: Float, velocityY: Float, height: Float, density: Float): Boolean {
        if (height <= 0f || density <= 0f) return false
        val distance = progress.coerceIn(0f, 1f) * height
        if (distance < 24f * density) return false
        if (velocityY >= 650f * density) return false
        if (velocityY <= -650f * density) return true
        return distance >= minOf(96f * density, height * .2f)
    }
}
