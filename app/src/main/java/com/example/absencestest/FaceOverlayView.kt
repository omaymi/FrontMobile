package com.example.absencestest

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val detections = mutableListOf<FaceDetection>()
    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 50f
        style = Paint.Style.FILL
    }
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    data class FaceDetection(
        val id: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    fun updateDetections(newDetections: List<FaceDetection>) {
        detections.clear()
        detections.addAll(newDetections)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (detection in detections) {
            // Dessiner le rectangle
            canvas.drawRect(
                detection.left,
                detection.top,
                detection.right,
                detection.bottom,
                boxPaint
            )
            // Dessiner le texte (ID)
            canvas.drawText(
                detection.id,
                detection.left,
                detection.top - 10f,
                textPaint
            )
        }
    }
}