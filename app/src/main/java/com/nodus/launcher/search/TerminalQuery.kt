package com.nodus.launcher.search

import java.text.Normalizer
import java.util.Locale

/** Local grammar only; user input is never passed to a shell or evaluated as code. */
object TerminalQuery {
    enum class Kind { SEARCH, OPEN, WEB, WIFI_ON, WIFI_OFF, WIFI, CAMERA, BLUETOOTH, AIRPLANE, SETTINGS }
    data class Query(val kind: Kind, val text: String)
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    fun parse(value: String): Query {
        val raw = value.trim().take(512)
        val text = normalize(raw)
        val wifi = "wi[- ]?fi"
        if (Regex("(spegni|disattiva|turn off|disable) (il |the )?$wifi").matches(text)) return Query(Kind.WIFI_OFF, "")
        if (Regex("(accendi|attiva|turn on|enable) (il |the )?$wifi").matches(text)) return Query(Kind.WIFI_ON, "")
        if (Regex("(?:(?:apri|open) (?:il |the )?)?$wifi").matches(text)) return Query(Kind.WIFI, "")
        if (text in setOf("apri la fotocamera", "apri fotocamera", "open camera", "open the camera", "camera", "fotocamera")) return Query(Kind.CAMERA, "")
        if (text in setOf("bluetooth", "apri bluetooth", "open bluetooth")) return Query(Kind.BLUETOOTH, "")
        if (text in setOf("modalita aereo", "airplane mode", "flight mode")) return Query(Kind.AIRPLANE, "")
        if (text in setOf("impostazioni", "settings", "apri impostazioni", "open settings", "apri le impostazioni")) return Query(Kind.SETTINGS, "")
        for (prefix in listOf("cerca sul web ", "cerca su internet ", "search the web ", "search web ", "web ")) {
            if (text.startsWith(prefix)) return Query(Kind.WEB, raw.split(Regex("\\s+"), limit = prefix.trim().split(' ').size + 1).last().trim())
        }
        for (prefix in listOf("apri ", "open ")) {
            if (text.startsWith(prefix)) return Query(Kind.OPEN, raw.split(Regex("\\s+"), limit = prefix.trim().split(' ').size + 1).last().trim())
        }
        return Query(Kind.SEARCH, raw)
    }
    fun score(label: String, query: String): Int {
        val labelKey = normalize(label); val queryKey = normalize(query)
        return when { queryKey.isEmpty() -> 0; labelKey == queryKey -> 3; labelKey.startsWith(queryKey) -> 2; labelKey.contains(queryKey) -> 1; else -> 0 }
    }
}
