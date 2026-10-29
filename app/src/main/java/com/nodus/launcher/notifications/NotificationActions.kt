package com.nodus.launcher.notifications

/** An opaque revision prevents commands from acting on a newer notification with the same key. */
data class NotificationRef(
    val key: String,
    val revision: String,
    val dismissible: Boolean,
    val autoCancel: Boolean = false,
    val canOpen: Boolean = false,
    val canReply: Boolean = false
)

enum class NotificationActionResult {
    SUCCESS, SENT_NOT_REMOVED, DISCONNECTED, MISSING, CHANGED,
    UNSUPPORTED, NOT_DISMISSIBLE, EMPTY_REPLY, FAILED
}

interface NotificationBackend {
    fun find(key: String): NotificationRef?
    fun open(target: NotificationRef)
    fun reply(target: NotificationRef, text: String)
    fun dismiss(target: NotificationRef)
}

/** Commands always resolve a fresh system snapshot; sending must succeed before cancellation. */
class NotificationActions(private val backend: NotificationBackend) {
    fun open(expected: NotificationRef): NotificationActionResult = run(expected) { current ->
        if (!current.canOpen) return@run NotificationActionResult.UNSUPPORTED
        backend.open(current)
        if (current.autoCancel && current.dismissible) {
            // An app may synchronously replace its notification in response to the open.
            try {
                val remaining = backend.find(current.key)
                if (remaining?.revision == current.revision) backend.dismiss(remaining)
            } catch (_: Exception) {
                // Opening has already succeeded; cancellation must not turn it into a failed open.
            }
        }
        NotificationActionResult.SUCCESS
    }

    fun dismiss(expected: NotificationRef): NotificationActionResult = run(expected) { current ->
        if (!current.dismissible) return@run NotificationActionResult.NOT_DISMISSIBLE
        backend.dismiss(current)
        NotificationActionResult.SUCCESS
    }

    fun reply(expected: NotificationRef, text: String): NotificationActionResult {
        if (text.isBlank()) return NotificationActionResult.EMPTY_REPLY
        return run(expected) { current ->
            if (!current.canReply) return@run NotificationActionResult.UNSUPPORTED
            backend.reply(current, text)
            // From this point, never report an unsent reply: retrying could send it twice.
            try {
                val remaining = backend.find(current.key)
                when {
                    remaining == null -> NotificationActionResult.SUCCESS
                    remaining.revision != current.revision -> NotificationActionResult.SENT_NOT_REMOVED
                    !remaining.dismissible -> NotificationActionResult.SENT_NOT_REMOVED
                    else -> {
                        backend.dismiss(remaining)
                        NotificationActionResult.SUCCESS
                    }
                }
            } catch (_: Exception) {
                NotificationActionResult.SENT_NOT_REMOVED
            }
        }
    }

    private inline fun run(
        expected: NotificationRef,
        action: (NotificationRef) -> NotificationActionResult
    ): NotificationActionResult = try {
        val current = backend.find(expected.key)
        when {
            current == null -> NotificationActionResult.MISSING
            current.revision != expected.revision -> NotificationActionResult.CHANGED
            else -> action(current)
        }
    } catch (_: Exception) {
        NotificationActionResult.FAILED
    }
}
