package com.nodus.launcher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.nodus.launcher.home.HomeInteractionPolicy
import kotlin.math.abs

/** Observe direction first, then track a single upward home drag at frame rate.
 * Horizontal widgets, taps, long presses, drawer scrolling and system edges keep ownership.
 */
suspend fun PointerInputScope.detectAppDrawerSwipe(
    edge: Float,
    canOpen: () -> Boolean,
    start: () -> Boolean,
    drag: (Float, Float) -> Unit,
    release: (Float, Float, Float) -> Unit,
    cancel: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!canOpen() || !HomeInteractionPolicy.insideGestureArea(
                down.position.x, down.position.y, size.width.toFloat(), size.height.toFloat(), edge)) return@awaitEachGesture
        val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
        var previousY = down.position.y
        var claimed = false
        var finished = false
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size != 1) return@awaitEachGesture
                val change = event.changes.first()
                if (change.id != down.id || change.isConsumed) return@awaitEachGesture
                tracker.addPosition(change.uptimeMillis, change.position)
                val delta = change.position - down.position
                if (!claimed) {
                    if (change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) return@awaitEachGesture
                    if (abs(delta.x) > viewConfiguration.touchSlop || abs(delta.y) > viewConfiguration.touchSlop) {
                        if (!HomeInteractionPolicy.upward(delta.x, delta.y, viewConfiguration.touchSlop) || !start()) return@awaitEachGesture
                        claimed = true
                        drag(delta.y, size.height.toFloat())
                    }
                } else drag(change.position.y - previousY, size.height.toFloat())
                previousY = change.position.y
                if (claimed) change.consume()
                if (!change.pressed) {
                    finished = true
                    if (claimed) release(tracker.calculateVelocity().y, size.height.toFloat(), density)
                    return@awaitEachGesture
                }
            }
        } finally {
            if (claimed && !finished) cancel()
        }
    }
}

/** Horizontal gestures are observed after children so music/calendar swipes retain ownership. */
suspend fun PointerInputScope.detectHomeHorizontalSwipe(
    edge: Float, canOpen: () -> Boolean, startRight: () -> Boolean,
    dragRight: (Float, Float) -> Unit, releaseRight: (Float, Float, Float) -> Unit,
    cancelRight: () -> Unit, openLeft: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!canOpen() || !HomeInteractionPolicy.insideGestureArea(down.position.x, down.position.y, size.width.toFloat(), size.height.toFloat(), edge)) return@awaitEachGesture
        val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
        var previousX = down.position.x; var direction = 0; var finished = false
        try {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.size != 1) return@awaitEachGesture
                val change = event.changes.first()
                if (change.id != down.id || change.isConsumed) return@awaitEachGesture
                tracker.addPosition(change.uptimeMillis, change.position)
                val delta = change.position - down.position
                if (direction == 0) {
                    if (change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) return@awaitEachGesture
                    if (abs(delta.x) > viewConfiguration.touchSlop || abs(delta.y) > viewConfiguration.touchSlop) {
                        if (abs(delta.x) <= abs(delta.y) * 1.3f) return@awaitEachGesture
                        direction = if (delta.x > 0) 1 else -1
                        if (direction == 1) { if (!startRight()) return@awaitEachGesture; dragRight(-delta.x, size.width.toFloat()) }
                    }
                } else if (direction == 1) dragRight(-(change.position.x - previousX), size.width.toFloat())
                previousX = change.position.x
                if (direction != 0) change.consume()
                if (!change.pressed) {
                    finished = true
                    if (direction == 1) releaseRight(-tracker.calculateVelocity().x, size.width.toFloat(), density)
                    if (direction == -1 && delta.x < -80 * density) openLeft()
                    return@awaitEachGesture
                }
            }
        } finally { if (direction == 1 && !finished) cancelRight() }
    }
}
