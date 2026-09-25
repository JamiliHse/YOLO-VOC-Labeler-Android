package com.example.ui.canvas

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.model.BoundingBox
import com.example.data.model.LabelClass
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnnotationCanvas(
    bitmap: Bitmap?,
    boxes: List<BoundingBox>,
    activeClass: LabelClass,
    selectedBoxId: String?,
    zoom: Float,
    panOffset: Offset,
    isPanZoomMode: Boolean,
    onBoxCreated: (BoundingBox) -> Unit,
    onBoxUpdated: (BoundingBox) -> Unit,
    onBoxSelected: (String?) -> Unit,
    onTransformChanged: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
    var activeHandle by remember { mutableStateOf(TouchHandle.NONE) }
    var boxUnderEdit by remember { mutableStateOf<BoundingBox?>(null) }

    val transform = remember(canvasSize, bitmap, zoom, panOffset) {
        if (bitmap != null) {
            CanvasFitTransformation.calculate(
                canvasSize = canvasSize,
                imgWidth = bitmap.width,
                imgHeight = bitmap.height,
                zoom = zoom,
                panX = panOffset.x,
                panY = panOffset.y
            )
        } else {
            CanvasFitTransformation(1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f)
        }
    }

    val pointerModifier = if (isPanZoomMode) {
        // Pan & Zoom Gesture Mode
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                val newZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                val newPan = panOffset + pan
                onTransformChanged(newZoom, newPan)
            }
        }
    } else {
        // Annotation Drawing & Editing Mode
        Modifier.pointerInput(bitmap, boxes, selectedBoxId, activeClass, transform) {
            detectDragGestures(
                onDragStart = { offset ->
                    if (bitmap == null) return@detectDragGestures

                    // 1. Check if user tapped a handle on the already selected box
                    val selected = boxes.find { it.id == selectedBoxId }
                    if (selected != null) {
                        val canvasRect = transform.normRectToCanvas(
                            selected.left, selected.top, selected.right, selected.bottom
                        )
                        val handle = CanvasHitTester.detectHandle(offset, canvasRect, tolerance = 42f)
                        if (handle != TouchHandle.NONE) {
                            activeHandle = handle
                            boxUnderEdit = selected
                            return@detectDragGestures
                        }
                    }

                    // 2. Check if tapped inside any existing box (select & prepare to move)
                    val tappedBox = boxes.lastOrNull { box ->
                        val r = transform.normRectToCanvas(box.left, box.top, box.right, box.bottom)
                        r.contains(offset)
                    }

                    if (tappedBox != null) {
                        onBoxSelected(tappedBox.id)
                        boxUnderEdit = tappedBox
                        activeHandle = TouchHandle.INSIDE
                        currentDragStart = offset
                    } else {
                        // 3. New Bounding Box Creation Mode
                        onBoxSelected(null)
                        activeHandle = TouchHandle.NONE
                        currentDragStart = offset
                        currentDragEnd = offset
                        boxUnderEdit = null
                    }
                },
                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                    change.consume()
                    val currentTarget = boxUnderEdit
                    if (currentTarget != null && activeHandle != TouchHandle.NONE) {
                        // Resizing or translating existing box
                        val updated = updateBoxWithHandle(currentTarget, activeHandle, dragAmount, transform)
                        boxUnderEdit = updated
                        onBoxUpdated(updated)
                    } else {
                        // Drawing new bounding box
                        currentDragEnd = change.position
                    }
                },
                onDragEnd = {
                    val start = currentDragStart
                    val end = currentDragEnd
                    if (start != null && end != null && boxUnderEdit == null) {
                        val normStart = transform.canvasToNorm(start)
                        val normEnd = transform.canvasToNorm(end)
                        val left = minOf(normStart.x, normEnd.x)
                        val right = maxOf(normStart.x, normEnd.x)
                        val top = minOf(normStart.y, normEnd.y)
                        val bottom = maxOf(normStart.y, normEnd.y)

                        // Discard accidental micro-clicks
                        if ((right - left) > 0.008f && (bottom - top) > 0.008f) {
                            val newBox = BoundingBox(
                                labelClass = activeClass,
                                left = left,
                                top = top,
                                right = right,
                                bottom = bottom,
                                isSelected = true
                            )
                            onBoxCreated(newBox)
                            onBoxSelected(newBox.id)
                        }
                    }
                    currentDragStart = null
                    currentDragEnd = null
                    boxUnderEdit = null
                    activeHandle = TouchHandle.NONE
                },
                onDragCancel = {
                    currentDragStart = null
                    currentDragEnd = null
                    boxUnderEdit = null
                    activeHandle = TouchHandle.NONE
                }
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(pointerModifier)
    ) {
        canvasSize = size

        // 1. Draw Image
        bitmap?.let { b ->
            val effectiveW = (transform.displayedImageWidth * transform.zoom).roundToInt()
            val effectiveH = (transform.displayedImageHeight * transform.zoom).roundToInt()
            val effectiveOffX = (transform.offsetX + transform.panX).roundToInt()
            val effectiveOffY = (transform.offsetY + transform.panY).roundToInt()

            drawImage(
                image = b.asImageBitmap(),
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(b.width, b.height),
                dstOffset = IntOffset(effectiveOffX, effectiveOffY),
                dstSize = IntSize(effectiveW, effectiveH)
            )
        }

        // 2. Draw Existing Bounding Boxes
        boxes.forEach { box ->
            val canvasRect = transform.normRectToCanvas(box.left, box.top, box.right, box.bottom)
            val isSelected = box.id == selectedBoxId
            val boxColor = box.labelClass.color

            // Box semi-transparent fill
            drawRect(
                color = boxColor.copy(alpha = if (isSelected) 0.22f else 0.12f),
                topLeft = canvasRect.topLeft,
                size = canvasRect.size
            )

            // Box Outline Stroke
            drawRect(
                color = boxColor,
                topLeft = canvasRect.topLeft,
                size = canvasRect.size,
                style = Stroke(width = if (isSelected) 5f else 2.5f)
            )

            // Class Label Tag Header
            drawBoxHeader(canvasRect, box.labelClass.name, boxColor)

            // If selected, draw 8 anchor handles and dimension pill
            if (isSelected) {
                drawResizeHandles(canvasRect)
                bitmap?.let { b ->
                    val pixelW = (box.width * b.width).toInt()
                    val pixelH = (box.height * b.height).toInt()
                    val dimText = "${pixelW}x${pixelH}px (%.1f%%)".format(Locale.US, box.width * box.height * 100f)
                    drawDimensionPill(canvasRect, dimText)
                }
            }
        }

        // 3. Draw In-Progress Drawing Box
        val start = currentDragStart
        val end = currentDragEnd
        if (start != null && end != null && boxUnderEdit == null) {
            val r = Rect(
                minOf(start.x, end.x),
                minOf(start.y, end.y),
                maxOf(start.x, end.x),
                maxOf(start.y, end.y)
            )
            drawRect(
                color = activeClass.color.copy(alpha = 0.2f),
                topLeft = r.topLeft,
                size = r.size
            )
            drawRect(
                color = activeClass.color,
                topLeft = r.topLeft,
                size = r.size,
                style = Stroke(width = 3.5f)
            )
            drawBoxHeader(r, activeClass.name, activeClass.color)
        }
    }
}

