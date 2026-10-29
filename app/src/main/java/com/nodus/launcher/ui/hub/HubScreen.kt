package com.nodus.launcher.ui.hub

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nodus.launcher.ui.common.ContentGrid
import com.nodus.launcher.notifications.NotificationActionResult
import com.nodus.launcher.notifications.NotificationRef
import com.nodus.launcher.service.ListenerState
import com.nodus.launcher.service.NodusNotificationListener
import com.nodus.launcher.service.NotificationItem
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.util.NotificationUtils
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HubScreen(widgetsModel: HubWidgetsViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val config by widgetsModel.settings.collectAsState()
    val hubMusic by widgetsModel.widgets.music.collectAsState()
    val hubEvents by widgetsModel.widgets.events.collectAsState()
    var widgetNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calendarGranted by remember { mutableStateOf(com.nodus.launcher.widgets.permitted(context, Manifest.permission.READ_CALENDAR)) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarGranted = granted; widgetsModel.refresh()
    }
    val notifications by NodusNotificationListener.notifications.collectAsState()
    val listener by NodusNotificationListener.listenerState.collectAsState()
    var permitted by remember { mutableStateOf(NotificationUtils.isNotificationListenerEnabled(context)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var knownRevisions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var initialized by remember { mutableStateOf(false) }
    var hasNew by remember { mutableStateOf(false) }
    var clearTargets by remember { mutableStateOf<List<NotificationRef>?>(null) }
    var draftKey by rememberSaveable { mutableStateOf<String?>(null) }
    var draftRevision by rememberSaveable { mutableStateOf("") }
    var draftTitle by rememberSaveable { mutableStateOf("") }
    var draftText by rememberSaveable { mutableStateOf("") }
    var draftError by rememberSaveable { mutableStateOf<String?>(null) }
    var replySent by rememberSaveable { mutableStateOf(false) }

    fun notify(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun showResult(result: NotificationActionResult) {
        when (result) {
            NotificationActionResult.SUCCESS -> Unit
            NotificationActionResult.SENT_NOT_REMOVED -> notify("Risposta inviata. La notifica è cambiata o non è eliminabile.")
            NotificationActionResult.DISCONNECTED -> notify("Servizio notifiche scollegato.")
            NotificationActionResult.MISSING -> notify("La notifica non è più disponibile.")
            NotificationActionResult.CHANGED -> notify("La notifica è stata aggiornata. Riprova dalla voce attuale.")
            NotificationActionResult.UNSUPPORTED -> notify("Azione non disponibile per questa notifica.")
            NotificationActionResult.NOT_DISMISSIBLE -> notify("Questa notifica non può essere eliminata.")
            NotificationActionResult.EMPTY_REPLY -> Unit
            NotificationActionResult.FAILED -> notify("Operazione non riuscita. Riprova.")
        }
    }
    fun beginReply(item: NotificationItem) {
        if (!item.ref.canReply) {
            notify("Risposta dall’hub non disponibile")
        } else {
            draftKey = item.key
            draftRevision = item.ref.revision
            draftTitle = item.title.ifBlank { item.appLabel }
            draftText = ""
            draftError = null
            replySent = false
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permitted = NotificationUtils.isNotificationListenerEnabled(context)
                NodusNotificationListener.refreshConnected()
                calendarGranted = com.nodus.launcher.widgets.permitted(context, Manifest.permission.READ_CALENDAR)
                widgetsModel.resume()
            }
            if (event == Lifecycle.Event.ON_PAUSE) widgetsModel.pause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); widgetsModel.pause() }
    }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                widgetNow = System.currentTimeMillis(); widgetsModel.tick()
                delay(60_000 - System.currentTimeMillis() % 60_000)
            }
        }
    }

    LaunchedEffect(notifications) {
        if (listener == ListenerState.CONNECTED) {
            val current = notifications.associate { it.key to it.ref.revision }
            val changed = current.any { (key, revision) -> knownRevisions[key] != revision }
            if (initialized && changed) {
                val previous = knownRevisions
                // Let LazyColumn retain its stable-key anchor before checking for unseen arrivals above it.
                withFrameNanos { }
                val anchorKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
                val anchorIndex = notifications.indexOfFirst { it.key == anchorKey }
                if (anchorIndex > 0 && notifications.take(anchorIndex).any { previous[it.key] != it.ref.revision }) {
                    hasNew = true
                }
            }
            knownRevisions = current
            initialized = true
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollBackward }
            .collect { atTop -> if (atTop) hasNew = false }
    }
    BackHandler { activity?.finish() }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
    ) {
        // The collapsed allowance is captured per notification, so arrivals never collapse a row being read.
        val availableHeight = maxHeight.value
        val rows = notifications.size.coerceAtLeast(1)
        val defaultLines = if (rows <= 3) {
            ((availableHeight - 180f) / rows / 18f - 3f).toInt().coerceIn(2, 10)
        } else 2
        Column(Modifier.fillMaxSize().padding(start = ContentGrid.Outer, end = ContentGrid.End)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { activity?.finish() }, modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(ContentGrid.IconLane)) { HubText("‹", size = 14) }
                        HubText("HUB", size = 14)
                    }
                }
                TextButton(
                    onClick = { clearTargets = notifications.filter { it.ref.dismissible }.map { it.ref } },
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp),
                    enabled = permitted && listener == ListenerState.CONNECTED && notifications.any { it.ref.dismissible }
                ) { HubText("Cancella tutte", size = 12, color = if (notifications.any { it.ref.dismissible }) Color.White else Color.Gray) }
            }
            Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                if (hasNew) TextButton(onClick = {
                    scope.launch { listState.animateScrollToItem(0); hasNew = false }
                }) { HubText("Nuove notifiche ↑", size = 12) }
            }
            Column(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (UiSettings.inHub(config.music)) hubMusic?.let { entry ->
                        item(key = "widget:music") { HubMusicCard(entry, widgetsModel.widgets::control, ::notify) }
                    }
                    if (UiSettings.inHub(config.calendar)) {
                        item(key = "widget:calendar") {
                            HubCalendarCard(hubEvents, widgetNow, calendarGranted,
                                authorize = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) }, notify = ::notify)
                        }
                    }
                    when {
                        !permitted -> item(key = "status:permission") {
                            Column {
                                HubText("Accesso alle notifiche disattivato", color = Color.LightGray)
                                TextButton(modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp), onClick = {
                                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                    catch (_: Exception) { notify("Impossibile aprire le impostazioni Android.") }
                                }) { HubText("Abilita accesso", size = 12) }
                            }
                        }
                        listener != ListenerState.CONNECTED -> item(key = "status:connection") {
                            Column {
                                HubText(if (listener == ListenerState.ERROR) "Impossibile leggere le notifiche" else "Servizio notifiche scollegato", color = Color.LightGray)
                                TextButton(modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp), onClick = {
                                    if (!NodusNotificationListener.reconnect(context)) notify("Riconnessione non riuscita.")
                                }) { HubText("Ricollega", size = 12) }
                            }
                        }
                        notifications.isEmpty() -> item(key = "status:empty") { HubText("Nessuna notifica", color = Color(0xFF9B9B9B)) }
                        else -> items(notifications, key = { it.key }) { item ->
                            NotificationRow(item, defaultLines,
                                onOpen = {
                                    val result = NodusNotificationListener.open(it.ref)
                                    if (result == NotificationActionResult.SUCCESS) activity?.finish() else showResult(result)
                                }, onReply = ::beginReply,
                                onDismiss = { showResult(NodusNotificationListener.dismiss(it.ref)) })
                        }
                    }
                }
                // Only this unoccupied region owns the navigation swipe. Rows never propagate it here.
                Box(Modifier.fillMaxWidth().weight(1f).blankBackSwipe { activity?.finish() })
            }
            SnackbarHost(hostState = snackbar)
        }
    }

    clearTargets?.let { targets ->
        AlertDialog(
            onDismissRequest = { clearTargets = null },
            title = { HubText("Eliminare tutte le notifiche?", size = 16) },
            text = { HubText("Verranno rimosse anche da Android. Le notifiche non eliminabili restano.") },
            confirmButton = {
                TextButton(onClick = {
                    val results = NodusNotificationListener.dismissAll(targets)
                    clearTargets = null
                    val failed = results.count { it !in listOf(NotificationActionResult.SUCCESS, NotificationActionResult.MISSING) }
                    if (failed > 0) notify("Alcune notifiche sono cambiate o non sono state rimosse. Controlla l’elenco.")
                }) { HubText("Elimina", color = Color(0xFFF800FF)) }
            },
            dismissButton = { TextButton(onClick = { clearTargets = null }) { HubText("Annulla") } },
            containerColor = Color(0xFF171717)
        )
    }

    if (draftKey != null) {
        val current = notifications.firstOrNull { it.key == draftKey }
        val active = permitted && listener == ListenerState.CONNECTED && current != null &&
            current.ref.revision == draftRevision && current.ref.canReply
        val unavailableReason = when {
            replySent -> "Risposta inviata. La rimozione non è stata completata: controlla la notifica nell’hub."
            !permitted || listener != ListenerState.CONNECTED -> "Accesso alle notifiche non disponibile. Il testo è conservato."
            current == null -> "La notifica non è più attiva. Il testo è conservato, l’invio è disabilitato."
            current.ref.revision != draftRevision -> "La notifica è stata aggiornata. Copia il testo e riapri la risposta dalla voce attuale."
            !current.ref.canReply -> "Risposta dall’hub non disponibile. Il testo è conservato."
            else -> draftError
        }
        AlertDialog(
            onDismissRequest = { draftKey = null },
            title = { HubText("Rispondi a $draftTitle", size = 16) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it; draftError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { HubText("Risposta") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = LocalAppStyle.current.family, color = Color.White),
                        minLines = 3,
                        maxLines = 8,
                        readOnly = replySent
                    )
                    if (unavailableReason != null) HubText(unavailableReason, size = 12, color = Color.LightGray)
                    if (draftText.isNotEmpty()) TextButton(onClick = {
                        clipboard.setText(AnnotatedString(draftText))
                    }) { HubText("Copia testo", size = 12) }
                }
            },
            confirmButton = {
                TextButton(enabled = active && !replySent && draftText.isNotBlank(), onClick = {
                    if (current != null) {
                        when (val result = NodusNotificationListener.reply(current.ref, draftText)) {
                            NotificationActionResult.SUCCESS -> { draftKey = null; draftText = "" }
                            NotificationActionResult.SENT_NOT_REMOVED -> replySent = true
                            else -> {
                                draftError = "Invio non riuscito. Il testo è conservato."
                                showResult(result)
                            }
                        }
                    }
                }) { HubText("Invia", color = if (active && !replySent && draftText.isNotBlank()) Color(0xFFF800FF) else Color.Gray) }
            },
            dismissButton = { TextButton(onClick = { draftKey = null }) { HubText(if (active && !replySent) "Annulla" else "Chiudi") } },
            containerColor = Color(0xFF171717)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(
    item: NotificationItem,
    initialLines: Int,
    onOpen: (NotificationItem) -> Unit,
    onReply: (NotificationItem) -> Unit,
    onDismiss: (NotificationItem) -> Unit
) {
    var offsetPx by remember(item.key) { mutableFloatStateOf(0f) }
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    val collapsedLines = rememberSaveable(item.key) { initialLines }
    var truncated by remember(item.key) { mutableStateOf(false) }
    val density = LocalDensity.current
    val hubSize = LocalAppStyle.current.settings.hubText
    val thresholdPx = with(density) { 80.dp.toPx() }
    val latestItem by rememberUpdatedState(item)
    val dismiss by rememberUpdatedState(onDismiss)
    val time = remember(item.postTime) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.postTime)) }
    Column(
        Modifier.fillMaxWidth()
            .offset { IntOffset(offsetPx.roundToInt(), 0) }
            .pointerInput(item.key, thresholdPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offsetPx = 0f },
                    onHorizontalDrag = { change, amount -> offsetPx += amount; change.consume() },
                    onDragEnd = {
                        val shouldDismiss = abs(offsetPx) >= thresholdPx
                        offsetPx = 0f
                        if (shouldDismiss) dismiss(latestItem)
                    },
                    onDragCancel = { offsetPx = 0f }
                )
            }
            .combinedClickable(
                onClickLabel = "Apri nell’app",
                onClick = { onOpen(item) },
                onLongClickLabel = "Rispondi nell’hub",
                onLongClick = { onReply(item) }
            )
            .semantics {
                customActions = listOf(CustomAccessibilityAction("Elimina notifica") { onDismiss(item); true })
            }
            .padding(top = 10.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.appLabel, fontFamily = LocalAppStyle.current.family, fontSize = (hubSize - 2).coerceAtLeast(11).sp, lineHeight = ((hubSize - 2).coerceAtLeast(11) * 1.5f).sp, color = Color(0xFF9B9B9B),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            HubText(time, size = 11, color = Color(0xFF9B9B9B))
        }
        if (item.title.isNotBlank()) {
            Text(item.title, fontFamily = LocalAppStyle.current.family, fontWeight = FontWeight.Medium,
                fontSize = hubSize.sp, lineHeight = (hubSize * 1.5f).sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
        }
        if (item.text.isNotBlank()) {
            Text(item.text, fontFamily = LocalAppStyle.current.family, fontSize = (hubSize - 1).coerceAtLeast(11).sp, lineHeight = ((hubSize - 1).coerceAtLeast(11) * 1.5f).sp, color = Color(0xFFBBBBBB),
                maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow },
                modifier = Modifier.padding(top = 2.dp))
            if (truncated && !expanded) TextButton(onClick = { expanded = true }, modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp)) { HubText("Mostra tutto", size = 11) }
        }
        if (!item.ref.dismissible) HubText("Non eliminabile", size = 11, color = Color.Gray)
    }
}

private fun Modifier.blankBackSwipe(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    var distance = 0f
    val thresholdPx = 80.dp.toPx()
    detectHorizontalDragGestures(
        onDragStart = { distance = 0f },
        onHorizontalDrag = { change, amount -> distance += amount; change.consume() },
        onDragEnd = { if (distance >= thresholdPx) onBack(); distance = 0f },
        onDragCancel = { distance = 0f }
    )
}

@Composable
private fun HubText(text: String, size: Int = 13, color: Color = Color.White) {
    val style = LocalAppStyle.current
    val actualSize = (style.settings.hubText + size - 13).coerceAtLeast(11)
    Text(text, fontFamily = style.family, fontSize = actualSize.sp, lineHeight = (actualSize * 1.5f).sp, color = color)
}
