import com.nodus.launcher.home.HomeInteractionPolicy as Gesture
import com.nodus.launcher.home.MusicPresentation as Music
import com.nodus.launcher.home.WidgetVisibilityPolicy as Widgets
import com.nodus.launcher.home.MusicSessionPolicy as Session

fun main() {
    var passed = 0
    fun test(name: String, body: () -> Unit) { body(); passed++; println("PASS $name") }
    test("drawer can start in the home interior") {
        check(Gesture.insideGestureArea(150f, 400f, 360f, 780f, 24f))
    }
    test("bottom system gesture strip and off-screen starts never open apps") {
        for (y in listOf(756f, 779f, 800f)) check(!Gesture.insideGestureArea(180f, y, 360f, 780f, 24f))
    }
    test("left right and top system edges are reserved") {
        for ((x,y) in listOf(0f to 400f, 23f to 400f, 336f to 400f, 180f to 23f)) {
            check(!Gesture.insideGestureArea(x, y, 360f, 780f, 24f))
        }
    }
    test("upward swipe opens at different display densities") {
        for (density in listOf(1f, 2f, 3.5f)) check(Gesture.upward(8*density, -90*density, 72*density))
    }
    test("short downward sideways and ambiguous diagonal motion do not open apps") {
        for ((dx,dy) in listOf(0f to -20f, 0f to 90f, -100f to -20f, 100f to -100f)) {
            check(!Gesture.upward(dx,dy,72f))
        }
    }
    test("a drag reversed before release does not meet the opening threshold") {
        check(Gesture.upward(0f,-120f,72f)); check(!Gesture.upward(0f,-30f,72f))
    }
    test("player title repeated in artist is removed for known separators") {
        for (separator in listOf(" -- ", " - ", " — ", " – ", " · ", " • ", " | ")) {
            check(Music.text("Going Under", "Going Under${separator}Evanescence", null, null, null) == Music.Text("Going Under", "Evanescence"))
        }
    }
    test("legitimate artist substrings and internal title occurrences survive") {
        check(Music.text("Air", "Air Supply",null,null,null).artist == "Air Supply")
        check(Music.text("Air", "Band - Air",null,null,null).artist == "Band - Air")
    }
    test("duplicate artist alone falls back to album artist then display subtitle") {
        check(Music.text("Song","Song",null,"Other", "Artist").artist == "Artist")
        check(Music.text("Song","Song",null,"Artist", null).artist == "Artist")
    }
    test("missing title falls back without inventing an artist") {
        check(Music.text(null,null,"Display title",null,null) == Music.Text("Display title", ""))
        check(Music.text(" ",null,null,null,null) == Music.Text("Musica", ""))
    }
    test("blank artist is not replaced by another copy of the title") {
        check(Music.text("Song","",null,"Song",null).artist.isEmpty())
    }
    test("metadata whitespace and title case variations normalize safely") {
        check(Music.text(" Going   Under ","going under -- Evanescence ",null,null,null) == Music.Text("Going Under", "Evanescence"))
    }
    test("dismissed music stays hidden on identical metadata and repeated callbacks") {
        check(Widgets.musicVersion("session:track", true, "session:track", true, 4) == 4L)
    }
    test("pausing a dismissed playing track does not resurrect its widget") {
        check(Widgets.musicVersion("session:track", false, "session:track", true, 4) == 4L)
        check(Widgets.musicVersion("session:track", false, "session:track", false, 4) == 4L)
    }
    test("resuming playback permits the dismissed track to return") {
        check(Widgets.musicVersion("session:track", true, "session:track", false, 4) == 5L)
    }
    test("a different track or new session permits music to return") {
        check(Widgets.musicVersion("new-track", false, "old-track", false, 4) == 5L)
        check(Widgets.musicVersion("new-session", false, null, false, 4) == 5L)
    }
    test("dismissed calendar stays hidden as old events conclude or reorder") {
        check(Widgets.calendarHidden("2026-10-29", "2026-10-29", setOf("a", "b"), setOf("b", "a")))
        check(Widgets.calendarHidden("2026-10-29", "2026-10-29", setOf("a", "b"), setOf("b")))
        check(Widgets.calendarHidden("2026-10-29", "2026-10-29", setOf("a", "b"), emptySet()))
    }
    test("a new event or new day permits the calendar to return") {
        check(!Widgets.calendarHidden("2026-10-29", "2026-10-29", setOf("a"), setOf("a", "b")))
        check(!Widgets.calendarHidden("2026-10-29", "2026-10-30", setOf("a"), setOf("a")))
        check(!Widgets.calendarHidden(null, "2026-10-29", emptySet(), setOf("a")))
    }
    test("widget identity hashes do not store event titles and preserve part boundaries") {
        check(Widgets.fingerprint("ab", "c") != Widgets.fingerprint("a", "bc"))
        check(Widgets.fingerprint("private title").length == 64)
        check(Widgets.fingerprint("a", "b") == Widgets.fingerprint("a", "b"))
    }
    test("home-only widgets never attach observers in the hub") {
        check(Widgets.onSurface("home", false)); check(!Widgets.onSurface("home", true))
    }
    test("hub-only widgets attach observers in the hub rather than home") {
        check(Widgets.onSurface("hub", true)); check(!Widgets.onSurface("hub", false))
    }
    test("both destinations attach the current visible surface") {
        check(Widgets.onSurface("both", true)); check(Widgets.onSurface("both", false))
    }
    test("hidden and unknown destinations stay disabled on every surface") {
        for (value in listOf("none", "", "unknown")) {
            check(!Widgets.onSurface(value, true)); check(!Widgets.onSurface(value, false))
        }
    }
    test("real playback stays visible without depending on a notification") {
        check(Session.visible(true, false, false)); check(Session.visible(true, false, true))
    }
    test("pause remains visible only with a real active media notification") {
        check(Session.visible(false, true, true)); check(!Session.visible(false, true, false))
    }
    test("removing notification from a residual paused session hides the widget") {
        check(Session.visible(false, true, true))
        check(!Session.visible(false, true, false))
        check(Session.visible(true, false, false))
    }
    test("stopped or destroyed sessions never remain visible because of an old notification") {
        check(!Session.visible(false, false, true)); check(!Session.visible(false, false, false))
    }
    println("$passed home interaction/music tests passed")
}