private fun updateBoxWithHandle(
    box: BoundingBox,
    handle: TouchHandle,
    delta: Offset,
    transform: CanvasFitTransformation
): BoundingBox {
    val effectiveW = transform.displayedImageWidth * transform.zoom
    val effectiveH = transform.displayedImageHeight * transform.zoom

    if (effectiveW <= 0f || effectiveH <= 0f) return box

    val deltaNormX = delta.x / effectiveW
    val deltaNormY = delta.y / effectiveH

    var l = box.left
    var r = box.right
    var t = box.top
    var b = box.bottom

    when (handle) {
        TouchHandle.TOP_LEFT -> { l += deltaNormX; t += deltaNormY }
        TouchHandle.TOP_RIGHT -> { r += deltaNormX; t += deltaNormY }
        TouchHandle.BOTTOM_LEFT -> { l += deltaNormX; b += deltaNormY }
        TouchHandle.BOTTOM_RIGHT -> { r += deltaNormX; b += deltaNormY }
        TouchHandle.TOP -> { t += deltaNormY }
        TouchHandle.BOTTOM -> { b += deltaNormY }
        TouchHandle.LEFT -> { l += deltaNormX }
        TouchHandle.RIGHT -> { r += deltaNormX }
        TouchHandle.INSIDE -> {
            val w = r - l
            val h = b - t
            val newL = (l + deltaNormX).coerceIn(0f, 1f - w)
            val newT = (t + deltaNormY).coerceIn(0f, 1f - h)
            return box.copy(
                left = newL,
                right = newL + w,
                top = newT,
                bottom = newT + h
            )
        }
        TouchHandle.NONE -> return box
    }

    return box.copy(
        left = minOf(l, r).coerceIn(0f, 1f),
        right = maxOf(l, r).coerceIn(0f, 1f),
        top = minOf(t, b).coerceIn(0f, 1f),
        bottom = maxOf(t, b).coerceIn(0f, 1f)
    )
}

