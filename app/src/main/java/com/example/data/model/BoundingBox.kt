package com.example.data.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import java.util.Locale
import java.util.UUID

data class BoundingBox(
    val id: String = UUID.randomUUID().toString(),
    val labelClass: LabelClass,
    // Normalized coordinates [0.0 - 1.0] relative to original image size
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val isSelected: Boolean = false
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + (width / 2f)
    val centerY: Float get() = top + (height / 2f)

    /**
     * Converts normalized [0.0 - 1.0] coordinates to pixel coordinates on image
     */
    fun toPixelRect(imageWidth: Int, imageHeight: Int): Rect {
        val safeW = imageWidth.coerceAtLeast(1).toFloat()
        val safeH = imageHeight.coerceAtLeast(1).toFloat()
        return Rect(
            left = (left * safeW).coerceIn(0f, safeW),
            top = (top * safeH).coerceIn(0f, safeH),
            right = (right * safeW).coerceIn(0f, safeW),
            bottom = (bottom * safeH).coerceIn(0f, safeH)
        )
    }

    /**
     * Converts to standard YOLO format line:
     * <class_index> <x_center> <y_center> <width> <height>
     * all normalized [0.0, 1.0] with 6 decimal places.
     */
    fun toYoloFormat(): String {
        return String.format(
            Locale.US,
            "%d %.6f %.6f %.6f %.6f",
            labelClass.classIndex,
            centerX.coerceIn(0f, 1f),
            centerY.coerceIn(0f, 1f),
            width.coerceIn(0f, 1f),
            height.coerceIn(0f, 1f)
        )
    }

    companion object {
        fun fromPixelRect(
            rect: Rect,
            imageWidth: Int,
            imageHeight: Int,
            labelClass: LabelClass,
            id: String = UUID.randomUUID().toString()
        ): BoundingBox {
            val safeW = imageWidth.coerceAtLeast(1).toFloat()
            val safeH = imageHeight.coerceAtLeast(1).toFloat()

            val normLeft = (rect.left / safeW).coerceIn(0f, 1f)
            val normTop = (rect.top / safeH).coerceIn(0f, 1f)
            val normRight = (rect.right / safeW).coerceIn(0f, 1f)
            val normBottom = (rect.bottom / safeH).coerceIn(0f, 1f)

            return BoundingBox(
                id = id,
                labelClass = labelClass,
                left = minOf(normLeft, normRight),
                top = minOf(normTop, normBottom),
                right = maxOf(normLeft, normRight),
                bottom = maxOf(normTop, normBottom)
            )
        }

        fun fromNormalized(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            labelClass: LabelClass,
            id: String = UUID.randomUUID().toString()
        ): BoundingBox {
            val l = minOf(left, right).coerceIn(0f, 1f)
            val r = maxOf(left, right).coerceIn(0f, 1f)
            val t = minOf(top, bottom).coerceIn(0f, 1f)
            val b = maxOf(top, bottom).coerceIn(0f, 1f)
            return BoundingBox(
                id = id,
                labelClass = labelClass,
                left = l,
                top = t,
                right = r,
                bottom = b
            )
        }

        fun fromYolo(
            line: String,
            classes: List<LabelClass>
        ): BoundingBox? {
            val tokens = line.trim().split("\\s+".toRegex())
            if (tokens.size < 5) return null
            val classIdx = tokens[0].toIntOrNull() ?: return null
            val xCenter = tokens[1].toFloatOrNull() ?: return null
            val yCenter = tokens[2].toFloatOrNull() ?: return null
            val width = tokens[3].toFloatOrNull() ?: return null
            val height = tokens[4].toFloatOrNull() ?: return null

            val label = classes.find { it.classIndex == classIdx }
                ?: LabelClass(
                    name = "class_$classIdx",
                    color = Color(0xFF38BDF8),
                    classIndex = classIdx
                )

            val left = (xCenter - (width / 2f)).coerceIn(0f, 1f)
            val top = (yCenter - (height / 2f)).coerceIn(0f, 1f)
            val right = (xCenter + (width / 2f)).coerceIn(0f, 1f)
            val bottom = (yCenter + (height / 2f)).coerceIn(0f, 1f)

            return fromNormalized(left, top, right, bottom, label)
        }
    }
}
