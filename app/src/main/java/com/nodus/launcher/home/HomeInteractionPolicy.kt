package com.nodus.launcher.home

import kotlin.math.abs

/** Geometry is expressed in one unit (pixels in the pointer adapter). */
object HomeInteractionPolicy {
    fun insideGestureArea(x: Float, y: Float, width: Float, height: Float, edge: Float) =
        x >= edge && x < width - edge && y >= edge && y < height - edge

    fun upward(dx: Float, dy: Float, distance: Float) =
        dy <= -distance && -dy > abs(dx) * 1.4f
}

object MusicPresentation {
    data class Text(val title: String, val artist: String)

    fun text(title: String?, artist: String?, displayTitle: String?, displaySubtitle: String?, albumArtist: String?): Text {
        fun String?.clean() = this?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        val name = title.clean().ifBlank { displayTitle.clean() }.ifBlank { "Musica" }
        fun artistOnly(raw: String?): String {
            val value = raw.clean()
            if (value.equals(name, ignoreCase = true)) return ""
            // Some players put "title -- artist" in ARTIST. Strip only an exact
            // leading title followed by a separator, never a substring of a name.
            if (value.startsWith(name, ignoreCase = true)) {
                val rest = value.substring(name.length)
                val separator = Regex("^\\s+(?:--|[-–—·•|])\\s+").find(rest)
                if (separator != null) return rest.substring(separator.range.last + 1).trim()
            }
            return value
        }
        val performer = sequenceOf(artist, albumArtist, displaySubtitle).map(::artistOnly).firstOrNull { it.isNotBlank() }.orEmpty()
        return Text(name, performer)
    }
}

/** Dismissing a home widget never changes the underlying media or calendar. */
object WidgetVisibilityPolicy {
    fun onSurface(destination: String, hub: Boolean): Boolean =
        destination == "both" || destination == if (hub) "hub" else "home"

    fun fingerprint(vararg parts: String): String {
        val bytes = parts.joinToString("") { "${it.length}:$it" }.toByteArray(Charsets.UTF_8)
        return java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
    fun musicVersion(identity: String, playing: Boolean, previousIdentity: String?, previousPlaying: Boolean, version: Long): Long =
        if (identity != previousIdentity || (playing && !previousPlaying)) version + 1 else version

    fun calendarHidden(dismissedDay: String?, today: String, dismissed: Set<String>, current: Set<String>) =
        dismissedDay == today && current.all { it in dismissed }
}

/** Paused media without an active notification is only a resumable/stale session. */
object MusicSessionPolicy {
    fun visible(playing: Boolean, paused: Boolean, hasNotification: Boolean) =
        playing || (paused && hasNotification)
}
