package com.nodus.launcher.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.location.Geocoder
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.provider.CalendarContract
import android.telephony.*
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.home.MusicPresentation
import com.nodus.launcher.home.MusicSessionPolicy
import com.nodus.launcher.home.WidgetVisibilityPolicy
import com.nodus.launcher.service.NodusNotificationListener
import com.nodus.launcher.util.NotificationUtils
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.net.URL
import java.time.*
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.math.roundToInt

fun permitted(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

data class CalendarEntry(val id: Long, val title: String, val begin: Long, val end: Long, val allDay: Boolean)
data class CalendarChoice(val id: String, val name: String)
data class MusicEntry(val title: String, val artist: String, val paused: Boolean, val activity: PendingIntent?,
    val previous: Boolean, val toggle: Boolean, val next: Boolean, val visibilityKey: String)
data class WeatherEntry(val temperature: Int, val code: Int, val city: String, val time: Long) {
    fun stale(now: Long) = now - time > 90 * 60_000L || time > now + 10 * 60_000L
    val description: String get() = when (code) {
        0 -> "Sereno"; 1, 2 -> "Poco nuvoloso"; 3 -> "Nuvoloso"; 45, 48 -> "Nebbia"
        51, 53, 55, 56, 57 -> "Pioviggine"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Pioggia"
        71, 73, 75, 77, 85, 86 -> "Neve"; 95, 96, 99 -> "Temporale"; else -> "Meteo"
    }
}

/** Observers belong to the visible home/hub surface; location is home-only. */
class DeviceWidgets(private val context: Context, private val scope: CoroutineScope) {
    val battery = MutableStateFlow<Int?>(null)
    val signal = MutableStateFlow<Int?>(null)
    val wifiConnected = MutableStateFlow(false)
    val events = MutableStateFlow<List<CalendarEntry>>(emptyList())
    val music = MutableStateFlow<MusicEntry?>(null)
    val weather = MutableStateFlow<WeatherEntry?>(null)
    val weatherError = MutableStateFlow<String?>(null)
    private val handler = Handler(Looper.getMainLooper())
    private var batteryReceiver: BroadcastReceiver? = null
    private var phoneListener: PhoneStateListener? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val wifiNetworks = mutableSetOf<Network>()
    private var calendarObserver: ContentObserver? = null
    private var mediaListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val controllers = mutableListOf<Pair<MediaController, MediaController.Callback>>()
    private var activeController: MediaController? = null
    private var musicIdentity: String? = null
    private var musicPlaying = false
    private var musicVersion = 0L
    private var mediaEvidenceJob: Job? = null
    private var calendarJob: Job? = null
    private var weatherJob: Job? = null
    private var lastAttempt = 0L
    private var settings = UiSettings()
    private var running = false
    private var hubSurface = false
    private fun onSurface(destination: String) = WidgetVisibilityPolicy.onSurface(destination, hubSurface)
    private val weatherCache = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    init {
        try {
            val j = JSONObject(weatherCache.getString("snapshot", "{}")!!)
            weather.value = WeatherEntry(j.getInt("temperature"), j.getInt("code"), j.getString("city"), j.getLong("time"))
        } catch (_: Exception) { }
    }

    @SuppressLint("MissingPermission")
    fun start(config: UiSettings, inHub: Boolean = false) {
        stop(); running = true; settings = config; hubSurface = inHub
        if (config.battery && !hubSurface) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (batteryReceiver === this && intent?.action == Intent.ACTION_BATTERY_CHANGED) updateBattery(intent)
                }
            }
            batteryReceiver = receiver
            val snapshot = ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            updateBattery(snapshot)
        }
        if (config.signal && !hubSurface) {
            observeWifi()
            signal.value = null
            try {
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(value: SignalStrength) { signal.value = value.level }
                }
                (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                    .listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                phoneListener = listener
            } catch (_: Exception) { signal.value = null }
        }
        if (onSurface(config.calendar) && permitted(context, Manifest.permission.READ_CALENDAR)) {
            val observer = object : ContentObserver(handler) { override fun onChange(selfChange: Boolean) { refreshCalendar() } }
            calendarObserver = observer
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            refreshCalendar()
        } else events.value = emptyList()
        if (onSurface(config.music) && NotificationUtils.isNotificationListenerEnabled(context)) {
            try {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val listener = MediaSessionManager.OnActiveSessionsChangedListener { attachControllers(it.orEmpty()) }
                manager.addOnActiveSessionsChangedListener(listener, ComponentName(context, NodusNotificationListener::class.java), handler)
                mediaListener = listener
                attachControllers(manager.getActiveSessions(ComponentName(context, NodusNotificationListener::class.java)))
                mediaEvidenceJob = scope.launch { NodusNotificationListener.mediaPackages.collect { updateMusic() } }
            } catch (_: Exception) { music.value = null }
        } else music.value = null
        if (!hubSurface) refreshWeather()
    }

    fun tick() {
        if (!running) return
        if (settings.battery && !hubSurface) updateBattery()
        if (onSurface(settings.calendar)) refreshCalendar()
        if (onSurface(settings.music)) updateMusic()
        if (settings.weather && !hubSurface) refreshWeather()
    }
    fun stop() {
        running = false
        batteryReceiver?.let { runCatching { context.unregisterReceiver(it) } }; batteryReceiver = null
        phoneListener?.let { runCatching { (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).listen(it, PhoneStateListener.LISTEN_NONE) } }; phoneListener = null
        networkCallback?.let { callback -> runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(callback)
        } }; networkCallback = null
        wifiNetworks.clear(); wifiConnected.value = false
        calendarObserver?.let { context.contentResolver.unregisterContentObserver(it) }; calendarObserver = null
        mediaListener?.let { runCatching { (context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager).removeOnActiveSessionsChangedListener(it) } }; mediaListener = null
        mediaEvidenceJob?.cancel(); mediaEvidenceJob = null
        detachControllers(); calendarJob?.cancel(); weatherJob?.cancel()
    }
    private fun updateBattery(snapshot: Intent? = null) {
        val capacity = runCatching {
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }.getOrNull()
        battery.value = BatteryReading.resolve(
            snapshot?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
            snapshot?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1,
            capacity, battery.value)
    }

    private fun detachControllers() {
        controllers.forEach { (c, callback) -> c.unregisterCallback(callback) }; controllers.clear(); activeController = null
    }
    private fun attachControllers(list: List<MediaController>) {
        if (!running || !onSurface(settings.music)) return
        detachControllers()
        list.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) { updateMusic() }
                override fun onMetadataChanged(metadata: MediaMetadata?) { updateMusic() }
                override fun onSessionDestroyed() {
                    controllers.removeAll { it.first == controller }; updateMusic()
                }
            }
            controller.registerCallback(callback, handler); controllers += controller to callback
        }
        updateMusic()
    }
    private fun updateMusic() {
        if (!running || !onSurface(settings.music)) return
        val evidence = NodusNotificationListener.mediaPackages.value
        val eligible = controllers.map { it.first }.filter { controller ->
            val state = controller.playbackState?.state
            MusicSessionPolicy.visible(state == PlaybackState.STATE_PLAYING, state == PlaybackState.STATE_PAUSED,
                controller.packageName in evidence)
        }
        activeController = eligible.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: eligible.firstOrNull()
        music.value = activeController?.let { c ->
            val metadata = c.metadata
            val text = MusicPresentation.text(
                metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
                metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST))
            val paused = c.playbackState?.state == PlaybackState.STATE_PAUSED
            val actions = c.playbackState?.actions ?: 0L
            val identity = WidgetVisibilityPolicy.fingerprint(c.packageName, c.sessionToken.hashCode().toString(),
                metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID).orEmpty(), text.title, text.artist)
            musicVersion = WidgetVisibilityPolicy.musicVersion(identity, !paused, musicIdentity, musicPlaying, musicVersion)
            musicIdentity = identity; musicPlaying = !paused
            MusicEntry(text.title, text.artist, paused, c.sessionActivity,
                actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
                actions and (PlaybackState.ACTION_PLAY_PAUSE or if (paused) PlaybackState.ACTION_PLAY else PlaybackState.ACTION_PAUSE) != 0L,
                actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L, "$identity:$musicVersion")
        }
        if (activeController == null) { musicIdentity = null; musicPlaying = false }
    }
    private fun observeWifi() {
        wifiNetworks.clear(); wifiConnected.value = false
        try {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (networkCallback !== this) return
                    wifiNetworks.add(network); wifiConnected.value = wifiNetworks.isNotEmpty()
                }
                override fun onLost(network: Network) {
                    if (networkCallback !== this) return
                    wifiNetworks.remove(network); wifiConnected.value = wifiNetworks.isNotEmpty()
                }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (networkCallback !== this) return
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifiNetworks.add(network)
                    else wifiNetworks.remove(network)
                    wifiConnected.value = wifiNetworks.isNotEmpty()
                }
            }
            // All callbacks use the main handler, as do start/stop and UI state.
            networkCallback = callback
            manager.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback, handler)
            manager.allNetworks.filter { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
                .forEach { wifiNetworks.add(it) }
            wifiConnected.value = wifiNetworks.isNotEmpty()
        } catch (_: Exception) {
            networkCallback?.let { runCatching {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it)
            } }
            networkCallback = null; wifiNetworks.clear(); wifiConnected.value = false
        }
    }

    fun control(command: String): Boolean { return try {
        val c = activeController ?: return false
        val allowed = c.playbackState?.actions ?: 0
        val required = when (command) {
            "previous" -> PlaybackState.ACTION_SKIP_TO_PREVIOUS
            "next" -> PlaybackState.ACTION_SKIP_TO_NEXT
            else -> PlaybackState.ACTION_PLAY_PAUSE or if (c.playbackState?.state == PlaybackState.STATE_PLAYING) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
        }
        if (allowed and required == 0L) return false
        when (command) {
            "previous" -> c.transportControls.skipToPrevious()
            "next" -> c.transportControls.skipToNext()
            "toggle" -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
        }; true
    } catch (_: Exception) { false } }

    private fun refreshCalendar() {
        calendarJob?.cancel()
        calendarJob = scope.launch {
            val result = withContext(Dispatchers.IO) { readEvents(context, settings.calendarIds) }
            events.value = result
        }
    }
    private fun refreshWeather() {
        if (!settings.weather) { weatherError.value = null; return }
        if (!permitted(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            weather.value = null; weatherError.value = "Autorizza la posizione"; return
        }
        val now = System.currentTimeMillis()
        if (weatherError.value == "Autorizza la posizione") { lastAttempt = 0; weatherError.value = null }
        val monotonicNow = SystemClock.elapsedRealtime()
        if (weatherJob?.isActive == true || (lastAttempt != 0L && monotonicNow - lastAttempt < 10 * 60_000L)) return
        if (weather.value?.let { now - it.time in 0L until 30 * 60_000L } == true) return
        lastAttempt = monotonicNow
        weatherError.value = null
        weatherJob = scope.launch {
            try {
                val location = withTimeoutOrNull(20_000) { currentLocation() }
                if (location == null) { weatherError.value = "Posizione non disponibile"; return@launch }
                val entry = withContext(Dispatchers.IO) {
                    val lat = String.format(Locale.US, "%.2f", location.latitude)
                    val lon = String.format(Locale.US, "%.2f", location.longitude)
                    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timeformat=unixtime")
                    val conn = url.openConnection() as HttpsURLConnection
                    conn.connectTimeout = 10_000; conn.readTimeout = 10_000
                    val json = try { conn.inputStream.bufferedReader().use { JSONObject(it.readText()) } } finally { conn.disconnect() }
                    val current = json.getJSONObject("current")
                    val city = try {
                        Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea }.orEmpty()
                    } catch (_: Exception) { "" }
                    WeatherEntry(current.getDouble("temperature_2m").roundToInt(), current.getInt("weather_code"), city, current.getLong("time") * 1000)
                }
                weather.value = entry; weatherError.value = null
                weatherCache.edit().putString("snapshot", JSONObject().put("temperature", entry.temperature)
                    .put("code", entry.code).put("city", entry.city).put("time", entry.time).toString()).apply()
            } catch (cancel: CancellationException) { lastAttempt = 0; throw cancel }
            catch (_: Exception) { weatherError.value = "Meteo non aggiornato" }
        }
    }
    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): android.location.Location? = suspendCancellableCoroutine { cont ->
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cancellation = CancellationSignal()
        cont.invokeOnCancellation { cancellation.cancel() }
        try {
            val provider = if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.PASSIVE_PROVIDER
            LocationManagerCompat.getCurrentLocation(manager, provider, cancellation, ContextCompat.getMainExecutor(context)) {
                if (cont.isActive) cont.resume(it)
            }
        } catch (_: Exception) { if (cont.isActive) cont.resume(null) }
    }

    companion object {
        fun calendars(context: Context): List<CalendarChoice> {
            if (!permitted(context, Manifest.permission.READ_CALENDAR)) return emptyList()
            return try {
                context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
                    "${CalendarContract.Calendars.VISIBLE}=1", null, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    ?.use { cursor -> buildList { while (cursor.moveToNext()) add(CalendarChoice(cursor.getLong(0).toString(), cursor.getString(1).orEmpty())) } }.orEmpty()
            } catch (_: Exception) { emptyList() }
        }
        @SuppressLint("MissingPermission")
        fun readEvents(context: Context, selected: String): List<CalendarEntry> {
            if (!permitted(context, Manifest.permission.READ_CALENDAR)) return emptyList()
            val now = System.currentTimeMillis(); val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val begin = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return try {
                val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(uri, begin - 86_400_000); ContentUris.appendId(uri, end + 86_400_000)
                val columns = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.CALENDAR_ID, CalendarContract.Instances.STATUS, CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val ids = selected.split(',').toSet()
                context.contentResolver.query(uri.build(), columns, "${CalendarContract.Instances.VISIBLE}=1", null, CalendarContract.Instances.BEGIN)
                    ?.use { cursor -> buildList {
                        while (cursor.moveToNext()) {
                            val allDay = cursor.getInt(4) == 1; val start = cursor.getLong(2); val finish = cursor.getLong(3)
                            if (selected != "*" && cursor.getLong(5).toString() !in ids) continue
                            if (cursor.getInt(6) == CalendarContract.Events.STATUS_CANCELED || cursor.getInt(7) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue
                            if (HomePolicy.visibleToday(start, finish, allDay, now, zone)) add(CalendarEntry(cursor.getLong(0), cursor.getString(1) ?: "Evento",
                                HomePolicy.eventStart(start, allDay, zone), HomePolicy.eventEnd(finish, allDay, zone), allDay))
                        }
                    } }.orEmpty()
            } catch (_: Exception) { emptyList() }
        }
    }
}
