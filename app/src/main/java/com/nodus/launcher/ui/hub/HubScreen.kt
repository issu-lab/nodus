package com.nodus.launcher.ui.hub

import com.nodus.launcher.R
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
            NotificationActionResult.SENT_NOT_REMOVED -> notify(context.getString(R.string.ui_runtime_reply_sent_the_notification_changed_or_cannot))
            NotificationActionResult.DISCONNECTED -> notify(context.getString(R.string.ui_runtime_notification_service_disconnected))
            NotificationActionResult.MISSING -> notify(context.getString(R.string.ui_runtime_the_notification_is_no_longer_available))
            NotificationActionResult.CHANGED -> notify(context.getString(R.string.ui_runtime_the_notification_was_updated_try_again_from))
            NotificationActionResult.UNSUPPORTED -> notify(context.getString(R.string.ui_runtime_action_unavailable_for_this_notification))
            NotificationActionResult.NOT_DISMISSIBLE -> notify(context.getString(R.string.ui_runtime_this_notification_cannot_be_dismissed))
            NotificationActionResult.EMPTY_REPLY -> Unit
            NotificationActionResult.FAILED -> notify(context.getString(R.string.ui_runtime_operation_failed_try_again))
        }
    }
    fun beginReply(item: NotificationItem) {
        if (!item.ref.canReply) {
            notify(context.getString(R.string.ui_runtime_replying_from_the_hub_is_unavailable))
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
                        HubText(context.getString(R.string.ui_runtime_hub), size = 14)
                    }
                }
                TextButton(
                    onClick = { clearTargets = notifications.filter { it.ref.dismissible }.map { it.ref } },
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp),
                    enabled = permitted && listener == ListenerState.CONNECTED && notifications.any { it.ref.dismissible }
                ) { HubText(context.getString(R.string.ui_runtime_clear_all), size = 12, color = if (notifications.any { it.ref.dismissible }) Color.White else Color.Gray) }
            }
            Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                if (hasNew) TextButton(onClick = {
                    scope.launch { listState.animateScrollToItem(0); hasNew = false }
                }) { HubText(context.getString(R.string.ui_runtime_new_notifications), size = 12) }
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
                                HubText(context.getString(R.string.ui_runtime_notification_access_disabled), color = Color.LightGray)
                                TextButton(modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp), onClick = {
                                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                    catch (_: Exception) { notify(context.getString(R.string.ui_runtime_cannot_open_android_settings)) }
                                }) { HubText(context.getString(R.string.ui_runtime_enable_access), size = 12) }
                            }
                        }
                        listener != ListenerState.CONNECTED -> item(key = "status:connection") {
                            Column {
                                HubText(if (listener == ListenerState.ERROR) context.getString(R.string.ui_runtime_cannot_read_notifications) else context.getString(R.string.ui_runtime_notification_service_disconnected_2), color = Color.LightGray)
                                TextButton(modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp), onClick = {
                                    if (!NodusNotificationListener.reconnect(context)) notify(context.getString(R.string.ui_runtime_reconnection_failed))
                                }) { HubText(context.getString(R.string.ui_runtime_reconnect), size = 12) }
                            }
                        }
                        notifications.isEmpty() -> item(key = "status:empty") { HubText(context.getString(R.string.ui_runtime_no_notifications), color = Color(0xFF9B9B9B)) }
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
            title = { HubText(context.getString(R.string.ui_runtime_dismiss_all_notifications), size = 16) },
            text = { HubText(context.getString(R.string.ui_runtime_they_will_also_be_dismissed_in_android)) },
            confirmButton = {
                TextButton(onClick = {
                    val results = NodusNotificationListener.dismissAll(targets)
                    clearTargets = null
                    val failed = results.count { it !in listOf(NotificationActionResult.SUCCESS, NotificationActionResult.MISSING) }
                    if (failed > 0) notify(context.getString(R.string.ui_runtime_some_notifications_changed_or_were_not_dismissed))
                }) { HubText(context.getString(R.string.ui_runtime_dismiss), color = Color(0xFFF800FF)) }
            },
            dismissButton = { TextButton(onClick = { clearTargets = null }) { HubText(context.getString(R.string.ui_runtime_cancel)) } },
            containerColor = Color(0xFF171717)
        )
    }

    if (draftKey != null) {
        val current = notifications.firstOrNull { it.key == draftKey }
        val active = permitted && listener == ListenerState.CONNECTED && current != null &&
            current.ref.revision == draftRevision && current.ref.canReply
        val unavailableReason = when {
            replySent -> context.getString(R.string.ui_runtime_reply_sent_dismissal_did_not_finish_check)
            !permitted || listener != ListenerState.CONNECTED -> context.getString(R.string.ui_runtime_notification_access_unavailable_your_text_has_been)
            current == null -> context.getString(R.string.ui_runtime_the_notification_is_no_longer_active_your)
            current.ref.revision != draftRevision -> context.getString(R.string.ui_runtime_the_notification_was_updated_copy_your_text)
            !current.ref.canReply -> context.getString(R.string.ui_runtime_replying_from_the_hub_is_unavailable_your)
            else -> draftError
        }
        AlertDialog(
            onDismissRequest = { draftKey = null },
            title = { HubText(context.getString(R.string.ui_reply_to, draftTitle), size = 16) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it; draftError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { HubText(context.getString(R.string.ui_runtime_reply)) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = LocalAppStyle.current.family, color = Color.White),
                        minLines = 3,
                        maxLines = 8,
                        readOnly = replySent
                    )
                    if (unavailableReason != null) HubText(unavailableReason, size = 12, color = Color.LightGray)
                    if (draftText.isNotEmpty()) TextButton(onClick = {
                        clipboard.setText(AnnotatedString(draftText))
                    }) { HubText(context.getString(R.string.ui_runtime_copy_text), size = 12) }
                }
            },
            confirmButton = {
                TextButton(enabled = active && !replySent && draftText.isNotBlank(), onClick = {
                    if (current != null) {
                        when (val result = NodusNotificationListener.reply(current.ref, draftText)) {
                            NotificationActionResult.SUCCESS -> { draftKey = null; draftText = "" }
                            NotificationActionResult.SENT_NOT_REMOVED -> replySent = true
                            else -> {
                                draftError = context.getString(R.string.ui_runtime_sending_failed_your_text_has_been_kept)
                                showResult(result)
                            }
                        }
                    }
                }) { HubText(context.getString(R.string.ui_runtime_send), color = if (active && !replySent && draftText.isNotBlank()) Color(0xFFF800FF) else Color.Gray) }
            },
            dismissButton = { TextButton(onClick = { draftKey = null }) { HubText(if (active && !replySent) context.getString(R.string.ui_runtime_cancel) else context.getString(R.string.ui_runtime_close)) } },
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
    val context = LocalContext.current
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
                onClickLabel = context.getString(R.string.ui_runtime_open_in_app),
                onClick = { onOpen(item) },
                onLongClickLabel = context.getString(R.string.ui_runtime_reply_in_hub),
                onLongClick = { onReply(item) }
            )
            .semantics {
                customActions = listOf(CustomAccessibilityAction(context.getString(R.string.ui_runtime_dismiss_notification)) { onDismiss(item); true })
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
            if (truncated && !expanded) TextButton(onClick = { expanded = true }, modifier = Modifier.defaultMinSize(minWidth = 1.dp), contentPadding = PaddingValues(0.dp)) { HubText(context.getString(R.string.ui_runtime_show_all), size = 11) }
        }
        if (!item.ref.dismissible) HubText(context.getString(R.string.ui_runtime_cannot_be_dismissed), size = 11, color = Color.Gray)
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
