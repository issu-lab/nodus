package com.nodus.launcher.ui.hub

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nodus.launcher.home.WidgetVisibilityPolicy
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.home.DismissibleHomeWidget
import com.nodus.launcher.ui.home.MusicWidget
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.widgets.CalendarEntry
import com.nodus.launcher.widgets.MusicEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HubMusicCard(entry: MusicEntry, control: (String) -> Boolean, notify: (String) -> Unit) {
    var hidden by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    if (hidden == entry.visibilityKey) return
    DismissibleHomeWidget(entry.visibilityKey, "musica", onDismiss = { hidden = entry.visibilityKey }) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            AppText("MUSICA", size = 11, color = Color.Gray)
            val style = LocalAppStyle.current
            CompositionLocalProvider(LocalAppStyle provides style.copy(settings = style.settings.copy(homeText = style.settings.hubText))) {
                MusicWidget(entry, open = {
                    try { if (entry.activity != null) entry.activity.send() else notify("Apertura del lettore non disponibile.") }
                    catch (_: Exception) { notify("Impossibile aprire il lettore.") }
                }, control = { if (!control(it)) notify("Controllo musicale non disponibile.") })
            }
        }
    }
}

@Composable
fun HubCalendarCard(events: List<CalendarEntry>, now: Long, granted: Boolean, authorize: () -> Unit, notify: (String) -> Unit) {
    val context = LocalContext.current
    var hiddenDay by rememberSaveable { mutableStateOf<String?>(null) }
    var hiddenEvents by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
    val ids = events.map { "${it.id}:${it.begin}" }
    if (WidgetVisibilityPolicy.calendarHidden(hiddenDay, today, hiddenEvents.toSet(), ids.toSet()) || (granted && events.isEmpty())) return
    DismissibleHomeWidget(WidgetVisibilityPolicy.fingerprint(today, *ids.sorted().toTypedArray()), "calendario", onDismiss = {
        hiddenDay = today; hiddenEvents = ids
    }) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            AppText("OGGI", size = 11, color = Color.Gray)
            if (!granted) {
                AppText("Autorizza il calendario per mostrare gli eventi di oggi.", size = 12, color = Color.LightGray)
                TextButton(onClick = authorize, modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp)) { AppText("Autorizza calendario", size = 12) }
            } else events.forEach { event ->
                Column(Modifier.fillMaxWidth().clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)) }
                    catch (_: Exception) { notify("Impossibile aprire il calendario.") }
                }.padding(vertical = 5.dp)) {
                    AppText(if (event.allDay) "Tutto il giorno" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin)), size = 11, color = Color.Gray)
                    AppText(event.title, size = LocalAppStyle.current.settings.hubText)
                }
            }
        }
    }
}
