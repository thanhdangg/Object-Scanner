package com.example.objectscanner.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.opencv.core.Point as CvPoint

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = android.graphics.Color.GREEN
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var quadPoints: Array<CvPoint>? = null

    fun setQuadPoints(points: Array<CvPoint>) {
        quadPoints = points
        invalidate() // vẽ lại view khi có điểm mới
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        quadPoints?.let { points ->
            if (points.size == 4) {
                // Vẽ các đường nối giữa 4 điểm tứ giác
                canvas.drawLine(points[0].x.toFloat(), points[0].y.toFloat(), points[1].x.toFloat(), points[1].y.toFloat(), paint)
                canvas.drawLine(points[1].x.toFloat(), points[1].y.toFloat(), points[2].x.toFloat(), points[2].y.toFloat(), paint)
                canvas.drawLine(points[2].x.toFloat(), points[2].y.toFloat(), points[3].x.toFloat(), points[3].y.toFloat(), paint)
                canvas.drawLine(points[3].x.toFloat(), points[3].y.toFloat(), points[0].x.toFloat(), points[0].y.toFloat(), paint)
            }
        }
    }
}
