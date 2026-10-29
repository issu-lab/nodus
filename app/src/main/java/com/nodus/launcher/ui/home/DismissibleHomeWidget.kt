package com.nodus.launcher.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DismissibleHomeWidget(identity: String, name: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var offset by remember(identity) { mutableFloatStateOf(0f) }
    val dismiss by rememberUpdatedState(onDismiss)
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    Box(Modifier.fillMaxWidth().offset { IntOffset(offset.roundToInt(), 0) }
        .pointerInput(identity, threshold) {
            detectHorizontalDragGestures(
                onDragStart = { offset = 0f },
                onHorizontalDrag = { change, amount -> offset += amount; change.consume() },
                onDragEnd = {
                    val remove = abs(offset) >= threshold
                    offset = 0f
                    if (remove) dismiss()
                },
                onDragCancel = { offset = 0f }
            )
        }.semantics {
            customActions = listOf(CustomAccessibilityAction("Nascondi widget $name") { dismiss(); true })
        }) { content() }
}
