import com.nodus.launcher.search.TerminalQuery as T
import com.nodus.launcher.util.ConfigSchema as C

fun main() {
    var n = 0
    fun test(name: String, body: () -> Unit) { body(); n++; println("PASS $name") }
    fun invalid(values: Map<String, Any>) { check(runCatching { C.validate(values) }.isFailure) }
    test("Italian and English app commands preserve names") {
        check(T.parse("Apri Instagram") == T.Query(T.Kind.OPEN, "Instagram"))
        check(T.parse("open Home Assistant") == T.Query(T.Kind.OPEN, "Home Assistant"))
        check(T.parse("apri    Instagram") == T.Query(T.Kind.OPEN, "Instagram"))
    }
    test("Wi-Fi grammar is exact and bilingual") {
        check(T.parse("spegni il Wi-Fi").kind == T.Kind.WIFI_OFF)
        check(T.parse("turn on wifi").kind == T.Kind.WIFI_ON)
        check(T.parse("come spegni wifi su Android").kind == T.Kind.SEARCH)
    }
    test("camera and device controls are explicit") {
        check(T.parse("apri la fotocamera").kind == T.Kind.CAMERA)
        check(T.parse("modalità aereo").kind == T.Kind.AIRPLANE)
        check(T.parse("bluetooth").kind == T.Kind.BLUETOOTH)
    }
    test("explicit web queries do not launch apps") {
        check(T.parse("cerca sul web Instagram") == T.Query(T.Kind.WEB, "Instagram"))
        check(T.parse("web spegni wifi").kind == T.Kind.WEB)
    }
    test("input is data and never shell code") {
        check(T.parse("ssh root@host; rm -rf /").kind == T.Kind.SEARCH)
        check(T.parse("spegni wifi; apri Instagram").kind == T.Kind.SEARCH)
    }
    test("ranking handles accents case and exactness") {
        check(T.score("Caffè", "caffe") == 3)
        check(T.score("Instagram", "insta") == 2)
        check(T.score("Instagram", "gram") == 1)
        check(T.score("Instagram", "") == 0)
    }
    test("backup accepts distinct header profiles and prompt") {
        C.validate(mapOf("standard_clock_size" to 54, "compact_clock_size" to 24, "terminal_user" to "issu", "terminal_device" to "galaxyS8", "font" to "custom", "favorite_order" to "org.telegram.messenger\ncom.instagram.android"))
    }
    test("private paths and runtime state cannot be imported") {
        invalid(mapOf("font_file" to "../../private"))
        invalid(mapOf("last_seen_version" to 500))
        invalid(mapOf("first_run" to false))
    }
    test("backup rejects wrong types out of range and unknown enums") {
        invalid(mapOf("show_clock" to "true")); invalid(mapOf("settings_text" to 99))
        invalid(mapOf("settings_text" to 16.0)); invalid(mapOf("font" to "download"))
    }
    test("backup rejects overfull and invalid favourites") {
        invalid(mapOf("visible_apps_count" to 1, "favorite_order" to "com.one\ncom.two"))
        invalid(mapOf("favorite_order" to "com.one\ncom.one"))
        invalid(mapOf("favorite_order" to "../../com.one"))
    }
    test("prompt cannot inject lines or commands") {
        check(C.validPrompt("issu") && C.validPrompt("galaxyS8"))
        invalid(mapOf("terminal_user" to "issu\nopen camera"))
        invalid(mapOf("terminal_device" to "x".repeat(41)))
    }
    test("calendar and signed dot colour remain portable") {
        C.validate(mapOf("calendar_ids" to "1,2,3", "dot_color" to 0xFFF800FF.toInt()))
        C.validate(mapOf("calendar_ids" to "*"))
        invalid(mapOf("calendar_ids" to "1,abc"))
    }
    println("$n terminal/configuration tests passed")
}
