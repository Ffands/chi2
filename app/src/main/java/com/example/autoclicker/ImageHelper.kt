package com.example.autoclicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageHelper {
    fun preprocessForOCR(src: Bitmap): Bitmap {
        // Create grayscale + high contrast
        // Scale x2 (Tesseract prefers larger text, e.g. 30px height)
        val width = src.width * 2
        val height = src.height * 2
        val dest = Bitmap.createScaledBitmap(src, width, height, true)
        
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        
        // Color matrix: grayscale
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        
        // Contrast enhancement
        val scale = 2.0f
        val translate = -0.5f * 255f * (scale - 1f)
        val contrastFilter = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        cm.postConcat(contrastFilter)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(dest, 0f, 0f, paint)
        
        // Count dark vs light pixels to decide if we need to invert
        var lightCount = 0
        var darkCount = 0
        val pixels = IntArray(width * height)
        grayscaleBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (p in pixels) {
            val r = (p shr 16) and 0xff
            if (r > 128) lightCount++ else darkCount++
        }
        
        // Tesseract prefers dark text on a light background.
        // If the background is mostly dark, invert the colors.
        if (darkCount > lightCount) {
            val invertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val invCanvas = Canvas(invertedBitmap)
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(invertMatrix)
            invCanvas.drawBitmap(grayscaleBitmap, 0f, 0f, paint)
            grayscaleBitmap.recycle()
            return invertedBitmap
        }
        
        return grayscaleBitmap
    }
}
