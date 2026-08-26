package com.example.autoclicker

import android.graphics.Bitmap
import android.graphics.Rect
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.mlsdk.text.MLTextAnalyzer

class ImageHelper {

    private var textAnalyzer: MLTextAnalyzer? = null

    init {
        try {
            val setting = MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
                .setLanguage("en")
                .create()
            textAnalyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findTextInBitmap(
        bitmap: Bitmap,
        targetText: String,
        exactMatch: Boolean,
        cropRect: Rect? = null,
        callback: (Boolean, Rect?) -> Unit
    ) {
        val analyzer = textAnalyzer
        if (analyzer == null) {
            callback(false, null)
            return
        }

        try {
            val processBitmap = if (cropRect != null) {
                ImageUtils.cropBitmap(bitmap, cropRect)
            } else {
                bitmap
            }

            val frame = MLFrame.fromBitmap(processBitmap)
            val task = analyzer.asyncAnalyseFrame(frame)

            task.addOnSuccessListener { mlText: MLText? ->
                if (mlText == null || mlText.blocks.isEmpty()) {
                    callback(false, null)
                    return@addOnSuccessListener
                }

                var found = false
                var matchRect: Rect? = null

                val cleanTarget = targetText.trim().lowercase()

                for (block in mlText.blocks) {
                    val blockText = block.stringValue.trim().lowercase()
                    val isMatch = if (exactMatch) {
                        blockText == cleanTarget
                    } else {
                        blockText.contains(cleanTarget)
                    }

                    if (isMatch) {
                        val border = block.border
                        if (border != null) {
                            val offsetX = cropRect?.left ?: 0
                            val offsetY = cropRect?.top ?: 0
                            matchRect = Rect(
                                border.left + offsetX,
                                border.top + offsetY,
                                border.right + offsetX,
                                border.bottom + offsetY
                            )
                        }
                        found = true
                        break
                    }
                }

                callback(found, matchRect)
            }.addOnFailureListener {
                callback(false, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false, null)
        }
    }

    fun release() {
        try {
            textAnalyzer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
