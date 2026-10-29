package com.nodus.launcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

object ColorExtractor {
    fun extractDominantColor(context: Context, drawable: Drawable): Int {
        return try {
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                drawable.toBitmap(64, 64)
            }
            val palette = Palette.from(bitmap).generate()
            palette.getVibrantColor(
                palette.getMutedColor(
                    palette.getDominantColor(0xFF9B9B9B.toInt())
                )
            )
        } catch (e: Exception) {
            0xFF9B9B9B.toInt()
        }
    }
}
