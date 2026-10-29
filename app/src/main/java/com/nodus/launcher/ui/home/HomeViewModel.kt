package com.nodus.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nodus.launcher.data.AppRepository
import com.nodus.launcher.home.HomePolicy
import com.nodus.launcher.model.AppInfo
import com.nodus.launcher.util.*
import com.nodus.launcher.widgets.DeviceWidgets
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class AppItemUi(val app: AppInfo, val dotColor: Int)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val prefs = Prefs(application)
    val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())
    val favorites = prefs.favoriteOrder.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val widgets = DeviceWidgets(application, viewModelScope)
    private val installed = MutableStateFlow<List<AppItemUi>>(emptyList())
    private val counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private var visible = false
    private var day = ""
    private var reloadJob: Job? = null
    private var widgetKey: List<Any>? = null
    val allApps = combine(installed, favorites) { apps, fav ->
        apps.map { it.copy(app = it.app.copy(isFavorite = it.app.packageName in fav)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val homeApps = combine(allApps, favorites, counts, settings) { apps, fav, usage, config ->
        val byId = apps.associateBy { it.app.packageName }
        HomePolicy.select(apps.map { HomePolicy.App(it.app.packageName, it.app.label) }, fav, usage, config.count)
            .mapNotNull(byId::get)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            com.nodus.launcher.service.NodusNotificationListener.listenerState.collect {
                if (visible) widgets.start(settings.value)
            }
        }
        viewModelScope.launch { settings.collect { config ->
            val key = listOf(config.battery, config.signal, config.weather, config.music, config.calendar, config.calendarIds)
            if (visible && key != widgetKey) { widgets.start(config); widgetKey = key }
        } }
    }
    fun resume() {
        visible = true; widgets.start(settings.value)
        widgetKey = listOf(settings.value.battery, settings.value.signal, settings.value.weather,
            settings.value.music, settings.value.calendar, settings.value.calendarIds)
        reload()
    }
    fun pause() { visible = false; widgets.stop() }
    fun tick() {
        widgets.tick()
        if (day != LocalDate.now().toString()) refreshCounts()
    }
    fun reload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppRepository(getApplication()).loadAllApps().distinctBy { it.packageName }.map { app ->
                    AppItemUi(app, ColorExtractor.extractDominantColor(getApplication(), app.icon))
                }
            }
            installed.value = apps
            if (apps.isNotEmpty()) prefs.reconcile(apps.associate { it.app.packageName to it.app.label })
            refreshCounts()
        }
    }
    private fun refreshCounts() {
        day = LocalDate.now().toString()
        viewModelScope.launch { counts.value = withContext(Dispatchers.IO) { RecentApps.dailyCounts(getApplication()) } }
    }
    override fun onCleared() { widgets.stop(); super.onCleared() }
}
