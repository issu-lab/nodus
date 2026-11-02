import com.nodus.launcher.home.UpdateNoticePolicy
import com.nodus.launcher.notifications.NotificationSwipePolicy

fun main() {
    var passed = 0
    fun test(name: String, block: () -> Unit) { block(); passed++; println("PASS $name") }
    test("short accidental swipe and high velocity noise do not dismiss") {
        check(!NotificationSwipePolicy.dismiss(5f, 4000f, 320f, 1f, true))
        check(!NotificationSwipePolicy.dismiss(40f, 200f, 320f, 1f, true))
    }
    test("deliberate swipe dismisses in both directions") {
        check(NotificationSwipePolicy.dismiss(90f, 0f, 320f, 1f, true))
        check(NotificationSwipePolicy.dismiss(-90f, 0f, 320f, 1f, true))
    }
    test("fling completes an intentional short swipe") {
        check(NotificationSwipePolicy.dismiss(30f, 1200f, 320f, 1f, true))
        check(NotificationSwipePolicy.dismiss(-30f, -1200f, 320f, 1f, true))
    }
    test("fast reversal returns rather than dismisses") {
        check(!NotificationSwipePolicy.dismiss(120f, -1200f, 320f, 1f, true))
    }
    test("ongoing and unknown size notifications never dismiss") {
        check(!NotificationSwipePolicy.dismiss(300f, 1200f, 320f, 1f, false))
        check(!NotificationSwipePolicy.dismiss(300f, 1200f, 0f, 1f, true))
    }
    test("swipe thresholds follow density and small viewports") {
        for (d in listOf(1f, 2f, 3f)) check(NotificationSwipePolicy.dismiss(90f*d, 0f, 320f*d, d, true))
        check(NotificationSwipePolicy.dismiss(45f, 0f, 120f, 1f, true))
    }
    test("first installation keeps the original welcome only") {
        check(!UpdateNoticePolicy.show(true, 0, 24))
    }
    test("legacy and skipped upgrades show the installed version once") {
        check(UpdateNoticePolicy.show(false, 0, 24))
        check(UpdateNoticePolicy.show(false, 21, 24))
        check(!UpdateNoticePolicy.show(false, 24, 24))
    }
    test("downgrade and invalid version do not repeat update notes") {
        check(!UpdateNoticePolicy.show(false, 25, 24))
        check(!UpdateNoticePolicy.show(false, 0, 0))
    }
    println("$passed swipe/update notice tests passed")
}
