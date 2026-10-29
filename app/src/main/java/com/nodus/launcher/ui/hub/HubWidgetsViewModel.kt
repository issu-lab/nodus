package com.nodus.launcher.ui.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nodus.launcher.service.NodusNotificationListener
import com.nodus.launcher.util.Prefs
import com.nodus.launcher.util.UiSettings
import com.nodus.launcher.widgets.DeviceWidgets
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Widget observers belong to the visible hub, independently of notification rows. */
class HubWidgetsViewModel(application: Application) : AndroidViewModel(application) {
    val settings = Prefs(application).settings.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())
    val widgets = DeviceWidgets(application, viewModelScope)
    private var visible = false
    private var key: List<String>? = null
    private fun UiSettings.widgetKey() = listOf(music, calendar, calendarIds)
    init {
        viewModelScope.launch { settings.collect { config ->
            if (visible && key != config.widgetKey()) {
                widgets.start(config, inHub = true); key = config.widgetKey()
            }
        } }
        viewModelScope.launch { NodusNotificationListener.listenerState.collect { refresh() } }
    }
    fun resume() { visible = true; refresh() }
    fun refresh() {
        if (visible) { widgets.start(settings.value, inHub = true); key = settings.value.widgetKey() }
    }
    fun tick() { if (visible) widgets.tick() }
    fun pause() { visible = false; widgets.stop() }
    override fun onCleared() { widgets.stop(); super.onCleared() }
}
