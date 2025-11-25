package com.example.mangaocr_demon.data.model

import android.graphics.RectF
import kotlinx.serialization.Serializable

/**
 * Represents a text block detected by OCR
 * Coordinates are normalized (0.0 to 1.0) relative to image dimensions
 */
@Serializable
data class TextBlock(
    // Normalized coordinates (0.0 - 1.0)
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,

    // Text content
    val originalText: String,
    val translatedText: String = "",

    // Metadata
    val language: String = "unknown",
    val confidence: Float = 0f
) {
    /**
     * Get pixel bounds from normalized coordinates
     */
    fun getBounds(imageWidth: Int, imageHeight: Int): RectF {
        return RectF(
            left * imageWidth,
            top * imageHeight,
            right * imageWidth,
            bottom * imageHeight
        )
    }

    /**
     * Create a copy with translation
     */
    fun copy(translatedText: String): TextBlock {
        return copy(translatedText = translatedText)
    }
}

/**
 * Container for OCR data with image metadata
 */
@Serializable
data class OcrData(
    val textBlocks: List<TextBlock>,
    val imageWidth: Int,
    val imageHeight: Int,
    val processedAt: Long = System.currentTimeMillis(),
    val dominantLanguage: String = "unknown"
)