package com.nodus.launcher.ui.home

import androidx.compose.ui.platform.LocalContext
import com.nodus.launcher.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.widgets.MusicEntry

@Composable
fun MusicWidget(entry: MusicEntry, modifier: Modifier = Modifier, open: () -> Unit, control: (String) -> Unit) {
    val spacing = LocalAppStyle.current.settings.homeSpacing
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().padding(top = HomePolicy.spacing(spacing, 12f).dp, bottom = HomePolicy.spacing(spacing, 4f).dp)) {
        Column(Modifier.fillMaxWidth().clickable(onClickLabel = context.getString(R.string.ui_open_music_player), onClick = open)) {
            Text(entry.title, fontFamily = LocalAppStyle.current.family,
                fontSize = LocalAppStyle.current.settings.homeText.sp, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (entry.artist.isNotBlank()) Text(entry.artist, fontFamily = LocalAppStyle.current.family,
                fontSize = 11.sp, lineHeight = 17.sp, color = Color.Gray,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppText(if (entry.paused) context.getString(R.string.ui_paused) else context.getString(R.string.ui_playing), size = 11,
                color = Color.Gray, modifier = Modifier.weight(1f))
            MusicButton("previous", context.getString(R.string.ui_previous_track), entry.previous) { control("previous") }
            MusicButton(if (entry.paused) "play" else "pause",
                if (entry.paused) context.getString(R.string.ui_resume_playback) else context.getString(R.string.ui_pause), entry.toggle) { control("toggle") }
            MusicButton("next", context.getString(R.string.ui_next_track), entry.next) { control("next") }
        }
    }
}

@Composable
private fun MusicButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
        Canvas(Modifier.size(22.dp)) {
            val color = if (enabled) Color.White else Color.DarkGray
            val w = size.width; val h = size.height
            if (symbol == "pause") {
                for (x in listOf(.28f, .62f)) drawLine(color, Offset(w*x, h*.18f), Offset(w*x, h*.82f), 2.dp.toPx())
            } else {
                val reverse = symbol == "previous"
                fun x(f: Float) = if (reverse) w*(1-f) else w*f
                val triangle = Path().apply {
                    moveTo(x(.25f), h*.18f); lineTo(x(.75f), h*.5f); lineTo(x(.25f), h*.82f); close()
                }
                drawPath(triangle, color)
                if (symbol != "play") drawLine(color, Offset(x(.83f),h*.18f), Offset(x(.83f),h*.82f), 1.5.dp.toPx())
            }
        }
    }
}
