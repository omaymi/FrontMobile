package com.example.absencestest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator


class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detections = mutableListOf<FaceDetection>()
    private var previousDetections = mutableListOf<FaceDetection>()
    private var transitionFraction = 0f

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 50f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    data class FaceDetection(
        val id: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    fun updateDetections(newDetections: List<FaceDetection>) {
        previousDetections = detections.toMutableList()
        detections.clear()
        detections.addAll(newDetections)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30 // Très rapide pour un rendu en temps réel
            addUpdateListener {
                transitionFraction = it.animatedValue as Float
                invalidate() // Redessiner immédiatement
            }
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (canvas == null) return // Sécurité

        for (i in detections.indices) {
            val current = detections.getOrNull(i) ?: continue
            val previous = previousDetections.getOrNull(i)

            val left = if (previous != null) {
                previous.left + (current.left - previous.left) * transitionFraction
            } else {
                current.left
            }
            val top = if (previous != null) {
                previous.top + (current.top - previous.top) * transitionFraction
            } else {
                current.top
            }
            val right = if (previous != null) {
                previous.right + (current.right - previous.right) * transitionFraction
            } else {
                current.right
            }
            val bottom = if (previous != null) {
                previous.bottom + (current.bottom - previous.bottom) * transitionFraction
            } else {
                current.bottom
            }

            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText(current.id, left, top - 10f, textPaint)
        }
    }
}