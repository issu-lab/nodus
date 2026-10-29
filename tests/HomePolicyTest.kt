import com.nodus.launcher.home.HomePolicy
import java.time.*

fun main() {
    var passed = 0
    fun test(name: String, body: () -> Unit) { body(); passed++; println("PASS $name") }
    val apps = listOf(HomePolicy.App("a", "Alpha"), HomePolicy.App("b", "Beta"), HomePolicy.App("c", "Charlie"), HomePolicy.App("d", "Delta"))
    test("favorite order wins over usage without duplicate suggestions") {
        check(HomePolicy.select(apps, listOf("b", "a"), mapOf("b" to 99, "d" to 20), 3) == listOf("b", "a", "d"))
    }
    test("no usage data fills remaining slots alphabetically") {
        check(HomePolicy.select(apps.reversed(), listOf("d"), emptyMap(), 3) == listOf("d", "a", "b"))
    }
    test("usage ties break alphabetically and unknown favorites do not occupy slots") {
        check(HomePolicy.select(apps, listOf("gone"), mapOf("a" to 5, "b" to 5), 2) == listOf("a", "b"))
    }
    test("legacy excess favorites never silently exceed the configured limit") {
        check(HomePolicy.select(apps, listOf("c", "b", "a"), emptyMap(), 2) == listOf("c", "b"))
    }
    test("duplicate launcher activities do not duplicate home rows") {
        check(HomePolicy.select(apps + apps, listOf("a", "a"), emptyMap(), 20) == listOf("a", "b", "c", "d"))
    }
    test("favorite limits block additions and lowering but allow removal") {
        check(HomePolicy.favoriteUpdate(listOf("a", "b"), "c", 2) == null)
        check(HomePolicy.favoriteUpdate(listOf("a", "b"), "a", 2) == listOf("b"))
        check(!HomePolicy.validLimit(1, 2)); check(HomePolicy.validLimit(2, 2))
    }
    test("foreground activity changes inside one app count once") {
        check(HomePolicy.countOpens(listOf("a" to 1, "a" to 2, "a" to 1, "home" to 1, "a" to 1))["a"] == 2)
    }
    test("fixed home preserves the requested position when everything fits") {
        val fit = HomePolicy.fixedPlacement(700f, 400f, 180f)
        check(fit.top == 180f && fit.scale == 1f)
    }
    test("new widgets shift the fixed block up before changing its size") {
        val before = HomePolicy.fixedPlacement(700f, 400f, 180f)
        val after = HomePolicy.fixedPlacement(700f, 650f, 180f)
        check(after.top == 50f && after.top < before.top && after.scale == 1f)
    }
    test("overfull home fits all content without scrolling or clipping") {
        val fit = HomePolicy.fixedPlacement(600f, 900f, 180f)
        check(fit.top == 0f && kotlin.math.abs(fit.scale - 2f/3f) < .0001f)
        check(kotlin.math.abs(900f * fit.scale - 600f) < .001f)
    }
    test("empty and zero-height viewports remain finite and never enlarge content") {
        for ((viewport, content) in listOf(0f to 0f, 600f to 0f, 0f to 800f)) {
            val fit = HomePolicy.fixedPlacement(viewport, content, 200f)
            check(fit.scale.isFinite() && fit.top.isFinite())
            check(fit.scale in 0f..1f && fit.top >= 0f)
        }
    }
    test("small screens large fonts and many events remain within the viewport") {
        for (viewport in listOf(240f, 480f, 700f, 1200f)) {
            for (content in listOf(200f, 650f, 950f, 2400f)) {
                for (offset in listOf(-40f, 0f, 120f, 400f)) {
                    val fit = HomePolicy.fixedPlacement(viewport, content, offset)
                    check(fit.top >= 0f && fit.top + content * fit.scale <= viewport + .001f)
                    check(fit.scale in 0f..1f)
                }
            }
        }
    }
    test("fixed placement behaves consistently across display densities") {
        val logical = HomePolicy.fixedPlacement(600f, 900f, 180f)
        for (density in listOf(1f, 2f, 3.5f)) {
            val physical = HomePolicy.fixedPlacement(600f*density, 900f*density, 180f*density)
            check(kotlin.math.abs(logical.scale - physical.scale) < .0001f)
            check(physical.top == logical.top*density)
        }
    }
    test("default spacing retains the previous app and widget gaps") {
        check(HomePolicy.spacing(20, 10f) == 10f)
        check(HomePolicy.spacing(20, 12f) == 12f)
        check(HomePolicy.spacing(20, 4f) == 4f)
    }
    test("spacing clamps at safe finite bounds and compacts all gap categories") {
        check(HomePolicy.spacing(-10, 10f) == 0f)
        check(HomePolicy.spacing(100, 10f) == 16f)
        for (base in listOf(4f, 5f, 8f, 10f, 12f, 16f)) {
            check(HomePolicy.spacing(10, base) == base / 2)
        }
    }
    test("compact spacing fits more content without reducing the text size") {
        fun height(gap: Int) = 6 * (24f + 2 * HomePolicy.spacing(gap, 10f)) + 210f
        val regular = HomePolicy.fixedPlacement(430f, height(20), 180f)
        val compact = HomePolicy.fixedPlacement(430f, height(0), 180f)
        check(regular.scale < 1f && compact.scale == 1f)
        check(compact.top + height(0) <= 430f)
    }
    for (zoneName in listOf("Europe/Rome", "America/Los_Angeles", "Pacific/Auckland")) {
        test("all-day events stay until local midnight in $zoneName") {
            val zone = ZoneId.of(zoneName); val day = LocalDate.of(2026, 10, 29)
            val begin = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val late = day.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
            check(HomePolicy.visibleToday(begin, end, true, late, zone))
            check(!HomePolicy.visibleToday(begin, end, true, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), zone))
        }
    }
    test("calendar excludes concluded and tomorrow events but retains in-progress events") {
        val zone = ZoneId.of("Europe/Rome"); val now = ZonedDateTime.of(2026,10,29,14,0,0,0,zone).toInstant().toEpochMilli()
        check(!HomePolicy.visibleToday(now-7200000, now, false, now, zone))
        check(HomePolicy.visibleToday(now-3600000, now+3600000, false, now, zone))
        check(!HomePolicy.visibleToday(now+86400000, now+90000000, false, now, zone))
    }
    println("$passed home/calendar policy tests passed")
}
