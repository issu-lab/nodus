import com.nodus.launcher.notifications.*

private class FakeBackend(var current: NotificationRef?) : NotificationBackend {
    val calls = mutableListOf<String>()
    var failSend = false
    var failCancel = false
    var failRead = false
    var afterSend: (() -> Unit)? = null
    override fun find(key: String): NotificationRef? {
        if (failRead) error("Disconnected")
        return current?.takeIf { it.key == key }
    }
    override fun open(target: NotificationRef) { calls += "open:${target.key}" }
    override fun reply(target: NotificationRef, text: String) {
        calls += "reply:$text"
        if (failSend) error("PendingIntent canceled")
        afterSend?.invoke()
    }
    override fun dismiss(target: NotificationRef) {
        calls += "dismiss:${target.key}"
        if (failCancel) error("Cancellation rejected")
    }
}

fun main() {
    var passed = 0
    fun test(name: String, body: () -> Unit) { body(); passed++; println("PASS $name") }
    val ref = NotificationRef("conversation", "revision-1", true, canOpen = true, canReply = true)
    test("reply is sent before the original notification is dismissed") {
        val backend = FakeBackend(ref)
        check(NotificationActions(backend).reply(ref, "Hello") == NotificationActionResult.SUCCESS)
        check(backend.calls == listOf("reply:Hello", "dismiss:conversation"))
    }
    test("failed send never dismisses and reports a retryable failure") {
        val backend = FakeBackend(ref).apply { failSend = true }
        check(NotificationActions(backend).reply(ref, "Draft") == NotificationActionResult.FAILED)
        check(backend.calls == listOf("reply:Draft"))
    }
    test("a vanished notification disables all actions") {
        val backend = FakeBackend(null); val actions = NotificationActions(backend)
        check(actions.reply(ref, "Draft") == NotificationActionResult.MISSING)
        check(actions.open(ref) == NotificationActionResult.MISSING)
        check(actions.dismiss(ref) == NotificationActionResult.MISSING)
        check(backend.calls.isEmpty())
    }
    test("replacement under the same key cannot receive a stale reply or deletion") {
        val backend = FakeBackend(ref.copy(revision = "new-message")); val actions = NotificationActions(backend)
        check(actions.reply(ref, "Draft") == NotificationActionResult.CHANGED)
        check(actions.dismiss(ref) == NotificationActionResult.CHANGED)
        check(actions.open(ref) == NotificationActionResult.CHANGED)
        check(backend.calls.isEmpty())
    }
    test("an arriving message after sending is not deleted") {
        val backend = FakeBackend(ref)
        backend.afterSend = { backend.current = ref.copy(revision = "new-message") }
        check(NotificationActions(backend).reply(ref, "Hello") == NotificationActionResult.SENT_NOT_REMOVED)
        check(backend.calls == listOf("reply:Hello"))
    }
    test("app removes notification after reply without a second cancellation") {
        val backend = FakeBackend(ref); backend.afterSend = { backend.current = null }
        check(NotificationActions(backend).reply(ref, "Hello") == NotificationActionResult.SUCCESS)
        check(backend.calls == listOf("reply:Hello"))
    }
    test("cancellation failure after send is not a retryable send failure") {
        val backend = FakeBackend(ref).apply { failCancel = true }
        check(NotificationActions(backend).reply(ref, "Hello") == NotificationActionResult.SENT_NOT_REMOVED)
    }
    test("losing service access after send does not invite duplicate sending") {
        val backend = FakeBackend(ref); backend.afterSend = { backend.failRead = true }
        check(NotificationActions(backend).reply(ref, "Hello") == NotificationActionResult.SENT_NOT_REMOVED)
    }
    test("notifications without content intent never execute a different action") {
        val noOpen = ref.copy(canOpen = false); val backend = FakeBackend(noOpen)
        check(NotificationActions(backend).open(noOpen) == NotificationActionResult.UNSUPPORTED)
        check(backend.calls.isEmpty())
    }
    test("unsupported and blank replies never send") {
        val noReply = ref.copy(canReply = false); val backend = FakeBackend(noReply)
        check(NotificationActions(backend).reply(noReply, "Hello") == NotificationActionResult.UNSUPPORTED)
        check(NotificationActions(backend).reply(noReply, "  ") == NotificationActionResult.EMPTY_REPLY)
        check(backend.calls.isEmpty())
    }
    test("non-dismissible notifications stay in Android") {
        val fixed = ref.copy(dismissible = false); val backend = FakeBackend(fixed)
        check(NotificationActions(backend).dismiss(fixed) == NotificationActionResult.NOT_DISMISSIBLE)
        check(backend.calls.isEmpty())
    }
    test("open honors auto cancel only after opening") {
        val target = ref.copy(autoCancel = true); val backend = FakeBackend(target)
        check(NotificationActions(backend).open(target) == NotificationActionResult.SUCCESS)
        check(backend.calls == listOf("open:conversation", "dismiss:conversation"))
    }
    test("regular open keeps the notification") {
        val backend = FakeBackend(ref)
        check(NotificationActions(backend).open(ref) == NotificationActionResult.SUCCESS)
        check(backend.calls == listOf("open:conversation"))
    }
    test("cancel failure does not turn a successful open into a failure") {
        val target = ref.copy(autoCancel = true); val backend = FakeBackend(target).apply { failCancel = true }
        check(NotificationActions(backend).open(target) == NotificationActionResult.SUCCESS)
    }
    println("$passed notification command tests passed")
}
