package com.nodus.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import com.nodus.launcher.home.AppDrawerPolicy
import kotlinx.coroutines.*

/** Drag updates are synchronous; only release/Back starts an animation coroutine. */
@Stable
class AppDrawerState(initialOpen: Boolean, private val scope: CoroutineScope, private val targetChanged: (Boolean) -> Unit) {
    private val animation = Animatable(if (initialOpen) 1f else 0f).apply { updateBounds(0f, 1f) }
    private var dragProgress by mutableStateOf<Float?>(null)
    private var targetOpen by mutableStateOf(initialOpen)
    private var settling by mutableStateOf(false)
    private var dragging = false
    private var closingDrag = false
    private var generation = 0
    private var motion: Job? = null
    val progress: Float get() = (dragProgress ?: animation.value).coerceIn(0f, 1f)
    val visible: Boolean get() = targetOpen || dragProgress != null || animation.value > 0f
    val canStart: Boolean get() = !visible && !settling

    fun start(): Boolean {
        if (!canStart) return false
        generation++; motion?.cancel(); settling = false
        dragProgress = progress; dragging = true; closingDrag = false
        return true
    }
    fun startClosing(): Boolean {
        if (!targetOpen || settling || dragging) return false
        generation++; motion?.cancel()
        dragProgress = progress; dragging = true; closingDrag = true
        return true
    }
    fun drag(deltaY: Float, height: Float) {
        if (dragging) dragProgress = AppDrawerPolicy.drag(progress, deltaY, height)
    }
    fun release(velocityY: Float, height: Float, density: Float) {
        if (!dragging) return
        val open = if (closingDrag) !AppDrawerPolicy.closes(progress, velocityY, height, density)
            else AppDrawerPolicy.opens(progress, velocityY, height, density)
        settle(open, if (height > 0f) -velocityY / height else 0f)
    }
    fun cancelDrag() { if (dragging) settle(closingDrag) }
    fun settle(open: Boolean, velocity: Float = animation.velocity) {
        val from = progress
        val run = ++generation
        motion?.cancel(); dragging = false
        targetOpen = open; targetChanged(open); settling = true
        // Retain the last displayed position until snapTo has acquired the animation mutex.
        dragProgress = from
        motion = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                animation.snapTo(from)
                dragProgress = null
                animation.animateTo(if (open) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow, visibilityThreshold = .001f),
                    initialVelocity = velocity.coerceIn(-8f, 8f))
            } finally {
                if (generation == run) settling = false
            }
        }
    }
}
