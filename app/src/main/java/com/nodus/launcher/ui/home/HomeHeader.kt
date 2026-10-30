package com.nodus.launcher.ui.home

import androidx.compose.ui.platform.LocalContext
import com.nodus.launcher.R
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.util.UiSettings
import com.nodus.launcher.widgets.WeatherEntry
import com.nodus.launcher.home.HomePolicy
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeHeader(config: UiSettings, now: Long, weather: WeatherEntry?, weatherError: String?,
               battery: Int?, signal: Int?, wifiConnected: Boolean, openSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val weatherDescription = context.getString(when (weather?.code) {
        0 -> R.string.ui_weather_clear
        1, 2 -> R.string.ui_weather_partly_cloudy
        3 -> R.string.ui_weather_cloudy
        45, 48 -> R.string.ui_weather_fog
        51, 53, 55, 56, 57 -> R.string.ui_weather_drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> R.string.ui_weather_rain
        71, 73, 75, 77, 85, 86 -> R.string.ui_weather_snow
        95, 96, 99 -> R.string.ui_weather_storm
        else -> R.string.ui_weather_unknown
    })
    val clock: @Composable () -> Unit = {
        if (config.clock) Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
            fontFamily = LocalAppStyle.current.family, fontSize = config.clockSize.sp,
            lineHeight = (config.clockSize * 54f / 42f).sp, fontWeight = FontWeight.Medium, color = Color.White,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = openSettings))
    }
    val date: @Composable () -> Unit = {
        if (config.date) AppText(SimpleDateFormat("EEE dd MMM", Locale.getDefault()).format(Date(now)),
            size = config.dateSize, color = Color(0xFF9B9B9B))
    }
    val indicators: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (config.battery) AppText(battery?.let { "$it%" } ?: "—%", size = config.batterySize, color = Color.LightGray)
            if (config.signal) {
                if (wifiConnected) WifiGlyph(config.signalSize)
                SignalGlyph(signal, config.signalSize)
            }
        }
    }
    val weatherContent: @Composable () -> Unit = {
        if (config.weather) {
            if (weather != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!config.weatherText) { WeatherGlyph(weather.code, weatherDescription, config.weatherSize * 20 / 14); Spacer(Modifier.width(6.dp)) }
                    AppText(if (config.weatherText) "${weatherDescription} · ${weather.temperature}°" else "${weather.temperature}°", size = config.weatherSize)
                }
                if (!config.compactHeader && weather.city.isNotBlank()) AppText(weather.city, size = (config.weatherSize * 11 / 14).coerceAtLeast(10), color = Color.Gray)
                if (weather.stale(now) || weatherError != null) AppText(context.getString(R.string.ui_out_of_date), size = config.weatherSize.coerceAtMost(14), color = Color.LightGray)
            } else AppText(weatherError ?: context.getString(R.string.ui_updating_weather), size = config.weatherSize, color = Color.LightGray)
        }
    }
    Column(Modifier.fillMaxWidth().padding(bottom = HomePolicy.spacing(config.homeSpacing, 12f).dp)) {
        if (config.compactHeader) {
            clock()
            SingleLineHeaderRow {
                date()
                if (config.weather) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics {
                            contentDescription = if (weather == null) weatherError ?: context.getString(R.string.ui_weather_unavailable)
                                else context.getString(R.string.ui_1_s_2_d_degrees, weatherDescription, weather.temperature) +
                                    if (weather.stale(now) || weatherError != null) context.getString(R.string.ui_out_of_date_2) else ""
                        }) {
                        if (weather != null) {
                            WeatherGlyph(weather.code, weatherDescription, config.weatherSize * 20 / 14)
                            Spacer(Modifier.width(4.dp))
                        }
                        AppText(weather?.let { "${it.temperature}°" } ?: "—°", size = config.weatherSize,
                            color = if (weather != null && !weather.stale(now) && weatherError == null) Color.White else Color.LightGray)
                    }
                }
                if (config.signal) {
                    if (wifiConnected) WifiGlyph(config.signalSize)
                    SignalGlyph(signal, config.signalSize)
                }
                if (config.battery) AppText(battery?.let { "$it%" } ?: "—%", size = config.batterySize, color = Color.LightGray)
            }
        } else {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                clock()
                if (config.weather) Column(horizontalAlignment = Alignment.End,
                    modifier = (if (config.clock) Modifier else Modifier.fillMaxWidth()).padding(start = if (config.clock) 12.dp else 0.dp)) { weatherContent() }
            }
            FlowRow(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                date()
                if (config.date) indicators() else Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { indicators() }
            }
        }
    }
}

