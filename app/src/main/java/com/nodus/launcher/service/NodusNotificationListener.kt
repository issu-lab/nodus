package com.nodus.launcher.service

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.security.MessageDigest
import androidx.core.app.NotificationCompat
import com.nodus.launcher.notifications.NotificationActionResult
import com.nodus.launcher.notifications.NotificationActions
import com.nodus.launcher.notifications.NotificationBackend
import com.nodus.launcher.notifications.NotificationRef
import com.nodus.launcher.util.Prefs
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory snapshot only: Android remains the authority for the notification lifetime. */
data class NotificationItem(
    val ref: NotificationRef,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long
) {
    val key: String get() = ref.key
}

enum class ListenerState { DISCONNECTED, CONNECTED, ERROR }

class NodusNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings = UiSettings()

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch { Prefs(this@NodusNotificationListener).settings.collect { config ->
            settings = config
            if (instance === this@NodusNotificationListener) refresh()
        } }
    }

    private fun included(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return when {
            n.category == Notification.CATEGORY_TRANSPORT || n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) -> UiSettings.inHub(settings.music)
            n.category == Notification.CATEGORY_EVENT || n.category == Notification.CATEGORY_REMINDER -> UiSettings.inHub(settings.calendar)
            else -> !sbn.isOngoing
        }
    }

    private val actions = NotificationActions(object : NotificationBackend {
        override fun find(key: String): NotificationRef? = findRaw(key)?.let(::reference)

        override fun open(target: NotificationRef) {
            val raw = requireCurrent(target)
            val pending = raw.notification.contentIntent ?: error("No content intent")
            pending.send()
        }

        override fun reply(target: NotificationRef, text: String) {
            val raw = requireCurrent(target)
            val action = replyAction(raw.notification) ?: error("No text reply action")
            val input = action.remoteInputs.first { it.allowFreeFormInput }
            val results = Bundle().apply { putCharSequence(input.resultKey, text) }
            val intent = Intent()
            RemoteInput.addResultsToIntent(arrayOf(input), intent, results)
            action.actionIntent.send(this@NodusNotificationListener, 0, intent)
        }

        override fun dismiss(target: NotificationRef) {
            val raw = requireCurrent(target)
            check(raw.isClearable) { "Notification is not dismissible" }
            cancelNotification(target.key)
            // Do not remove locally: wait for Android's removal callback.
        }
    })

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (instance === this) refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (instance === this) refresh()
    }

    override fun onListenerDisconnected() {
        disconnect()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun disconnect() {
        if (instance !== this) return
        instance = null
        publish(emptyList())
        _mediaPackages.value = emptySet()
        _listenerState.value = ListenerState.DISCONNECTED
    }

    private fun findRaw(key: String): StatusBarNotification? {
        check(instance === this) { "Listener disconnected" }
        return getActiveNotifications(arrayOf(key))?.firstOrNull { it.key == key }
    }

    private fun requireCurrent(expected: NotificationRef): StatusBarNotification {
        val current = findRaw(expected.key) ?: error("Notification removed")
        check(reference(current).revision == expected.revision) { "Notification changed" }
        return current
    }

    private fun reference(sbn: StatusBarNotification): NotificationRef {
        val notification = sbn.notification
        val body = body(notification)
        // Content, posting time and action identities distinguish replacements under the same key.
        val fingerprint = listOf(
            sbn.postTime, notification.`when`,
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(), body,
            notification.contentIntent?.hashCode(), notification.flags,
            notification.actions?.joinToString { action ->
                "${action.actionIntent.hashCode()}:${action.remoteInputs?.joinToString { it.resultKey }}"
            }
        ).joinToString("\u0000")
        val revision = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return NotificationRef(
            key = sbn.key,
            revision = revision,
            dismissible = sbn.isClearable,
            autoCancel = notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
            canOpen = notification.contentIntent != null,
            canReply = replyAction(notification) != null
        )
    }

    private fun refresh() {
        try {
            val active = activeNotifications ?: emptyArray()
            // Evidence for paused widgets is independent of home/hub notification filters.
            _mediaPackages.value = active.filter {
                val n = it.notification
                n.category == Notification.CATEGORY_TRANSPORT || n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
            }.map { it.packageName }.toSet()
            val items = active.filter(::included)
                .map { sbn ->
                    NotificationItem(
                        ref = reference(sbn),
                        packageName = sbn.packageName,
                        appLabel = try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(sbn.packageName, 0)
                            ).toString()
                        } catch (_: Exception) { sbn.packageName },
                        title = sbn.notification.extras
                            .getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                        text = body(sbn.notification),
                        postTime = sbn.postTime
                    )
                }
                .sortedWith(compareByDescending<NotificationItem> { it.postTime }.thenBy { it.key })
            publish(items)
            _listenerState.value = ListenerState.CONNECTED
        } catch (error: Exception) {
            // Never retain a stale inbox or count when the snapshot is no longer accessible.
            Log.w(TAG, "Cannot read notification snapshot", error)
            _mediaPackages.value = emptySet()
            publish(emptyList())
            _listenerState.value = ListenerState.ERROR
        }
    }

    private fun execute(command: (NotificationActions) -> NotificationActionResult): NotificationActionResult {
        val result = command(actions)
        if (result == NotificationActionResult.FAILED) {
            Log.w(TAG, "Notification command failed")
        }
        refresh()
        return result
    }

    companion object {
        private const val TAG = "NodusNotifications"
        private var instance: NodusNotificationListener? = null
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()
        private val _mediaPackages = MutableStateFlow<Set<String>>(emptySet())
        val mediaPackages: StateFlow<Set<String>> = _mediaPackages.asStateFlow()
        private val _notificationCount = MutableStateFlow(0)
        val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()
        private val _listenerState = MutableStateFlow(ListenerState.DISCONNECTED)
        val listenerState: StateFlow<ListenerState> = _listenerState.asStateFlow()

        private fun publish(items: List<NotificationItem>) {
            _notifications.value = items
            _notificationCount.value = items.size
        }

        private fun replyAction(notification: Notification): Notification.Action? =
            notification.actions?.firstOrNull { action ->
                action.remoteInputs?.any { it.allowFreeFormInput } == true
            }

        private fun body(notification: Notification): String {
            val extras = notification.extras
            val messages = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)?.messages.orEmpty()
            if (messages.isNotEmpty()) {
                return messages.joinToString("\n") { message ->
                    val sender = message.person?.name
                    if (sender.isNullOrBlank()) message.text.toString() else "$sender: ${message.text}"
                }
            }
            return extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.joinToString("\n")?.takeIf { it.isNotBlank() }
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        }

        fun open(target: NotificationRef): NotificationActionResult = instance?.execute {
            it.open(target)
        } ?: NotificationActionResult.DISCONNECTED

        fun dismiss(target: NotificationRef): NotificationActionResult = instance?.execute {
            it.dismiss(target)
        } ?: NotificationActionResult.DISCONNECTED

        fun reply(target: NotificationRef, text: String): NotificationActionResult = instance?.execute {
            it.reply(target, text)
        } ?: NotificationActionResult.DISCONNECTED

        /** Use the confirmed snapshot, never a blanket cancelAllNotifications call. */
        fun dismissAll(targets: List<NotificationRef>): List<NotificationActionResult> =
            targets.filter { it.dismissible }.map(::dismiss)

        fun refreshConnected() { instance?.refresh() }

        fun reconnect(context: Context): Boolean = try {
            requestRebind(ComponentName(context, NodusNotificationListener::class.java))
            true
        } catch (_: Exception) { false }
    }
}
