package com.nodus.launcher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import com.nodus.launcher.data.AppRepository
import com.nodus.launcher.ui.common.*
import com.nodus.launcher.ui.home.AppItemUi
import com.nodus.launcher.ui.home.AppRow
import com.nodus.launcher.ui.home.MusicWidget
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.util.*
import com.nodus.launcher.widgets.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs

@Composable
fun SettingsScreen() {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }
    val config by prefs.settings.collectAsState(initial = UiSettings())
    var spacingPreview by remember(config.homeSpacing) { mutableIntStateOf(config.homeSpacing) }
    val favorites by prefs.favoriteOrder.collectAsState(initial = emptyList())
    var page by rememberSaveable { mutableStateOf("menu") }
    var refreshed by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<String?>(null) }
    val allApps by produceState(initialValue = emptyList<com.nodus.launcher.model.AppInfo>(), refreshed) {
        value = withContext(Dispatchers.IO) { AppRepository(context).loadAllApps().distinctBy { it.packageName } }
    }
    val calendars by produceState(initialValue = emptyList<CalendarChoice>(), refreshed) {
        value = withContext(Dispatchers.IO) { DeviceWidgets.calendars(context) }
    }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun launchSettings(action: String) {
        try { context.startActivity(Intent(action)) } catch (_: Exception) { message("Impostazione non disponibile su questo telefono.") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshed++
        if (!granted) message("Accesso non concesso. Puoi abilitarlo dalle impostazioni Android.")
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val filename = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "fonts").apply { mkdirs() }
                    val file = File(dir, "import-${System.nanoTime()}.font")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output ->
                            val buffer = ByteArray(8192); var total = 0
                            while (true) { val n = input.read(buffer); if (n < 0) break; total += n
                                require(total <= 8 * 1024 * 1024) { "Il font supera 8 MB." }; output.write(buffer, 0, n) }
                        } } ?: error("File non leggibile.")
                        val header = file.inputStream().use { input -> ByteArray(4).also { check(input.read(it) == 4) } }
                        require(header.contentEquals(byteArrayOf(0,1,0,0)) || String(header, Charsets.US_ASCII) in listOf("OTTO", "true")) { "Seleziona un file TTF o OTF valido." }
                        Typeface.createFromFile(file)
                        "fonts/${file.name}"
                    } catch (e: Exception) { file.delete(); throw e }
                }
                prefs.text("font_file", filename); prefs.text("font", "custom")
                message("Font importato e applicato.")
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { message("Font non valido o non leggibile (TTF/OTF, massimo 8 MB).") }
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshed++ }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    fun back() { if (page == "menu") (context as? Activity)?.finish() else page = if (page in listOf("favorites", "calendars")) "home" else "menu" }
    BackHandler { back() }
    Box(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            TextButton(onClick = { back() }) { AppText("‹ ${when(page) { "home" -> "HOME"; "favorites" -> "PREFERITI"; "calendars" -> "CALENDARI"; "hub" -> "HUB NOTIFICHE"; "appearance" -> "ASPETTO"; "about" -> "INFORMAZIONI"; else -> "IMPOSTAZIONI" }}", size = 14) }
            key(page) { Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                when (page) {
                    "menu" -> {
                        MenuRow("Home", "App, preferiti, ora e widget") { page = "home" }
                        MenuRow("Hub notifiche", "Accesso e gesti") { page = "hub" }
                        MenuRow("Aspetto", "Font, dimensioni e puntini") { page = "appearance" }
                        MenuRow("Informazioni su noduS", "Versione, funzionalità e benvenuto") { page = "about" }
                        Section("Sistema")
                        Action("Imposta come launcher predefinito") { launchSettings(Settings.ACTION_HOME_SETTINGS) }
                    }
                    "home" -> {
                        Section("App della home")
                        MenuRow("Numero massimo di app", config.count.toString()) { dialog = "count" }
                        MenuRow("Ordina i preferiti", favorites.size.toString()) { page = "favorites" }
                        AppText("Posti liberi: aperture negli ultimi 7 giorni, con aggiornamento giornaliero. Senza accesso, ordine alfabetico.", size = 12, color = Color.Gray)
                        Action(if (RecentApps.hasAccess(context)) "Accesso utilizzo: attivo" else "Autorizza dati di utilizzo") { launchSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
                        Section("Area superiore")
                        Toggle("Nascondi barra di stato", config.hideStatusBar) { scope.launch { prefs.bool("hide_status_bar", it) } }
                        Toggle("Ora", config.clock) { scope.launch { prefs.bool("show_clock", it) } }
                        Toggle("Data", config.date) { scope.launch { prefs.bool("show_date", it) } }
                        Toggle("Batteria · percentuale", config.battery) { scope.launch { prefs.bool("show_battery", it) } }
                        Toggle("Segnale e Wi-Fi · solo icone", config.signal) { scope.launch { prefs.bool("show_signal", it) } }
                        Toggle("Meteo accanto all’ora", config.weather) { enabled ->
                            scope.launch { prefs.bool("show_weather", enabled) }
                            if (enabled && !permitted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        if (config.weather) {
                            Toggle("Meteo solo testuale", config.weatherText) { scope.launch { prefs.bool("weather_text", it) } }
                            AppText("Posizione del telefono. Meteo: Open-Meteo. Coordinate approssimate inviate solo per aggiornare il meteo.", size = 11, color = Color.Gray)
                            if (!permitted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) Action("Autorizza posizione") { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                        }
                        Section("Posizione verticale della home")
                        AppText("Sposta tutto il blocco, mantenendo le distanze. Il massimo effettivo si adatta allo spazio disponibile.", size = 12, color = Color.Gray)
                        SettingSlider("Spostamento richiesto", config.padding, 0..200, "dp") { scope.launch { prefs.number("home_top_padding", it) } }
                        Section("Spaziatura")
                        SettingSlider("Spaziatura righe e widget", config.homeSpacing, 0..32, "dp", previewChange = { spacingPreview = it }) {
                            scope.launch { prefs.number("home_spacing", it) }
                        }
                        AppText("Riduci lo spazio tra le app e attorno ai widget senza cambiare il font. La home resta fissa.", size = 11, color = Color.Gray)
                        AppText("Anteprima", size = 11, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        CompositionLocalProvider(LocalAppStyle provides LocalAppStyle.current.copy(settings = config.copy(homeSpacing = spacingPreview))) {
                            Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(12.dp)) {
                                val icon = context.packageManager.defaultActivityIcon
                                listOf("Telefono", "Messaggi").forEach { label ->
                                    AppRow(AppItemUi(com.nodus.launcher.model.AppInfo(label, "preview.$label", "", icon), 0xFF69A9D8.toInt()), {}, {})
                                }
                                MusicWidget(MusicEntry("Titolo brano", "Artista", true, null, false, false, false, "preview"), open = {}, control = {})
                            }
                        }
                        Section("Piccoli widget")
                        Destination("Musica", config.music) { scope.launch { prefs.text("music_destination", it) } }
                        AppText("Visibile in riproduzione e pausa. Richiede accesso alle notifiche.", size = 11, color = Color.Gray)
                        if (!NotificationUtils.isNotificationListenerEnabled(context)) Action("Autorizza accesso notifiche") { launchSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                        Destination("Calendario", config.calendar) { selected ->
                            scope.launch { prefs.text("calendar_destination", selected) }
                            if ((UiSettings.onHome(selected) || UiSettings.inHub(selected)) && !permitted(context, Manifest.permission.READ_CALENDAR)) permission.launch(Manifest.permission.READ_CALENDAR)
                        }
                        AppText("Widget con gli eventi di oggi non conclusi nella destinazione scelta. I promemoria restano notifiche Android.", size = 11, color = Color.Gray)
                        if (UiSettings.onHome(config.calendar) || UiSettings.inHub(config.calendar)) {
                            if (!permitted(context, Manifest.permission.READ_CALENDAR)) Action("Autorizza calendario") { permission.launch(Manifest.permission.READ_CALENDAR) }
                            else MenuRow("Calendari da mostrare", if (config.calendarIds == "*") "Tutti quelli visibili" else "Selezione personalizzata") { page = "calendars" }
                        }
                        Action("Autorizzazioni dell’app in Android") {
                            try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }
                            catch (_: Exception) { message("Impostazione non disponibile.") }
                        }
                    }
                    "favorites" -> {
                        AppText("Trascina per ordinare. Le frecce permettono di spostare una voce senza trascinarla.", size = 12, color = Color.Gray)
                        FavoriteOrder(favorites, allApps.associateBy { it.packageName }) { ids -> scope.launch { prefs.reorder(ids) } }
                        if (favorites.isEmpty()) AppText("Nessun preferito. Tieni premuto su un’app nella home per aggiungerla.", color = Color.Gray)
                    }
                    "calendars" -> {
                        Toggle("Tutti i calendari visibili", config.calendarIds == "*") { checked -> scope.launch { prefs.text("calendar_ids", if (checked) "*" else "") } }
                        calendars.forEach { calendar ->
                            val selected = config.calendarIds == "*" || calendar.id in config.calendarIds.split(',')
                            Toggle(calendar.name, selected) { checked ->
                                val ids = (if (config.calendarIds == "*") calendars.map { it.id } else config.calendarIds.split(',').filter { it.isNotBlank() }).toMutableSet()
                                if (checked) ids.add(calendar.id) else ids.remove(calendar.id)
                                scope.launch { prefs.text("calendar_ids", ids.joinToString(",")) }
                            }
                        }
                        if (calendars.isEmpty()) AppText("Nessun calendario visibile o accesso non disponibile.", color = Color.Gray)
                    }
                    "hub" -> {
                        Section("Accesso alle notifiche")
                        AppText(if (NotificationUtils.isNotificationListenerEnabled(context)) "Accesso: attivo" else "Accesso: disattivo")
                        Action("Gestisci accesso") { launchSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                        Section("Gesti")
                        AppText("Tap: apre il contenuto nell’app.\n\nPressione prolungata: risposta diretta, se supportata. Dopo l’invio la notifica viene rimossa.\n\nSwipe sulla notifica: elimina, a destra o a sinistra.\n\nSwipe destro nel vuoto: torna alla home.\n\nCancella tutte: richiede conferma.", color = Color.LightGray)
                    }
                    "appearance" -> {
                        Section("Carattere")
                        Choice("Font dell’interfaccia", config.font, listOf("default" to "IBM Plex Mono", "system" to "Sistema", "custom" to "Importa TTF/OTF")) { value ->
                            if (value == "custom") importer.launch(arrayOf("*/*")) else scope.launch { prefs.text("font", value) }
                        }
                        if (config.font == "custom") { AppText("Font importato attivo", size = 11, color = Color.Gray); Action("Sostituisci font") { importer.launch(arrayOf("*/*")) } }
                        SettingSlider("Testo home", config.homeText, 12..28) { scope.launch { prefs.number("home_text", it) } }
                        SettingSlider("Testo hub", config.hubText, 12..24) { scope.launch { prefs.number("hub_text", it) } }
                        Section("Icone")
                        Toggle("Usa puntini", config.dots) { scope.launch { prefs.bool("use_dots", it) } }
                        AppText(if (config.dots) "Puntini" else "Icone originali delle app", size = 11, color = Color.Gray)
                        if (config.dots) {
                            SettingSlider("Dimensione puntini", config.dotSize, 4..16) { scope.launch { prefs.number("dot_size", it) } }
                            Toggle("Colore uniforme", config.dotUniform) { scope.launch { prefs.bool("dot_uniform", it) } }
                            if (config.dotUniform) Action("Colore: #%06X".format(config.dotColor and 0xFFFFFF)) { dialog = "color" }
                        }
                        Section("Anteprima")
                        AppRow(AppItemUi(com.nodus.launcher.model.AppInfo("Telegram", "preview", "", context.packageManager.defaultActivityIcon), 0xFF69A9D8.toInt()), {}, {})
                        Text("Marco\nArrivo tra dieci minuti.", fontFamily = LocalAppStyle.current.family, fontSize = config.hubText.sp, color = Color.LightGray)
                        Action("Ripristina aspetto predefinito") { dialog = "reset" }
                    }
                    "about" -> com.nodus.launcher.ui.welcome.AppIdentityContent(welcome = false)
                }
            } }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    when (dialog) {
        "count" -> AlertDialog(onDismissRequest = { dialog = null }, title = { AppText("Numero massimo di app", size = 16) }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                (1..maxOf(24, favorites.size + 8, config.count)).forEach { count -> TextButton(onClick = {
                    scope.launch { if (!prefs.setVisibleAppsCount(count)) message("Hai ${favorites.size} preferiti: rimuovine uno prima di ridurre il limite.") }
                    dialog = null
                }) { AppText(count.toString()) } }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = { dialog = null }) { AppText("Annulla") } })
        "color" -> {
            var color by remember { mutableStateOf("%06X".format(config.dotColor and 0xFFFFFF)) }
            AlertDialog(onDismissRequest = { dialog = null }, title = { AppText("Colore uniforme", size = 16) }, text = {
                OutlinedTextField(value = color, onValueChange = { if (it.length <= 7) color = it.removePrefix("#") }, label = { AppText("Esadecimale RRGGBB") }, singleLine = true)
            }, confirmButton = { TextButton(enabled = color.matches(Regex("[0-9a-fA-F]{6}")), onClick = {
                scope.launch { prefs.number("dot_color", (0xFF000000L or color.toLong(16)).toInt()) }; dialog = null
            }) { AppText("Applica", color = Accent) } }, dismissButton = { TextButton(onClick = { dialog = null }) { AppText("Annulla") } })
        }
        "reset" -> AlertDialog(onDismissRequest = { dialog = null }, title = { AppText("Ripristinare l’aspetto?", size = 16) },
            confirmButton = { TextButton(onClick = { scope.launch { prefs.resetAppearance() }; dialog = null }) { AppText("Ripristina", color = Accent) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { AppText("Annulla") } })
    }
}

@Composable
private fun Section(title: String) { AppText(title, size = 14, medium = true, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) }
@Composable
private fun Action(title: String, onClick: () -> Unit) { TextButton(onClick = onClick) { AppText(title, color = Accent) } }
@Composable
private fun MenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
        Column(Modifier.weight(1f)) { AppText(title, size = 14); if (subtitle.isNotBlank()) AppText(subtitle, size = 11, color = Color.Gray) }
        AppText("›", size = 20, color = Color.Gray)
    }
}
@Composable
private fun Toggle(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AppText(title, modifier = Modifier.weight(1f)); Switch(checked = value, onCheckedChange = onChange)
    }
}
@Composable
private fun Choice(title: String, selected: String, choices: List<Pair<String, String>>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MenuRow(title, choices.firstOrNull { it.first == selected }?.second.orEmpty()) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (key, label) -> DropdownMenuItem(text = { AppText(label) }, onClick = { onChange(key); expanded = false }) }
        }
    }
}
@Composable
private fun Destination(title: String, selected: String, onChange: (String) -> Unit) = Choice(title, selected,
    listOf("home" to "Solo home", "hub" to "Solo hub", "both" to "Home e hub", "none" to "Nessuno"), onChange)
