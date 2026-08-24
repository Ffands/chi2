package com.example.autoclicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageUtils {
    fun preprocessForOCR(bitmap: Bitmap): Bitmap {
        var scaled = bitmap
        // Scale proportionally if too small. 100px is a good height for OCR
        if (bitmap.height < 100) {
            val scale = 100f / bitmap.height.toFloat()
            scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), 100, true)
        } else if (bitmap.width < 100) {
            val scale = 100f / bitmap.width.toFloat()
            scaled = Bitmap.createScaledBitmap(bitmap, 100, (bitmap.height * scale).toInt(), true)
        }
        
        val w = scaled.width
        val h = scaled.height
        val bwBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        
        // Calculate average brightness
        var totalBrightness = 0L
        for (color in pixels) {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            totalBrightness += (r + g + b) / 3
        }
        val avgBrightness = (totalBrightness / pixels.size).toInt()
        
        val invert = avgBrightness < 127 // If background is dark, invert colors!
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            var gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            
            if (invert) {
                gray = 255 - gray
            }
            
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        
        bwBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        
        return bwBitmap
    }
}
