package com.nodus.launcher.search

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nodus.launcher.R
import com.nodus.launcher.model.AppInfo
import com.nodus.launcher.ui.common.Accent
import com.nodus.launcher.ui.common.AppText
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.*

private data class TerminalResult(val id: String, val title: String, val detail: String, val run: () -> Unit)
private data class ContactResult(val id: Long, val lookup: String, val name: String)

@OptIn(ExperimentalLayoutApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TerminalScreen(config: UiSettings, apps: List<AppInfo>, active: Boolean,
                   close: () -> Unit, openSettings: (String) -> Unit, message: (String) -> Unit) {
    val context = LocalContext.current
    val style = LocalAppStyle.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var focused by remember { mutableStateOf(false) }
    var cursorOn by remember { mutableStateOf(true) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var contactsAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { contactsAllowed = it }
    val imeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = active && !imeVisible) { close() }
    LaunchedEffect(active) { if (active) { focus.requestFocus(); keyboard?.show() } else keyboard?.hide() }
    LaunchedEffect(active, focused, lifecycle) {
        cursorOn = true
        if (active && focused) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val motion = runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0 }.getOrDefault(false)
            while (motion && isActive) { delay(600); cursorOn = !cursorOn }
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }
    fun launch(intent: Intent) {
        try { context.startActivity(intent) } catch (_: Exception) { message(context.getString(R.string.term_unavailable)) }
    }
    fun web(text: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(text)}")))
    fun wifi(enabled: Boolean?) {
        if (enabled != null && Build.VERSION.SDK_INT < 29) {
            @Suppress("DEPRECATION")
            val changed = runCatching { (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).setWifiEnabled(enabled) }.getOrDefault(false)
            if (changed) { message(context.getString(R.string.term_wifi_requested)); return }
        }
        message(context.getString(R.string.term_wifi_panel))
        launch(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS))
    }
    val parsed = remember(input.text) { TerminalQuery.parse(input.text) }
    val contacts by produceState<List<ContactResult>>(emptyList(), parsed, contactsAllowed, active) {
        value = emptyList()
        if (active && contactsAllowed && parsed.kind == TerminalQuery.Kind.SEARCH && parsed.text.length >= 2) {
            delay(180)
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val found = mutableListOf<ContactResult>()
                    context.contentResolver.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(parsed.text)),
                        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { cursor ->
                        while (cursor.moveToNext() && found.size < 12) found += ContactResult(cursor.getLong(0), cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty())
                    }
                    found.toList()
                }.getOrDefault(emptyList())
            }
        }
    }
    val results = mutableListOf<TerminalResult>()
    if (input.text.isNotBlank()) {
        when (parsed.kind) {
            TerminalQuery.Kind.WIFI_ON, TerminalQuery.Kind.WIFI_OFF, TerminalQuery.Kind.WIFI -> {
                val enabled = when (parsed.kind) { TerminalQuery.Kind.WIFI_ON -> true; TerminalQuery.Kind.WIFI_OFF -> false; else -> null }
                results += TerminalResult("wifi", context.getString(when(enabled) { true -> R.string.term_wifi_on; false -> R.string.term_wifi_off; null -> R.string.term_wifi }),
                    context.getString(if (Build.VERSION.SDK_INT >= 29) R.string.term_system_panel else R.string.term_device_action)) { wifi(enabled) }
            }
            TerminalQuery.Kind.CAMERA -> results += TerminalResult("camera", context.getString(R.string.term_camera), context.getString(R.string.term_device_action)) { launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
            TerminalQuery.Kind.BLUETOOTH -> results += TerminalResult("bluetooth", "Bluetooth", context.getString(R.string.term_system_panel)) { launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            TerminalQuery.Kind.AIRPLANE -> results += TerminalResult("airplane", context.getString(R.string.ui_airplane_mode), context.getString(R.string.term_system_panel)) { launch(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) }
            TerminalQuery.Kind.SETTINGS -> results += TerminalResult("settings", context.getString(R.string.ui_settings), "noduS") { openSettings("menu") }
            else -> Unit
        }
        if (parsed.kind in setOf(TerminalQuery.Kind.SEARCH, TerminalQuery.Kind.OPEN)) {
            apps.map { it to maxOf(TerminalQuery.score(it.label, parsed.text), if (TerminalQuery.normalize(it.packageName) == TerminalQuery.normalize(parsed.text)) 3 else 0) }
                .filter { it.second > 0 }.sortedWith(compareByDescending<Pair<AppInfo, Int>> { it.second }.thenBy { it.first.label }).take(20).forEach { (app, _) ->
                    results += TerminalResult("app:${app.packageName}", app.label, context.getString(R.string.term_app)) {
                        launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(app.packageName, app.activityName)))
                    }
                }
            for ((page, title) in listOf("home" to R.string.ui_home, "indicators" to R.string.ui_group_indicators, "hub" to R.string.ui_group_notifications,
                "widgets" to R.string.ui_group_widgets, "appearance" to R.string.ui_appearance, "terminal" to R.string.term_title, "backup" to R.string.backup_title)) {
                val keywords = when (page) {
                    "indicators" -> listOf("meteo", "weather", "batteria", "battery", "ora", "clock", "data", "date", "segnale", "signal")
                    "appearance" -> listOf("font", "testo", "text", "icone", "icons", "dots", "puntini")
                    "widgets" -> listOf("musica", "music", "calendario", "calendar")
                    "backup" -> listOf("backup", "import", "export", "importa", "esporta")
                    else -> emptyList()
                }
                if (TerminalQuery.score(context.getString(title), parsed.text) > 0 || keywords.any { TerminalQuery.score(it, parsed.text) > 0 }) results += TerminalResult("settings:$page", context.getString(title), context.getString(R.string.ui_settings)) { openSettings(page) }
            }
            contacts.forEach { c -> results += TerminalResult("contact:${c.id}", c.name, context.getString(R.string.term_contact)) {
                launch(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(c.id, c.lookup)))
            } }
        }
        if (parsed.kind != TerminalQuery.Kind.OPEN) {
            val text = if (parsed.kind == TerminalQuery.Kind.WEB) parsed.text else input.text.trim()
            if (text.isNotEmpty()) results += TerminalResult("web", context.getString(R.string.term_web, text), context.getString(R.string.term_browser)) { web(text) }
        }
    }
    val device = config.terminalDevice.ifEmpty { Build.MODEL.filter { it.isLetterOrDigit() || it in "._-" }.take(40).ifEmpty { "android" } }
    Column(Modifier.fillMaxSize().background(Color.Black).imePadding().padding(horizontal = 24.dp)) {
        TextButton(onClick = { keyboard?.hide(); close() }, enabled = active) { AppText("‹", size = 22, modifier = Modifier.semantics { contentDescription = context.getString(R.string.ui_runtime_return_home) }) }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            androidx.compose.material3.Text("${config.terminalUser.ifEmpty { "issu" }}@$device ", fontFamily = style.family,
                fontSize = 16.sp, color = Accent, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp * .68f).dp))
            BasicTextField(input, onValueChange = { if (it.text.length <= 512 && '\n' !in it.text) input = it },
                enabled = active, textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = style.family),
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.run?.invoke() }),
                cursorBrush = SolidColor(Color.Transparent), onTextLayout = { layout = it },
                modifier = Modifier.weight(1f).heightIn(min = 28.dp).focusRequester(focus).onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = context.getString(R.string.term_input) }
                    .drawWithContent {
                        drawContent()
                        if (focused && cursorOn && input.selection.collapsed) layout?.let { textLayout ->
                            val rect = textLayout.getCursorRect(input.selection.end.coerceIn(0, input.text.length))
                            drawRect(Accent.copy(alpha = .7f), topLeft = androidx.compose.ui.geometry.Offset(rect.left.coerceIn(0f, (size.width - 9.dp.toPx()).coerceAtLeast(0f)), rect.top),
                                size = androidx.compose.ui.geometry.Size(9.dp.toPx(), rect.height))
                        }
                    })
        }
        if (input.text.isNotBlank() && !contactsAllowed && parsed.kind == TerminalQuery.Kind.SEARCH) {
            TextButton(onClick = { contactPermission.launch(Manifest.permission.READ_CONTACTS) }) { AppText(context.getString(R.string.term_enable_contacts), color = Color.Gray) }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(results, key = { it.id }) { result ->
                Column(Modifier.fillMaxWidth().clickable(enabled = active) { result.run() }.padding(vertical = 12.dp)) {
                    AppText(result.title, size = 16)
                    AppText(result.detail, size = 12, color = Color.Gray)
                }
            }
            if (input.text.isNotBlank() && results.isEmpty()) item { AppText(context.getString(R.string.term_no_results), color = Color.Gray) }
        }
    }
}
