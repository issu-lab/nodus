import com.nodus.launcher.util.ConfigBackup as B
import java.io.*
import java.util.zip.*

fun main() {
    var n = 0
    fun test(name: String, body: () -> Unit) { body(); n++; println("PASS $name") }
    fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> entries.forEach { (name, bytes) -> z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() } }
        return out.toByteArray()
    }
    fun json(text: String) = "configuration.json" to text.toByteArray()
    fun reject(bytes: ByteArray) { check(runCatching { B.read(ByteArrayInputStream(bytes)) }.isFailure) }
    test("configuration preserves types favourites and inactive header profiles") {
        val values = mapOf<String, Any>("home_text" to 18, "show_clock" to false, "favorite_order" to "com.one\ncom.two", "standard_clock_size" to 52, "compact_clock_size" to 26, "terminal_device" to "galaxyS8", "dot_color" to 0xFFF800FF.toInt())
        val out = ByteArrayOutputStream(); B.write(out, B.Snapshot(values, null))
        val result = B.read(ByteArrayInputStream(out.toByteArray()))
        check(result.values == values && result.font == null)
    }
    test("font bytes round trip without a source path") {
        val font = byteArrayOf(0, 1, 0, 0, 5, 6, 7)
        val out = ByteArrayOutputStream(); B.write(out, B.Snapshot(mapOf("font" to "custom"), font))
        check(B.read(ByteArrayInputStream(out.toByteArray())).font!!.contentEquals(font))
    }
    test("unknown version and foreign format are rejected") {
        reject(zip(json("""{"format":"nodus-configuration","version":2,"preferences":{}}""")))
        reject(zip(json("""{"format":"other","version":1,"preferences":{}}""")))
    }
    test("archive paths are never extracted or accepted") {
        reject(zip("../font.bin" to byteArrayOf(1))); reject(zip("/configuration.json" to byteArrayOf(1)))
    }
    test("invalid JSON and runtime state cannot be restored") {
        reject(zip(json("{")))
        reject(zip(json("""{"format":"nodus-configuration","version":1,"preferences":{"first_run":false}}""")))
    }
    test("custom font cannot disappear or attach to default font") {
        reject(zip(json("""{"format":"nodus-configuration","version":1,"preferences":{"font":"custom"}}""")))
        reject(zip(json("""{"format":"nodus-configuration","version":1,"preferences":{}}"""), "font.bin" to byteArrayOf(1)))
    }
    test("compressed oversized content is bounded after decompression") {
        reject(zip("configuration.json" to ByteArray(128 * 1024 + 1) { 32 }))
        reject(zip("font.bin" to ByteArray(8 * 1024 * 1024 + 1)))
    }
    test("non archive and incomplete input is rejected") {
        reject(byteArrayOf(1, 2, 3)); reject(zip("font.bin" to byteArrayOf(1)))
    }
    println("$n backup archive tests passed")
}