@Composable
private fun SettingSlider(title: String, value: Int, range: IntRange, unit: String = "", previewChange: (Int) -> Unit = {}, change: (Int) -> Unit) {
    var preview by remember(value) { mutableFloatStateOf(value.toFloat()) }
    AppText("$title: ${preview.toInt()} $unit")
    Slider(value = preview, onValueChange = { preview = it; previewChange(it.toInt()) }, onValueChangeFinished = { change(preview.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0))
}
@Composable
private fun FavoriteOrder(ids: List<String>, apps: Map<String, com.nodus.launcher.model.AppInfo>, save: (List<String>) -> Unit) {
    var order by remember { mutableStateOf(ids) }; var dragging by remember { mutableStateOf<String?>(null) }
    var rowHeight by remember { mutableFloatStateOf(56f) }; val latestOrder by rememberUpdatedState(order)
    LaunchedEffect(ids) { if (dragging == null) order = ids }
    val latestSave by rememberUpdatedState(save)
    order.forEach { id -> key(id) {
        Row(Modifier.fillMaxWidth().background(if (dragging == id) Color(0xFF171717) else Color.Black)
            .onSizeChanged { rowHeight = it.height.toFloat() }
            .pointerInput(id) { var dy = 0f
                detectDragGesturesAfterLongPress(onDragStart = { dragging = id; dy = 0f }, onDragEnd = { dragging = null; latestSave(latestOrder) },
                    onDragCancel = { dragging = null; order = ids }, onDrag = { change, amount ->
                        change.consume(); dy += amount.y
                        if (abs(dy) >= rowHeight) {
                            val old = order.indexOf(id); val next = (old + if (dy > 0) 1 else -1).coerceIn(0, order.lastIndex)
                            if (old != next) { order = order.toMutableList().apply { add(next, removeAt(old)) }; dy = 0f }
                        }
                    })
            }, verticalAlignment = Alignment.CenterVertically) {
            AppText("⠿", color = Color.Gray, modifier = Modifier.padding(end = 12.dp))
            AppText(apps[id]?.label ?: id, modifier = Modifier.weight(1f))
            val index = order.indexOf(id)
            listOf(-1 to "↑", 1 to "↓").forEach { (delta, arrow) -> TextButton(enabled = index + delta in order.indices, onClick = {
                order = order.toMutableList().apply { add(index + delta, removeAt(index)) }; save(order)
            }, contentPadding = PaddingValues(4.dp), modifier = Modifier.widthIn(min = 36.dp).semantics { contentDescription = "${if (delta < 0) "Sposta su" else "Sposta giù"} ${apps[id]?.label ?: id}" }) { AppText(arrow, color = Color.LightGray) } }
        }
    } }
}