private fun DrawScope.drawBoxHeader(rect: Rect, label: String, color: Color) {
    val textPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = 30f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bgPaint = Paint().apply {
        this.color = color.hashCode()
        isAntiAlias = true
    }
    val textWidth = textPaint.measureText(label)
    val headerHeight = 38f
    val padding = 16f

    val topY = if (rect.top - headerHeight >= 0) rect.top - headerHeight else rect.top
    val headerRect = RectF(
        rect.left,
        topY,
        rect.left + textWidth + (padding * 2),
        topY + headerHeight
    )

    drawContext.canvas.nativeCanvas.drawRoundRect(headerRect, 8f, 8f, bgPaint)
    drawContext.canvas.nativeCanvas.drawText(
        label,
        headerRect.left + padding,
        headerRect.bottom - 10f,
        textPaint
    )
}

private fun DrawScope.drawDimensionPill(rect: Rect, dimText: String) {
    val textPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    val bgPaint = Paint().apply {
        this.color = android.graphics.Color.argb(200, 15, 23, 42)
        isAntiAlias = true
    }
    val textWidth = textPaint.measureText(dimText)
    val pillHeight = 32f
    val pillRect = RectF(
        rect.left,
        rect.bottom + 4f,
        rect.left + textWidth + 18f,
        rect.bottom + 4f + pillHeight
    )
    drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
    drawContext.canvas.nativeCanvas.drawText(
        dimText,
        pillRect.left + 9f,
        pillRect.bottom - 8f,
        textPaint
    )
}

private fun DrawScope.drawResizeHandles(rect: Rect) {
    val radius = 10f
    val handles = listOf(
        rect.topLeft,
        rect.topRight,
        rect.bottomLeft,
        rect.bottomRight,
        Offset(rect.center.x, rect.top),
        Offset(rect.center.x, rect.bottom),
        Offset(rect.left, rect.center.y),
        Offset(rect.right, rect.center.y)
    )

    handles.forEach { pt ->
        // Glow/shadow
        drawCircle(color = Color(0x60000000), radius = radius + 2.5f, center = pt)
        // White interior
        drawCircle(color = Color.White, radius = radius, center = pt)
        // Solid dark blue rim
        drawCircle(color = Color(0xFF0F172A), radius = radius, center = pt, style = Stroke(width = 3f))
    }
}
