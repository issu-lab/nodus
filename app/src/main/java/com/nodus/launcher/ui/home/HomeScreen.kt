package com.nodus.launcher.ui.home

import android.app.Activity
import android.content.*
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.home.WidgetVisibilityPolicy
import com.nodus.launcher.R
import com.nodus.launcher.service.NodusNotificationListener
import com.nodus.launcher.ui.common.*
import com.nodus.launcher.ui.hub.HubActivity
import com.nodus.launcher.ui.settings.SettingsActivity
import com.nodus.launcher.ui.theme.LocalAppStyle
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), homeRequest: Int = 0) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val config by viewModel.settings.collectAsState()
    val apps by viewModel.homeApps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val all by viewModel.allApps.collectAsState()
    val count by NodusNotificationListener.notificationCount.collectAsState()
    val battery by viewModel.widgets.battery.collectAsState()
    val signal by viewModel.widgets.signal.collectAsState()
    val wifiConnected by viewModel.widgets.wifiConnected.collectAsState()
    val events by viewModel.widgets.events.collectAsState()
    val music by viewModel.widgets.music.collectAsState()
    val weather by viewModel.widgets.weather.collectAsState()
    val weatherError by viewModel.widgets.weatherError.collectAsState()
    val snackbar = remember { SnackbarHostState() }; val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val drawer = remember { AppDrawerState(showAll, scope) { showAll = it } }
    val drawerVisible by remember { derivedStateOf { drawer.visible } }
    val drawerList = rememberLazyListState()
    LaunchedEffect(drawerVisible) { if (!drawerVisible) drawerList.scrollToItem(0) }
    var hiddenMusicKey by rememberSaveable { mutableStateOf<String?>(null) }
    var hiddenCalendarDay by rememberSaveable { mutableStateOf<String?>(null) }
    var hiddenCalendarEvents by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
    val eventKeys = events.map { "${it.id}:${it.begin}" }
    val calendarHidden = WidgetVisibilityPolicy.calendarHidden(hiddenCalendarDay, today, hiddenCalendarEvents.toSet(), eventKeys.toSet())
    var favoriteTarget by remember { mutableStateOf<AppItemUi?>(null) }
    fun launchApp(item: AppItemUi) {
        try { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(item.app.packageName, item.app.activityName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { scope.launch { snackbar.showSnackbar("App non disponibile. Aggiorno l’elenco.") }; viewModel.reload() }
    }
    fun openDrawer() {
        drawer.settle(true)
    }
    fun openSettings() {
        context.startActivity(Intent(context, SettingsActivity::class.java))
        (context as? Activity)?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }
    fun openHub() {
        context.startActivity(Intent(context, HubActivity::class.java))
        (context as? Activity)?.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { viewModel.resume(); NodusNotificationListener.refreshConnected() }
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        }
        lifecycle.addObserver(observer)
        val packages = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { viewModel.reload() } }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, packages, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(packages); viewModel.pause() }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = System.currentTimeMillis(); viewModel.tick(); delay(60_000 - System.currentTimeMillis() % 60_000) }
        }
    }
    LaunchedEffect(favorites.size, config.count) {
        if (favorites.size > config.count) {
            if (snackbar.showSnackbar("I preferiti superano il limite attuale. Aggiorna le impostazioni.", "Impostazioni") == SnackbarResult.ActionPerformed) openSettings()
        }
    }
    BackHandler(enabled = drawerVisible) { drawer.settle(false) }
    val canOpenApps by remember { derivedStateOf { drawer.canStart } }
    var observedHomeRequest by remember { mutableIntStateOf(homeRequest) }
    LaunchedEffect(homeRequest) {
        if (homeRequest != observedHomeRequest) {
            drawer.settle(false); observedHomeRequest = homeRequest
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)
        .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.mandatorySystemGestures))
        .clipToBounds()
        .pointerInput(Unit) {
            detectAppDrawerSwipe(edge = 24.dp.toPx(), canOpen = { canOpenApps },
                start = drawer::start, drag = drawer::drag, release = drawer::release, cancel = drawer::cancelDrag)
        }
        .pointerInput(Unit) { var dx = 0f; detectHorizontalDragGestures(
            onDragStart = { dx = 0f }, onHorizontalDrag = { change, amount -> dx += amount; change.consume() },
            onDragEnd = { if (canOpenApps && dx < -80.dp.toPx()) openHub(); dx = 0f }, onDragCancel = { dx = 0f }) }
        .pointerInput(Unit) { detectTapGestures(onLongPress = { if (canOpenApps) openSettings() }) }
        .semantics { customActions = if (drawerVisible) listOf(
            CustomAccessibilityAction("Torna alla home") { drawer.settle(false); true })
            else listOf(CustomAccessibilityAction("Apri impostazioni") { openSettings(); true },
                CustomAccessibilityAction("Apri tutte le app") { openDrawer(); true }) }
    ) {
        val body: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(top = HomePolicy.spacing(config.homeSpacing, 8f).dp, bottom = HomePolicy.spacing(config.homeSpacing, 16f).dp)) {
                HomeHeader(config, now, weather, weatherError, battery, signal, wifiConnected, { openSettings() })
                TextButton(onClick = { openHub() }, modifier = Modifier.offset(x = (-10).dp).background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    AppText(if (count == 0) "Hub notifiche: nessuna notifica" else "Hub notifiche: $count ${if (count == 1) "notifica" else "notifiche"}")
                }
                if (UiSettings.onHome(config.calendar) && events.isNotEmpty() && !calendarHidden) {
                    DismissibleHomeWidget(WidgetVisibilityPolicy.fingerprint(today, *eventKeys.sorted().toTypedArray()), "calendario", onDismiss = {
                        hiddenCalendarDay = today; hiddenCalendarEvents = eventKeys
                    }) { Column {
                    AppText("OGGI", size = 11, color = Color.Gray, modifier = Modifier.padding(top = HomePolicy.spacing(config.homeSpacing, 16f).dp, bottom = HomePolicy.spacing(config.homeSpacing, 4f).dp))
                    events.forEach { event ->
                        Column(Modifier.fillMaxWidth().combinedClickable(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)) }
                            catch (_: Exception) { message("Impossibile aprire il calendario.") }
                        }).padding(vertical = HomePolicy.spacing(config.homeSpacing, 5f).dp)) {
                            AppText(if (event.allDay) "Tutto il giorno" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin)), size = 12, color = Color.LightGray)
                            AppText(event.title, size = 12)
                        }
                    }
                    } }
                }
                if (UiSettings.onHome(config.music)) music?.takeUnless { it.visibilityKey == hiddenMusicKey }?.let { entry ->
                    DismissibleHomeWidget(entry.visibilityKey, "musica", onDismiss = { hiddenMusicKey = entry.visibilityKey }) {
                    MusicWidget(entry, open = {
                        try { if (entry.activity != null) entry.activity.send() else message("Apertura del lettore non disponibile.") }
                        catch (_: Exception) { message("Impossibile aprire il lettore.") }
                    }, control = { command ->
                        if (!viewModel.widgets.control(command)) message("Controllo musicale non disponibile.")
                    })
                    }
                }
                Spacer(Modifier.height(HomePolicy.spacing(config.homeSpacing, 12f).dp))
                apps.forEach { item ->
                    key(item.app.packageName) {
                        AppRow(item, onLongClick = { favoriteTarget = item }, onClick = { launchApp(item) })
                    }
                }
            }
        }
        val bodyModifier = Modifier.fillMaxSize().padding(start = ContentGrid.Outer, end = ContentGrid.End)
        // Full-size home surface owns blank-area touches, so the warm panel below it cannot capture them.
        Box(Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = {}, onLongPress = { if (canOpenApps) openSettings() }) }
            .then(if (drawerVisible) Modifier.clearAndSetSemantics {} else Modifier)) {
            FixedHomeLayout(offset = config.padding.dp, modifier = bodyModifier, content = body)
        }
        if (drawerVisible) {
            // Block taps on the exposed home during transitions; the original drag keeps its root pointer chain.
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = {}) })
        }
        AppDrawer(drawer, drawerVisible, all, drawerList, close = { drawer.settle(false) },
            open = ::launchApp, favorite = { favoriteTarget = it })
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f))
    }
    favoriteTarget?.let { item ->
        AlertDialog(onDismissRequest = { favoriteTarget = null }, title = { AppText(if (item.app.isFavorite) "Rimuovere dai preferiti?" else "Aggiungere ai preferiti?", size = 16) },
            text = { AppText(item.app.label) }, confirmButton = { TextButton(onClick = {
                scope.launch {
                    if (!viewModel.prefs.toggleFavorite(item.app.packageName)) {
                        val result = snackbar.showSnackbar("Hai raggiunto il limite. Aumentalo nelle impostazioni o rimuovi un preferito.", "Impostazioni")
                        if (result == SnackbarResult.ActionPerformed) openSettings()
                    }
                }; favoriteTarget = null
            }) { AppText("Sì", color = Accent) } }, dismissButton = { TextButton(onClick = { favoriteTarget = null }) { AppText("Annulla") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(item: AppItemUi, onLongClick: () -> Unit, onClick: () -> Unit) {
    val style = LocalAppStyle.current; val config = style.settings
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = HomePolicy.spacing(config.homeSpacing, 10f).dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(ContentGrid.IconLane), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.CenterStart) {
                if (config.dots) Canvas(Modifier.size(config.dotSize.dp)) { drawCircle(Color(if (config.dotUniform) config.dotColor else item.dotColor)) }
                else {
                    val bitmap = remember(item.app.icon) { item.app.icon.toBitmap(64, 64).asImageBitmap() }
                    Image(bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
        AppText(item.app.label, size = config.homeText, medium = item.app.isFavorite)
    }
}
