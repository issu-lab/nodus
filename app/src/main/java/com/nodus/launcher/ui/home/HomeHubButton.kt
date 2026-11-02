package com.nodus.launcher.ui.home

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nodus.launcher.R
import com.nodus.launcher.ui.common.Accent
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.common.LocalMenuLeadingSpace
import com.nodus.launcher.util.UiSettings

@Composable
fun HomeHubButton(config: UiSettings, count: Int, open: () -> Unit) {
    val context = LocalContext.current
    val label = if (count == 0) context.getString(R.string.ui_runtime_notification_hub_no_notifications)
        else context.resources.getQuantityString(R.plurals.ui_notification_count, count, count)
    TextButton(onClick = open, shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = config.homeHubPadding.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (count > 0 && config.homeHubIndicator != "off") {
                PresenceDot(config.homeHubIndicator == "pulse")
                Spacer(Modifier.width(8.dp))
            }
            CompositionLocalProvider(LocalMenuLeadingSpace provides false) {
                AppText(label, size = config.homeHubText, color = Color.White, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PresenceDot(pulse: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var animate by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle, context) {
        fun update() {
            val motion = runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }.getOrDefault(false)
            animate = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && motion
        }
        val lifecycleObserver = LifecycleEventObserver { _, _ -> update() }
        val motionObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = update()
        }
        lifecycle.addObserver(lifecycleObserver)
        val observing = runCatching { context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, motionObserver) }.isSuccess
        update()
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            if (observing) runCatching { context.contentResolver.unregisterContentObserver(motionObserver) }
        }
    }
    val brightness = if (pulse && animate) {
        val transition = rememberInfiniteTransition(label = "notificationPresence")
        transition.animateFloat(1f, .55f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "presenceBrightness").value
    } else 1f
    val description = context.getString(R.string.ui_presence_present)
    Canvas(Modifier.size(16.dp).semantics { contentDescription = description }) {
        val radius = size.minDimension / 2
        for (i in 4 downTo 1) drawCircle(Accent.copy(alpha = .035f * brightness), radius * (.5f + i * .125f))
        drawCircle(Accent.copy(alpha = brightness), radius * .46f)
    }
}
