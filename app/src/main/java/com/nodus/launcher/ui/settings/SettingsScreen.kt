package com.nodus.launcher.ui.settings

import com.nodus.launcher.R
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
import com.nodus.launcher.ui.home.HomeHeader
import com.nodus.launcher.widgets.WeatherEntry
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
    var headerPreview by remember(config) { mutableStateOf(config) }
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
        try { context.startActivity(Intent(action)) } catch (_: Exception) { message(context.getString(R.string.ui_this_setting_is_unavailable_on_this_phone)) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshed++
        if (!granted) message(context.getString(R.string.ui_access_not_granted_you_can_enable_it))
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
                                require(total <= 8 * 1024 * 1024) { context.getString(R.string.ui_the_font_exceeds_8_mb) }; output.write(buffer, 0, n) }
                        } } ?: error(context.getString(R.string.ui_cannot_read_this_file))
                        val header = file.inputStream().use { input -> ByteArray(4).also { check(input.read(it) == 4) } }
                        require(header.contentEquals(byteArrayOf(0,1,0,0)) || String(header, Charsets.US_ASCII) in listOf("OTTO", "true")) { context.getString(R.string.ui_select_a_valid_ttf_or_otf_file) }
                        Typeface.createFromFile(file)
                        "fonts/${file.name}"
                    } catch (e: Exception) { file.delete(); throw e }
                }
                prefs.text("font_file", filename); prefs.text("font", "custom")
                message(context.getString(R.string.ui_font_imported_and_applied))
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { message(context.getString(R.string.ui_invalid_or_unreadable_font_ttf_otf_maximum)) }
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
            TextButton(onClick = { back() }) { AppText("‹ ${when(page) { "home" -> context.getString(R.string.ui_home); "favorites" -> context.getString(R.string.ui_favorites); "calendars" -> context.getString(R.string.ui_calendars); "hub" -> context.getString(R.string.ui_notification_hub); "appearance" -> context.getString(R.string.ui_appearance); "about" -> context.getString(R.string.ui_about); else -> context.getString(R.string.ui_settings) }}", size = 14) }
            key(page) { Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                when (page) {
                    "menu" -> {
                        MenuRow(context.getString(R.string.ui_home_2), context.getString(R.string.ui_apps_favorites_clock_and_widgets)) { page = "home" }
                        MenuRow(context.getString(R.string.ui_notification_hub_2), context.getString(R.string.ui_access_and_gestures)) { page = "hub" }
                        MenuRow(context.getString(R.string.ui_appearance_2), context.getString(R.string.ui_fonts_sizes_and_dots)) { page = "appearance" }
                        MenuRow(context.getString(R.string.ui_about_nodus), context.getString(R.string.ui_version_features_and_welcome)) { page = "about" }
                        Section(context.getString(R.string.ui_system))
                        Action(context.getString(R.string.ui_set_as_default_launcher)) { launchSettings(Settings.ACTION_HOME_SETTINGS) }
                    }
                    "home" -> {
                        Section(context.getString(R.string.ui_home_apps))
                        MenuRow(context.getString(R.string.ui_maximum_number_of_apps), config.count.toString()) { dialog = "count" }
                        MenuRow(context.getString(R.string.ui_reorder_favorites), favorites.size.toString()) { page = "favorites" }
                        AppText(context.getString(R.string.ui_empty_slots_use_app_openings_over_the), size = 12, color = Color.Gray)
                        Action(if (RecentApps.hasAccess(context)) context.getString(R.string.ui_usage_access_enabled) else context.getString(R.string.ui_allow_usage_access)) { launchSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
                        Section(context.getString(R.string.ui_header))
                        Toggle(context.getString(R.string.ui_hide_status_bar), config.hideStatusBar) { scope.launch { prefs.bool("hide_status_bar", it) } }
                        Toggle(context.getString(R.string.ui_clock), config.clock) { scope.launch { prefs.bool("show_clock", it) } }
                        Toggle(context.getString(R.string.ui_date), config.date) { scope.launch { prefs.bool("show_date", it) } }
                        Toggle(context.getString(R.string.ui_battery_percentage), config.battery) { scope.launch { prefs.bool("show_battery", it) } }
                        Toggle(context.getString(R.string.ui_signal_and_wi_fi_icons_only), config.signal) { scope.launch { prefs.bool("show_signal", it) } }
                        Toggle(context.getString(R.string.ui_weather), config.weather) { enabled ->
                            scope.launch { prefs.bool("show_weather", enabled) }
                            if (enabled && !permitted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        if (config.weather) {
                            Toggle(context.getString(R.string.ui_text_only_weather_standard_layout), config.weatherText) { scope.launch { prefs.bool("weather_text", it) } }
                            AppText(context.getString(R.string.ui_phone_location_weather_open_meteo_approximate_coordinates), size = 11, color = Color.Gray)
                            if (!permitted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) Action(context.getString(R.string.ui_allow_location_access)) { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                        }
                        Section(context.getString(R.string.ui_header_sizes_and_layout))
                        Toggle(context.getString(R.string.ui_super_compact_layout), config.compactHeader) { enabled ->
                            scope.launch { prefs.headerPreset(enabled) }
                        }
                        AppText(context.getString(R.string.ui_super_compact_clock_above_date_weather_signal), size = 11, color = Color.Gray)
                        SettingSlider(context.getString(R.string.ui_clock_size), config.clockSize, 18..64, "sp", previewChange = { headerPreview = headerPreview.copy(clockSize = it) }) { scope.launch { prefs.number("clock_size", it) } }
                        SettingSlider(context.getString(R.string.ui_date_size), config.dateSize, 10..28, "sp", previewChange = { headerPreview = headerPreview.copy(dateSize = it) }) { scope.launch { prefs.number("date_size", it) } }
                        SettingSlider(context.getString(R.string.ui_weather_size), config.weatherSize, 10..28, "sp", previewChange = { headerPreview = headerPreview.copy(weatherSize = it) }) { scope.launch { prefs.number("weather_size", it) } }
                        SettingSlider(context.getString(R.string.ui_battery_size), config.batterySize, 10..28, "sp", previewChange = { headerPreview = headerPreview.copy(batterySize = it) }) { scope.launch { prefs.number("battery_size", it) } }
                        SettingSlider(context.getString(R.string.ui_signal_and_wi_fi_size), config.signalSize, 12..32, "dp", previewChange = { headerPreview = headerPreview.copy(signalSize = it) }) { scope.launch { prefs.number("signal_size", it) } }
                        AppText(context.getString(R.string.ui_preview_with_sample_data), size = 11, color = Color.Gray)
                        HomeHeader(headerPreview, 0L, WeatherEntry(18, 0, context.getString(R.string.ui_location), 0L), null, 86, 3, true)
                        Action(context.getString(R.string.ui_reset_header)) { scope.launch { prefs.resetHeader() } }
                        Section(context.getString(R.string.ui_home_vertical_position))
                        AppText(context.getString(R.string.ui_move_the_entire_block_while_keeping_its), size = 12, color = Color.Gray)
                        SettingSlider(context.getString(R.string.ui_requested_offset), config.padding, 0..200, "dp") { scope.launch { prefs.number("home_top_padding", it) } }
                        Section(context.getString(R.string.ui_spacing))
                        SettingSlider(context.getString(R.string.ui_row_and_widget_spacing), config.homeSpacing, 0..32, "dp", previewChange = { spacingPreview = it }) {
                            scope.launch { prefs.number("home_spacing", it) }
                        }
                        AppText(context.getString(R.string.ui_reduce_spacing_between_apps_and_around_widgets), size = 11, color = Color.Gray)
                        AppText(context.getString(R.string.ui_preview), size = 11, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        CompositionLocalProvider(LocalAppStyle provides LocalAppStyle.current.copy(settings = config.copy(homeSpacing = spacingPreview))) {
                            Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(12.dp)) {
                                val icon = context.packageManager.defaultActivityIcon
                                listOf(context.getString(R.string.ui_phone), context.getString(R.string.ui_messages)).forEach { label ->
                                    AppRow(AppItemUi(com.nodus.launcher.model.AppInfo(label, "preview.$label", "", icon), 0xFF69A9D8.toInt()), {}, {})
                                }
                                MusicWidget(MusicEntry(context.getString(R.string.ui_track_title), context.getString(R.string.ui_artist), true, null, false, false, false, "preview"), open = {}, control = {})
                            }
                        }
                        Section(context.getString(R.string.ui_small_widgets))
                        Destination(context.getString(R.string.ui_music), config.music) { scope.launch { prefs.text("music_destination", it) } }
                        AppText(context.getString(R.string.ui_visible_while_playing_or_paused_requires_notification), size = 11, color = Color.Gray)
                        if (!NotificationUtils.isNotificationListenerEnabled(context)) Action(context.getString(R.string.ui_allow_notification_access)) { launchSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                        Destination(context.getString(R.string.ui_calendar), config.calendar) { selected ->
                            scope.launch { prefs.text("calendar_destination", selected) }
                            if ((UiSettings.onHome(selected) || UiSettings.inHub(selected)) && !permitted(context, Manifest.permission.READ_CALENDAR)) permission.launch(Manifest.permission.READ_CALENDAR)
                        }
                        AppText(context.getString(R.string.ui_shows_today_s_unfinished_events_in_the), size = 11, color = Color.Gray)
                        if (UiSettings.onHome(config.calendar) || UiSettings.inHub(config.calendar)) {
                            if (!permitted(context, Manifest.permission.READ_CALENDAR)) Action(context.getString(R.string.ui_allow_calendar_access)) { permission.launch(Manifest.permission.READ_CALENDAR) }
                            else MenuRow(context.getString(R.string.ui_calendars_to_show), if (config.calendarIds == "*") context.getString(R.string.ui_all_visible_calendars) else context.getString(R.string.ui_custom_selection)) { page = "calendars" }
                        }
                        Action(context.getString(R.string.ui_app_permissions_in_android)) {
                            try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }
                            catch (_: Exception) { message(context.getString(R.string.ui_setting_unavailable)) }
                        }
                    }
                    "favorites" -> {
                        AppText(context.getString(R.string.ui_drag_to_reorder_use_the_arrows_to), size = 12, color = Color.Gray)
                        FavoriteOrder(favorites, allApps.associateBy { it.packageName }) { ids -> scope.launch { prefs.reorder(ids) } }
                        if (favorites.isEmpty()) AppText(context.getString(R.string.ui_no_favorites_hold_an_app_on_the), color = Color.Gray)
                    }
                    "calendars" -> {
                        Toggle(context.getString(R.string.ui_all_visible_calendars_2), config.calendarIds == "*") { checked -> scope.launch { prefs.text("calendar_ids", if (checked) "*" else "") } }
                        calendars.forEach { calendar ->
                            val selected = config.calendarIds == "*" || calendar.id in config.calendarIds.split(',')
                            Toggle(calendar.name, selected) { checked ->
                                val ids = (if (config.calendarIds == "*") calendars.map { it.id } else config.calendarIds.split(',').filter { it.isNotBlank() }).toMutableSet()
                                if (checked) ids.add(calendar.id) else ids.remove(calendar.id)
                                scope.launch { prefs.text("calendar_ids", ids.joinToString(",")) }
                            }
                        }
                        if (calendars.isEmpty()) AppText(context.getString(R.string.ui_no_visible_calendars_or_access_unavailable), color = Color.Gray)
                    }
                    "hub" -> {
                        Section(context.getString(R.string.ui_notification_access))
                        AppText(if (NotificationUtils.isNotificationListenerEnabled(context)) context.getString(R.string.ui_access_enabled) else context.getString(R.string.ui_access_disabled))
                        Action(context.getString(R.string.ui_manage_access)) { launchSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                        Section(context.getString(R.string.ui_gestures))
                        AppText(context.getString(R.string.ui_tap_open_content_in_the_app_n), color = Color.LightGray)
                    }
                    "appearance" -> {
                        Section(context.getString(R.string.ui_typeface))
                        Choice(context.getString(R.string.ui_interface_font), config.font, listOf("default" to "IBM Plex Mono", "system" to context.getString(R.string.ui_system), "custom" to context.getString(R.string.ui_import_ttf_otf))) { value ->
                            if (value == "custom") importer.launch(arrayOf("*/*")) else scope.launch { prefs.text("font", value) }
                        }
                        if (config.font == "custom") { AppText(context.getString(R.string.ui_imported_font_active), size = 11, color = Color.Gray); Action(context.getString(R.string.ui_replace_font)) { importer.launch(arrayOf("*/*")) } }
                        SettingSlider(context.getString(R.string.ui_home_text), config.homeText, 12..28) { scope.launch { prefs.number("home_text", it) } }
                        SettingSlider(context.getString(R.string.ui_hub_text), config.hubText, 12..24) { scope.launch { prefs.number("hub_text", it) } }
                        Section(context.getString(R.string.ui_icons))
                        Toggle(context.getString(R.string.ui_use_dots), config.dots) { scope.launch { prefs.bool("use_dots", it) } }
                        AppText(if (config.dots) context.getString(R.string.ui_dots) else context.getString(R.string.ui_original_app_icons), size = 11, color = Color.Gray)
                        if (config.dots) {
                            SettingSlider(context.getString(R.string.ui_dot_size), config.dotSize, 4..16) { scope.launch { prefs.number("dot_size", it) } }
                            Toggle(context.getString(R.string.ui_uniform_color), config.dotUniform) { scope.launch { prefs.bool("dot_uniform", it) } }
                            if (config.dotUniform) Action(context.getString(R.string.ui_color_1_06x, config.dotColor and 0xFFFFFF)) { dialog = "color" }
                        }
                        Section(context.getString(R.string.ui_preview))
                        AppRow(AppItemUi(com.nodus.launcher.model.AppInfo("Telegram", "preview", "", context.packageManager.defaultActivityIcon), 0xFF69A9D8.toInt()), {}, {})
                        Text(context.getString(R.string.ui_alex_ni_will_arrive_in_ten_minutes), fontFamily = LocalAppStyle.current.family, fontSize = config.hubText.sp, color = Color.LightGray)
                        Action(context.getString(R.string.ui_reset_default_appearance)) { dialog = "reset" }
                    }
                    "about" -> com.nodus.launcher.ui.welcome.AppIdentityContent(welcome = false)
                }
            } }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    when (dialog) {
        "count" -> AlertDialog(onDismissRequest = { dialog = null }, title = { AppText(context.getString(R.string.ui_maximum_number_of_apps), size = 16) }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                (1..maxOf(24, favorites.size + 8, config.count)).forEach { count -> TextButton(onClick = {
                    scope.launch { if (!prefs.setVisibleAppsCount(count)) message(context.resources.getQuantityString(R.plurals.ui_favorite_limit, favorites.size, favorites.size)) }
                    dialog = null
                }) { AppText(count.toString()) } }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = { dialog = null }) { AppText(context.getString(R.string.ui_cancel)) } })
        "color" -> {
            var color by remember { mutableStateOf("%06X".format(config.dotColor and 0xFFFFFF)) }
            AlertDialog(onDismissRequest = { dialog = null }, title = { AppText(context.getString(R.string.ui_uniform_color), size = 16) }, text = {
                OutlinedTextField(value = color, onValueChange = { if (it.length <= 7) color = it.removePrefix("#") }, label = { AppText(context.getString(R.string.ui_hexadecimal_rrggbb)) }, singleLine = true)
            }, confirmButton = { TextButton(enabled = color.matches(Regex("[0-9a-fA-F]{6}")), onClick = {
                scope.launch { prefs.number("dot_color", (0xFF000000L or color.toLong(16)).toInt()) }; dialog = null
            }) { AppText(context.getString(R.string.ui_apply), color = Accent) } }, dismissButton = { TextButton(onClick = { dialog = null }) { AppText(context.getString(R.string.ui_cancel)) } })
        }
        "reset" -> AlertDialog(onDismissRequest = { dialog = null }, title = { AppText(context.getString(R.string.ui_reset_appearance), size = 16) },
            confirmButton = { TextButton(onClick = { scope.launch { prefs.resetAppearance() }; dialog = null }) { AppText(context.getString(R.string.ui_reset), color = Accent) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { AppText(context.getString(R.string.ui_cancel)) } })
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
private fun Destination(title: String, selected: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    Choice(title, selected,
    listOf("home" to context.getString(R.string.ui_home_only), "hub" to context.getString(R.string.ui_hub_only), "both" to context.getString(R.string.ui_home_and_hub), "none" to context.getString(R.string.ui_none)), onChange)
}
@Composable
private fun SettingSlider(title: String, value: Int, range: IntRange, unit: String = "", previewChange: (Int) -> Unit = {}, change: (Int) -> Unit) {
    var preview by remember(value) { mutableFloatStateOf(value.toFloat()) }
    AppText("$title: ${preview.toInt()} $unit")
    Slider(value = preview, onValueChange = { preview = it; previewChange(it.toInt()) }, onValueChangeFinished = { change(preview.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0))
}
@Composable
private fun FavoriteOrder(ids: List<String>, apps: Map<String, com.nodus.launcher.model.AppInfo>, save: (List<String>) -> Unit) {
    val context = LocalContext.current
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
            }, contentPadding = PaddingValues(4.dp), modifier = Modifier.widthIn(min = 36.dp).semantics { contentDescription = "${if (delta < 0) context.getString(R.string.ui_move_up) else context.getString(R.string.ui_move_down)} ${apps[id]?.label ?: id}" }) { AppText(arrow, color = Color.LightGray) } }
        }
    } }
}
