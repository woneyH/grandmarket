package com.pbl.grandmarket_android.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.parseColor("#00d2ff")
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    
    private val textPaint = Paint().apply {
        color = Color.parseColor("#00d2ff")
        textSize = 50f
        style = Paint.Style.FILL
    }
    
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }

    private var results: List<YoloDetector.Detection> = emptyList()
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f

    fun setResults(results: List<YoloDetector.Detection>, imageWidth: Int, imageHeight: Int) {
        this.results = results
        
        // Calculate scale factors
        this.scaleX = width.toFloat() / imageWidth
        this.scaleY = height.toFloat() / imageHeight
        
        invalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            val left = result.x1 * scaleX
            val top = result.y1 * scaleY
            val right = result.x2 * scaleX
            val bottom = result.y2 * scaleY

            val rect = RectF(left, top, right, bottom)
            
            // Draw Bounding Box
            canvas.drawRect(rect, paint)

            // Draw Label Background
            val label = "${result.className} ${(result.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val bgRect = RectF(left, top - 60f, left + textWidth + 20f, top)
            canvas.drawRect(bgRect, bgPaint)

            // Draw Label Text
            canvas.drawText(label, left + 10f, top - 15f, textPaint)
        }
    }
}
