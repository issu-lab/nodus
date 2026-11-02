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
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
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
import com.nodus.launcher.ui.common.ContentGrid
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.ui.common.Accent
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.common.LocalMenuLeadingSpace
import com.nodus.launcher.util.UiSettings

@Composable
fun HomeHubButton(config: UiSettings, count: Int, open: () -> Unit) {
    val context = LocalContext.current
    val label = if (count == 0) context.getString(R.string.ui_runtime_notification_hub_no_notifications)
        else context.resources.getQuantityString(R.plurals.ui_notification_count, count, count)
    // Use the exact app-row lane and minimum icon box. Background never indents content.
    val backgroundPadding = config.homeHubPadding.coerceIn(0, 32)
    val appGap = HomePolicy.spacing(config.homeSpacing, 10f)
    val outerGap = (appGap - backgroundPadding).coerceAtLeast(0f)
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = open)
        .padding(vertical = outerGap.dp)) {
        Row(Modifier.fillMaxWidth()
            .then(if (backgroundPadding > 0) Modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp)) else Modifier)
            .padding(vertical = backgroundPadding.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(ContentGrid.IconLane), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.CenterStart) {
                    if (count > 0 && config.homeHubIndicator != "off") {
                        PresenceDot(config.homeHubIndicator == "pulse", if (config.dots) config.dotSize else 8)
                    }
                }
            }
            CompositionLocalProvider(LocalMenuLeadingSpace provides false) {
                AppText(label, size = config.homeHubText, color = Color.White, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PresenceDot(pulse: Boolean, dotSize: Int) {
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
    Canvas(Modifier.size(dotSize.dp).semantics { contentDescription = description }) {
        val radius = size.minDimension / 2
        for (i in 4 downTo 1) drawCircle(Accent.copy(alpha = .035f * brightness), radius * (1f + i * .2f))
        drawCircle(Accent.copy(alpha = brightness), radius)
    }
}
