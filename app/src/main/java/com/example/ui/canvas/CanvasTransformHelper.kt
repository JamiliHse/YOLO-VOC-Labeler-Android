package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

data class CanvasFitTransformation(
    val baseScale: Float,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val offsetX: Float,
    val offsetY: Float,
    val displayedImageWidth: Float,
    val displayedImageHeight: Float
) {
    val totalScale: Float get() = baseScale * zoom

    /**
     * Converts a raw viewport touch position (with zoom & pan) to normalized [0.0 - 1.0] image coordinates.
     */
    fun canvasToNorm(point: Offset): Offset {
        val effectiveW = displayedImageWidth * zoom
        val effectiveH = displayedImageHeight * zoom
        val effectiveOffX = offsetX + panX
        val effectiveOffY = offsetY + panY

        if (effectiveW <= 0f || effectiveH <= 0f) return Offset(0f, 0f)

        val normX = (point.x - effectiveOffX) / effectiveW
        val normY = (point.y - effectiveOffY) / effectiveH
        return Offset(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
    }

    /**
     * Converts normalized [0.0 - 1.0] image coordinates to viewport Canvas coordinates.
     */
    fun normToCanvas(normX: Float, normY: Float): Offset {
        val effectiveW = displayedImageWidth * zoom
        val effectiveH = displayedImageHeight * zoom
        val effectiveOffX = offsetX + panX
        val effectiveOffY = offsetY + panY

        return Offset(
            x = effectiveOffX + (normX.coerceIn(0f, 1f) * effectiveW),
            y = effectiveOffY + (normY.coerceIn(0f, 1f) * effectiveH)
        )
    }

    /**
     * Converts a normalized box to viewport screen Rect.
     */
    fun normRectToCanvas(left: Float, top: Float, right: Float, bottom: Float): Rect {
        val tl = normToCanvas(left, top)
        val br = normToCanvas(right, bottom)
        return Rect(tl.x, tl.y, br.x, br.y)
    }

    companion object {
        fun calculate(
            canvasSize: Size,
            imgWidth: Int,
            imgHeight: Int,
            zoom: Float = 1f,
            panX: Float = 0f,
            panY: Float = 0f
        ): CanvasFitTransformation {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f || imgWidth <= 0 || imgHeight <= 0) {
                return CanvasFitTransformation(
                    baseScale = 1f,
                    zoom = zoom,
                    panX = panX,
                    panY = panY,
                    offsetX = 0f,
                    offsetY = 0f,
                    displayedImageWidth = 1f,
                    displayedImageHeight = 1f
                )
            }

            val scaleX = canvasSize.width / imgWidth.toFloat()
            val scaleY = canvasSize.height / imgHeight.toFloat()
            val scale = minOf(scaleX, scaleY)

            val displayedW = imgWidth * scale
            val displayedH = imgHeight * scale
            val offX = (canvasSize.width - displayedW) / 2f
            val offY = (canvasSize.height - displayedH) / 2f

            return CanvasFitTransformation(
                baseScale = scale,
                zoom = zoom,
                panX = panX,
                panY = panY,
                offsetX = offX,
                offsetY = offY,
                displayedImageWidth = displayedW,
                displayedImageHeight = displayedH
            )
        }
    }
}

object CanvasHitTester {

    fun detectHandle(point: Offset, rect: Rect, tolerance: Float = 36f): TouchHandle {
        val corners = mapOf(
            TouchHandle.TOP_LEFT to rect.topLeft,
            TouchHandle.TOP_RIGHT to rect.topRight,
            TouchHandle.BOTTOM_LEFT to rect.bottomLeft,
            TouchHandle.BOTTOM_RIGHT to rect.bottomRight,
            TouchHandle.TOP to Offset(rect.center.x, rect.top),
            TouchHandle.BOTTOM to Offset(rect.center.x, rect.bottom),
            TouchHandle.LEFT to Offset(rect.left, rect.center.y),
            TouchHandle.RIGHT to Offset(rect.right, rect.center.y)
        )

        for ((handle, pos) in corners) {
            if ((point - pos).getDistance() <= tolerance) {
                return handle
            }
        }
        return TouchHandle.NONE
    }
}