/** Keep the whole status row on one line even with large fonts or narrow screens.
 * Measure its natural width, then fit proportionally around the shared leading edge. */
@Composable
private fun SingleLineHeaderRow(content: @Composable RowScope.() -> Unit) {
    Layout(modifier = Modifier.fillMaxWidth(), content = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }) { children, constraints ->
        val row = children.single().measure(Constraints(maxWidth = Constraints.Infinity, maxHeight = constraints.maxHeight))
        val width = constraints.maxWidth
        val scale = if (row.width > width && row.width > 0) width.toFloat() / row.width else 1f
        layout(width, constraints.constrainHeight((row.height * scale).roundToInt())) {
            row.placeWithLayer(0, 0) {
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun WifiGlyph(iconSize: Int) {
    val context = LocalContext.current
    Canvas(Modifier.size(iconSize.dp).semantics { contentDescription = context.getString(R.string.ui_wi_fi_connected) }) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * .85f)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        for (radius in listOf(.25f, .46f, .68f)) {
            val r = size.width * radius
            drawArc(Color.White, startAngle = 225f, sweepAngle = 90f, useCenter = false,
                topLeft = center - androidx.compose.ui.geometry.Offset(r, r),
                size = androidx.compose.ui.geometry.Size(r*2, r*2), style = stroke)
        }
        drawCircle(Color.White, 1.3.dp.toPx(), center)
    }
}

@Composable
private fun SignalGlyph(level: Int?, iconSize: Int) {
    val context = LocalContext.current
    Canvas(Modifier.size(iconSize.dp).semantics { contentDescription = level?.let { context.getString(R.string.ui_signal_1_d_out_of_4, it) } ?: context.getString(R.string.ui_signal_unavailable) }) {
        for (i in 0..3) drawRect(if (level != null && i < level) Color.White else Color.DarkGray,
            topLeft = androidx.compose.ui.geometry.Offset(i * size.width / 4, size.height * (0.7f - i * 0.2f)),
            size = androidx.compose.ui.geometry.Size(size.width / 6, size.height * (0.3f + i * 0.2f)))
    }
}
@Composable
private fun WeatherGlyph(code: Int, description: String, iconSize: Int) {
    Canvas(Modifier.size(iconSize.dp).semantics { contentDescription = description }) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
        if (code <= 2) {
            drawCircle(Color.White, size.width * .22f, style = stroke)
            for (i in 0..7) {
                val a = Math.PI * i / 4
                val cx = size.width / 2; val cy = size.height / 2
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(a).toFloat()*size.width*.34f,cy + kotlin.math.sin(a).toFloat()*size.height*.34f),
                    androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(a).toFloat()*size.width*.47f,cy + kotlin.math.sin(a).toFloat()*size.height*.47f),1.2.dp.toPx())
            }
        } else {
            val w = size.width; val h = size.height
            val cloud = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .24f, h * .69f)
                cubicTo(w * -.02f, h * .69f, w * -.02f, h * .32f, w * .25f, h * .35f)
                cubicTo(w * .25f, h * .03f, w * .73f, h * .03f, w * .77f, h * .35f)
                cubicTo(w * 1.03f, h * .33f, w * 1.03f, h * .69f, w * .77f, h * .69f)
                close()
            }
            drawPath(cloud, Color.White, style = stroke)
            if (code in listOf(45, 48)) drawLine(Color.White,
                androidx.compose.ui.geometry.Offset(w * .15f, h * .86f), androidx.compose.ui.geometry.Offset(w * .85f, h * .86f), 1.dp.toPx())
            else if (code >= 51) for (i in 1..3) drawLine(Color.White,
                androidx.compose.ui.geometry.Offset(w * i / 4, h * .82f),
                androidx.compose.ui.geometry.Offset(w * i / 4 - 2, h), 1.dp.toPx())
        }
    }
}
