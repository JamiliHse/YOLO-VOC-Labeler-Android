package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.canvas.CanvasFitTransformation
import com.example.ui.canvas.CanvasHitTester
import com.example.ui.canvas.TouchHandle
import kotlin.math.roundToInt

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    STANDARD_4_3("4:3", 4f / 3f),
    WIDE_16_9("16:9", 16f / 9f),
    PHOTO_3_2("3:2", 3f / 2f)
}

@Composable
fun CropToolDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropApplied: (Bitmap) -> Unit
) {
    var workingBitmap by remember { mutableStateOf(bitmap) }
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.FREE) }

    // Normalized crop box [0.0 - 1.0] relative to current workingBitmap
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var activeHandle by remember { mutableStateOf(TouchHandle.NONE) }

    val transform = remember(canvasSize, workingBitmap) {
        CanvasFitTransformation.calculate(
            canvasSize = canvasSize,
            imgWidth = workingBitmap.width,
            imgHeight = workingBitmap.height
        )
    }

    // Function to rotate image by 90 degrees
    fun rotateImage(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true
        )
        workingBitmap = rotated
        cropLeft = 0.05f
        cropTop = 0.05f
        cropRight = 0.95f
        cropBottom = 0.95f
    }

    // Function to flip image
    fun flipImage(horizontal: Boolean) {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        val flipped = Bitmap.createBitmap(
            workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true
        )
        workingBitmap = flipped
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                    Text(
                        "Crop & Adjust Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            val pixelLeft = (cropLeft * workingBitmap.width).roundToInt().coerceIn(0, workingBitmap.width - 1)
                            val pixelTop = (cropTop * workingBitmap.height).roundToInt().coerceIn(0, workingBitmap.height - 1)
                            val pixelWidth = ((cropRight - cropLeft) * workingBitmap.width).roundToInt().coerceIn(1, workingBitmap.width - pixelLeft)
                            val pixelHeight = ((cropBottom - cropTop) * workingBitmap.height).roundToInt().coerceIn(1, workingBitmap.height - pixelTop)

                            val cropped = Bitmap.createBitmap(
                                workingBitmap, pixelLeft, pixelTop, pixelWidth, pixelHeight
                            )
                            onCropApplied(cropped)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Apply")
                    }
                }

                // Interactive Crop Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(workingBitmap, transform, cropLeft, cropTop, cropRight, cropBottom) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val canvasRect = transform.normRectToCanvas(cropLeft, cropTop, cropRight, cropBottom)
                                        val handle = CanvasHitTester.detectHandle(offset, canvasRect, tolerance = 48f)
                                        activeHandle = if (handle != TouchHandle.NONE) {
                                            handle
                                        } else if (canvasRect.contains(offset)) {
                                            TouchHandle.INSIDE
                                        } else {
                                            TouchHandle.NONE
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (activeHandle != TouchHandle.NONE) {
                                            val dNormX = dragAmount.x / transform.displayedImageWidth
                                            val dNormY = dragAmount.y / transform.displayedImageHeight

                                            when (activeHandle) {
                                                TouchHandle.TOP_LEFT -> {
                                                    cropLeft = (cropLeft + dNormX).coerceIn(0f, cropRight - 0.05f)
                                                    cropTop = (cropTop + dNormY).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                TouchHandle.TOP_RIGHT -> {
                                                    cropRight = (cropRight + dNormX).coerceIn(cropLeft + 0.05f, 1f)
                                                    cropTop = (cropTop + dNormY).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                TouchHandle.BOTTOM_LEFT -> {
                                                    cropLeft = (cropLeft + dNormX).coerceIn(0f, cropRight - 0.05f)
                                                    cropBottom = (cropBottom + dNormY).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                TouchHandle.BOTTOM_RIGHT -> {
                                                    cropRight = (cropRight + dNormX).coerceIn(cropLeft + 0.05f, 1f)
                                                    cropBottom = (cropBottom + dNormY).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                TouchHandle.TOP -> {
                                                    cropTop = (cropTop + dNormY).coerceIn(0f, cropBottom - 0.05f)
                                                }
                                                TouchHandle.BOTTOM -> {
                                                    cropBottom = (cropBottom + dNormY).coerceIn(cropTop + 0.05f, 1f)
                                                }
                                                TouchHandle.LEFT -> {
                                                    cropLeft = (cropLeft + dNormX).coerceIn(0f, cropRight - 0.05f)
                                                }
                                                TouchHandle.RIGHT -> {
                                                    cropRight = (cropRight + dNormX).coerceIn(cropLeft + 0.05f, 1f)
                                                }
                                                TouchHandle.INSIDE -> {
                                                    val w = cropRight - cropLeft
                                                    val h = cropBottom - cropTop
                                                    val newL = (cropLeft + dNormX).coerceIn(0f, 1f - w)
                                                    val newT = (cropTop + dNormY).coerceIn(0f, 1f - h)
                                                    cropLeft = newL
                                                    cropRight = newL + w
                                                    cropTop = newT
                                                    cropBottom = newT + h
                                                }
                                                TouchHandle.NONE -> {}
                                            }
                                        }
                                    },
                                    onDragEnd = { activeHandle = TouchHandle.NONE },
                                    onDragCancel = { activeHandle = TouchHandle.NONE }
                                )
                            }
                    ) {
                        canvasSize = size

                        // Draw Image
                        val effectiveW = transform.displayedImageWidth.roundToInt()
                        val effectiveH = transform.displayedImageHeight.roundToInt()
                        val effectiveOffX = transform.offsetX.roundToInt()
                        val effectiveOffY = transform.offsetY.roundToInt()

                        drawImage(
                            image = workingBitmap.asImageBitmap(),
                            srcOffset = IntOffset(0, 0),
                            srcSize = IntSize(workingBitmap.width, workingBitmap.height),
                            dstOffset = IntOffset(effectiveOffX, effectiveOffY),
                            dstSize = IntSize(effectiveW, effectiveH)
                        )

                        val cropRect = transform.normRectToCanvas(cropLeft, cropTop, cropRight, cropBottom)

                        // Darkened semi-transparent overlay outside crop box
                        // Top mask
                        drawRect(Color(0x99000000), topLeft = Offset(0f, 0f), size = Size(size.width, cropRect.top))
                        // Bottom mask
                        drawRect(Color(0x99000000), topLeft = Offset(0f, cropRect.bottom), size = Size(size.width, size.height - cropRect.bottom))
                        // Left mask
                        drawRect(Color(0x99000000), topLeft = Offset(0f, cropRect.top), size = Size(cropRect.left, cropRect.height))
                        // Right mask
                        drawRect(Color(0x99000000), topLeft = Offset(cropRect.right, cropRect.top), size = Size(size.width - cropRect.right, cropRect.height))

                        // Crop Box Outline
                        drawRect(Color.White, topLeft = cropRect.topLeft, size = cropRect.size, style = Stroke(width = 2.5f))

                        // Rule of Thirds 3x3 Grid
                        val oneThirdW = cropRect.width / 3f
                        val oneThirdH = cropRect.height / 3f
                        val gridColor = Color(0x70FFFFFF)
                        drawLine(gridColor, Offset(cropRect.left + oneThirdW, cropRect.top), Offset(cropRect.left + oneThirdW, cropRect.bottom), strokeWidth = 1f)
                        drawLine(gridColor, Offset(cropRect.left + oneThirdW * 2, cropRect.top), Offset(cropRect.left + oneThirdW * 2, cropRect.bottom), strokeWidth = 1f)
                        drawLine(gridColor, Offset(cropRect.left, cropRect.top + oneThirdH), Offset(cropRect.right, cropRect.top + oneThirdH), strokeWidth = 1f)
                        drawLine(gridColor, Offset(cropRect.left, cropRect.top + oneThirdH * 2), Offset(cropRect.right, cropRect.top + oneThirdH * 2), strokeWidth = 1f)

                        // 8 Resize Handles
                        val handlePoints = listOf(
                            cropRect.topLeft, cropRect.topRight, cropRect.bottomLeft, cropRect.bottomRight,
                            Offset(cropRect.center.x, cropRect.top), Offset(cropRect.center.x, cropRect.bottom),
                            Offset(cropRect.left, cropRect.center.y), Offset(cropRect.right, cropRect.center.y)
                        )
                        handlePoints.forEach { pt ->
                            drawCircle(Color.White, radius = 10f, center = pt)
                            drawCircle(Color(0xFF0F172A), radius = 10f, center = pt, style = Stroke(width = 2.5f))
                        }
                    }
                }

                // Bottom Controls: Aspect Ratio & Rotation Tools
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Resolution readout
                    val currentCropW = ((cropRight - cropLeft) * workingBitmap.width).roundToInt()
                    val currentCropH = ((cropBottom - cropTop) * workingBitmap.height).roundToInt()
                    Text(
                        text = "Crop size: $currentCropW x $currentCropH px (Original: ${workingBitmap.width} x ${workingBitmap.height})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Aspect Ratio Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CropAspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = {
                                    selectedRatio = ratio
                                    if (ratio.ratio != null) {
                                        val currentW = cropRight - cropLeft
                                        val desiredH = (currentW * (workingBitmap.width.toFloat() / workingBitmap.height.toFloat())) / ratio.ratio
                                        if (cropTop + desiredH <= 1f) {
                                            cropBottom = cropTop + desiredH
                                        } else {
                                            cropBottom = 1f
                                            cropTop = (1f - desiredH).coerceAtLeast(0f)
                                        }
                                    }
                                },
                                label = { Text(ratio.label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Rotation & Flip Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = { rotateImage(-90f) }) {
                            Icon(Icons.Default.RotateLeft, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Rotate -90°")
                        }
                        OutlinedButton(onClick = { rotateImage(90f) }) {
                            Icon(Icons.Default.RotateRight, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Rotate +90°")
                        }
                        OutlinedButton(onClick = { flipImage(horizontal = true) }) {
                            Icon(Icons.Default.Flip, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Flip Horiz")
                        }
                    }
                }
            }
        }
    }
}
