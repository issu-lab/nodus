package com.nodus.launcher.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nodus.launcher.ui.theme.LocalAppStyle

val LocalMenuLeadingSpace = staticCompositionLocalOf { false }
val Accent = Color(0xFFF800FF)
@Composable
fun AppText(text: String, modifier: Modifier = Modifier, size: Int = 13, color: Color = Color.White, medium: Boolean = false) {
    Text(if (LocalMenuLeadingSpace.current) "\u00A0$text" else text, modifier, color = color, fontFamily = LocalAppStyle.current.family,
        fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal)
}
