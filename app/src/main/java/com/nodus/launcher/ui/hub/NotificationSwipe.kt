package com.nodus.launcher.ui.hub

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.nodus.launcher.notifications.NotificationActionResult
import com.nodus.launcher.notifications.NotificationSwipePolicy
import kotlinx.coroutines.*
import kotlin.math.abs

/** Direct finger updates, one coroutine per release; never removes a local copy of a notification. */
@Composable
internal fun NotificationSwipe(identity: String, revision: String, dismissible: Boolean,
    modifier: Modifier = Modifier, onDismiss: () -> NotificationActionResult,
    content: @Composable (requestDismiss: () -> Unit, busy: Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val motion = remember(identity, revision) { Animatable(0f) }
    var manual by remember(identity, revision) { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(1f) }
    var busy by remember(identity, revision) { mutableStateOf(false) }
    var job by remember(identity, revision) { mutableStateOf<Job?>(null) }
    val action by rememberUpdatedState(onDismiss)
    DisposableEffect(identity, revision) { onDispose { job?.cancel() } }
    fun settle(remove: Boolean, direction: Float) {
        if (busy) return
        val from = manual ?: motion.value
        val command = action // Keep the revision the user actually swiped.
        job?.cancel()
        busy = remove
        manual = from
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            motion.snapTo(from); manual = null
            if (remove) {
                motion.animateTo(if (direction < 0) -width else width, tween(180))
                val result = command()
                if (result == NotificationActionResult.SUCCESS || result == NotificationActionResult.MISSING) {
                    // Normally Android removes this composition. Recover if no removal snapshot arrives.
                    delay(700)
                    com.nodus.launcher.service.NodusNotificationListener.refreshConnected()
                    delay(100)
                }
            }
            motion.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
            busy = false
        }
    }
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }
        .graphicsLayer {
            translationX = manual ?: motion.value
            alpha = if (dismissible) (1f - abs(translationX) / width.coerceAtLeast(1f) * .65f).coerceIn(.35f, 1f) else 1f
        }
        .pointerInput(identity, revision, dismissible, density) {
            var distance = 0f
            var accepted = false
            val velocity = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = {
                    accepted = !busy
                    if (accepted) { job?.cancel(); distance = manual ?: motion.value; manual = distance; velocity.resetTracking() }
                },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    if (accepted && !busy) {
                        distance += amount
                        manual = if (dismissible) distance else (distance * .18f).coerceIn(-24f*density, 24f*density)
                        velocity.addPosition(change.uptimeMillis, change.position)
                    }
                },
                onDragEnd = {
                    if (accepted && !busy) settle(NotificationSwipePolicy.dismiss(distance, velocity.calculateVelocity().x, width, density, dismissible), distance)
                },
                onDragCancel = { if (accepted && !busy) settle(false, 0f); accepted = false }
            )
        }) { content({ if (dismissible) settle(true, 1f) else action() }, busy) }
}
