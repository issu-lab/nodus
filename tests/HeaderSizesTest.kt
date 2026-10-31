import com.nodus.launcher.home.HeaderSizes

fun main() {
    var count = 0
    fun test(name: String, body: () -> Unit) { body(); count++; println("PASS $name") }
    test("fresh profiles keep their distinct defaults") {
        check(HeaderSizes.read(false, "clock_size") { null } == 42)
        check(HeaderSizes.read(true, "clock_size") { null } == 24)
    }
    test("legacy customized sizes migrate into the active profile") {
        val data = mutableMapOf("clock_size" to 30, "date_size" to 18)
        data.putAll(HeaderSizes.snapshot(true) { data[it] })
        HeaderSizes.keys.forEach { data.remove(it) }
        check(HeaderSizes.read(true, "clock_size") { data[it] } == 30)
        check(HeaderSizes.read(true, "date_size") { data[it] } == 18)
        check(HeaderSizes.read(false, "clock_size") { data[it] } == 42)
    }
    test("repeated switching preserves both customized profiles") {
        val data = mutableMapOf("standard_clock_size" to 54, "compact_clock_size" to 30,
            "standard_signal_size" to 26, "compact_signal_size" to 16)
        repeat(8) { turn -> data.putAll(HeaderSizes.snapshot(turn % 2 == 0) { data[it] }) }
        check(HeaderSizes.read(false, "clock_size") { data[it] } == 54)
        check(HeaderSizes.read(true, "clock_size") { data[it] } == 30)
        check(HeaderSizes.read(false, "signal_size") { data[it] } == 26)
        check(HeaderSizes.read(true, "signal_size") { data[it] } == 16)
    }
    test("profile values take precedence over legacy fields") {
        val data = mapOf("clock_size" to 42, "compact_clock_size" to 32)
        check(HeaderSizes.read(true, "clock_size") { data[it] } == 32)
    }
    test("invalid persisted sizes stay in supported ranges") {
        check(HeaderSizes.read(true, "clock_size") { -500 } == 18)
        check(HeaderSizes.read(false, "signal_size") { 500 } == 32)
    }
    println("$count header profile tests passed")
}
