package com.nodus.launcher.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.nodus.launcher.util.Prefs
import com.nodus.launcher.util.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppStyle(val settings: UiSettings = UiSettings(), val family: FontFamily = IBMPlexMono)
val LocalAppStyle = staticCompositionLocalOf { AppStyle() }

@Composable
fun NodusTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember { Prefs(context) }
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    com.nodus.launcher.ui.common.StatusBarVisibility(settings.hideStatusBar)
    val custom by produceState<FontFamily?>(null, settings.font, settings.fontFile) {
        value = null
        if (settings.font == "custom" && settings.fontFile.isNotEmpty()) {
            value = withContext(Dispatchers.IO) {
                try { FontFamily(Typeface.createFromFile(File(context.filesDir, settings.fontFile))) }
                catch (_: Exception) { null }
            }
        }
    }
    val family = when (settings.font) { "system" -> FontFamily.Default; "custom" -> custom ?: IBMPlexMono; else -> IBMPlexMono }
    val base = NodusTypography
    val typography = remember(family) { Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family), displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family), headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family), headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family), titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family), bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family), bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family), labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    ) }
    CompositionLocalProvider(LocalAppStyle provides AppStyle(settings, family)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFF800FF), onPrimary = Color.Black,
            background = Color.Black, onBackground = Color.White, surface = Color(0xFF171717), onSurface = Color.White),
            typography = typography, content = content)
    }
}
