package com.example.autoclicker

import android.graphics.Bitmap
import android.graphics.Rect

object ImageUtils {
    fun cropBitmap(source: Bitmap, cropRect: Rect): Bitmap {
        val left = Math.max(0, cropRect.left)
        val top = Math.max(0, cropRect.top)
        val right = Math.min(source.width, cropRect.right)
        val bottom = Math.min(source.height, cropRect.bottom)
        val width = Math.max(1, right - left)
        val height = Math.max(1, bottom - top)
        return Bitmap.createBitmap(source, left, top, width, height)
    }
}
