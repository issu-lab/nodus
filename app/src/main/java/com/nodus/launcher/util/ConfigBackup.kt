package com.nodus.launcher.util

import android.content.Context
import android.graphics.Typeface
import org.json.JSONObject
import java.io.*
import java.util.zip.*

/** A bounded ZIP, never extracted to paths supplied by the archive. */
object ConfigBackup {
    data class Snapshot(val values: Map<String, Any>, val font: ByteArray?)
    private const val FONT_LIMIT = 8 * 1024 * 1024
    fun write(output: OutputStream, snapshot: Snapshot) {
        ConfigSchema.validate(snapshot.values)
        require((snapshot.values["font"] == "custom") == (snapshot.font != null))
        val root = JSONObject().put("format", "nodus-configuration").put("version", 1)
            .put("preferences", JSONObject(snapshot.values))
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("configuration.json")); zip.write(root.toString(2).toByteArray()); zip.closeEntry()
            snapshot.font?.let { require(it.size <= FONT_LIMIT); zip.putNextEntry(ZipEntry("font.bin")); zip.write(it); zip.closeEntry() }
        }
    }
    fun read(input: InputStream): Snapshot {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(entry.name in setOf("configuration.json", "font.bin") && !entry.isDirectory && entry.name !in entries)
                val limit = if (entry.name == "font.bin") FONT_LIMIT else 128 * 1024
                val bytes = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) { val n = zip.read(buffer); if (n < 0) break; require(bytes.size() + n <= limit); bytes.write(buffer, 0, n) }
                entries[entry.name] = bytes.toByteArray(); zip.closeEntry()
            }
        }
        val root = JSONObject(String(entries["configuration.json"] ?: error("configuration"), Charsets.UTF_8))
        require(root.getString("format") == "nodus-configuration" && root.get("version") == 1)
        val prefs = root.getJSONObject("preferences")
        val values = prefs.keys().asSequence().associateWith { key -> prefs.get(key) }
        ConfigSchema.validate(values)
        require((values["font"] == "custom") == entries.containsKey("font.bin"))
        return Snapshot(values, entries["font.bin"])
    }
    fun checkedFont(context: Context, bytes: ByteArray): File {
        require(bytes.size in 4..FONT_LIMIT)
        val header = bytes.copyOfRange(0, 4)
        require(header.contentEquals(byteArrayOf(0, 1, 0, 0)) || String(header, Charsets.US_ASCII) in listOf("OTTO", "true"))
        val directory = File(context.filesDir, "fonts").apply { mkdirs() }
        val file = File.createTempFile("restored-", ".font", directory)
        try { file.writeBytes(bytes); Typeface.createFromFile(file); return file }
        catch (e: Exception) { file.delete(); throw e }
    }
}
