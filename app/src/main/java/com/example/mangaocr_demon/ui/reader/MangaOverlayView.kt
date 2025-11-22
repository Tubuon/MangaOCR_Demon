// File: ui/reader/MangaOverlayView.kt
package com.example.mangaocr_demon.ui.reader

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.mangaocr_demon.data.model.TextBlock

class MangaOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textBlocks = listOf<TextBlock>()
    private var imageWidth = 0
    private var imageHeight = 0
    private var showOverlay = true

    private var showTranslation = true

    fun setShowTranslation(show: Boolean) {
        showTranslation = show
        invalidate()
    }

    fun toggleTranslationMode() {
        showTranslation = !showTranslation
        invalidate()
    }




    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        alpha = 220
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val borderPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var debugMode = false
    private var onTextBlockClickListener: ((TextBlock) -> Unit)? = null

    init {
        setWillNotDraw(false)
    }

    fun setTextBlocks(blocks: List<TextBlock>, imgWidth: Int, imgHeight: Int) {
        this.textBlocks = blocks
        this.imageWidth = imgWidth
        this.imageHeight = imgHeight
        invalidate()
    }

    fun toggleOverlay() {
        showOverlay = !showOverlay
        invalidate()
    }

    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        invalidate()
    }

    fun setOnTextBlockClickListener(listener: (TextBlock) -> Unit) {
        onTextBlockClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showOverlay || textBlocks.isEmpty()) return
        if (imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        textBlocks.forEach { block ->
            drawTextBlock(canvas, block, scaleX, scaleY)
        }
    }

    private fun drawTextBlock(canvas: Canvas, block: TextBlock, scaleX: Float, scaleY: Float) {
        val bounds = block.getBounds(imageWidth, imageHeight)
        val scaledBounds = RectF(
            bounds.left * scaleX,
            bounds.top * scaleY,
            bounds.right * scaleX,
            bounds.bottom * scaleY
        )

        // Draw background
        canvas.drawRoundRect(scaledBounds, 8f, 8f, backgroundPaint)

        // Draw border in debug mode
        if (debugMode) {
            canvas.drawRoundRect(scaledBounds, 8f, 8f, borderPaint)
        }

        // ⭐ Choose text to display
        val displayText = if (showTranslation && block.translatedText.isNotEmpty()) {
            block.translatedText
        } else {
            block.originalText
        }

        drawFittedText(canvas, displayText, scaledBounds)
    }

    private fun drawFittedText(canvas: Canvas, text: String, bounds: RectF) {
        val availableWidth = bounds.width() - 16f
        val availableHeight = bounds.height() - 16f

        if (availableWidth <= 0 || availableHeight <= 0) return

        var fontSize = 40f
        textPaint.textSize = fontSize

        // Adjust font size to fit
        val maxIterations = 10
        var iteration = 0

        while (iteration < maxIterations) {
            textPaint.textSize = fontSize
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.fontMetrics.let { it.descent - it.ascent }

            if (textWidth <= availableWidth && textHeight <= availableHeight) {
                break
            }

            fontSize -= 2f
            if (fontSize < 12f) {
                fontSize = 12f
                break
            }
            iteration++
        }

        textPaint.textSize = fontSize

        val x = bounds.left + 8f
        val y = bounds.top + 8f - textPaint.fontMetrics.ascent

        if (textPaint.measureText(text) > availableWidth) {
            drawMultiLineText(canvas, text, bounds, availableWidth)
        } else {
            canvas.drawText(text, x, y, textPaint)
        }
    }

    private fun drawMultiLineText(canvas: Canvas, text: String, bounds: RectF, maxWidth: Float) {
        val words = text.split(" ")
        var currentLine = ""
        var y = bounds.top + 8f - textPaint.fontMetrics.ascent
        val lineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + 4f

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = textPaint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, bounds.left + 8f, y, textPaint)
                currentLine = word
                y += lineHeight
                if (y > bounds.bottom - 8f) break
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty() && y <= bounds.bottom - 8f) {
            canvas.drawText(currentLine, bounds.left + 8f, y, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val clickedBlock = findTextBlockAt(event.x, event.y)
            if (clickedBlock != null) {
                onTextBlockClickListener?.invoke(clickedBlock)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTextBlockAt(x: Float, y: Float): TextBlock? {
        if (imageWidth == 0 || imageHeight == 0) return null

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        return textBlocks.firstOrNull { block ->
            val bounds = block.getBounds(imageWidth, imageHeight)
            val scaledBounds = RectF(
                bounds.left * scaleX,
                bounds.top * scaleY,
                bounds.right * scaleX,
                bounds.bottom * scaleY
            )
            scaledBounds.contains(x, y)
        }
    }
}