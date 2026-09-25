package com.example.data.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.model.BoundingBox
import com.example.data.model.DatasetImageItem
import com.example.data.model.DefaultClasses

object SampleDatasetProvider {

    fun getSampleImages(): List<DatasetImageItem> {
        val personCls = DefaultClasses[0]
        val carCls = DefaultClasses[1]
        val bicycleCls = DefaultClasses[2]
        val dogCls = DefaultClasses[4]
        val trafficLightCls = DefaultClasses[5]

        val trafficBitmap = createTrafficSceneBitmap()
        val trafficBoxes = listOf(
            BoundingBox.fromNormalized(0.24f, 0.48f, 0.52f, 0.74f, carCls),
            BoundingBox.fromNormalized(0.58f, 0.52f, 0.88f, 0.76f, carCls),
            BoundingBox.fromNormalized(0.08f, 0.44f, 0.22f, 0.78f, bicycleCls),
            BoundingBox.fromNormalized(0.42f, 0.16f, 0.52f, 0.36f, trafficLightCls)
        )

        val urbanBitmap = createUrbanStreetBitmap()
        val urbanBoxes = listOf(
            BoundingBox.fromNormalized(0.18f, 0.38f, 0.34f, 0.84f, personCls),
            BoundingBox.fromNormalized(0.38f, 0.42f, 0.52f, 0.82f, personCls),
            BoundingBox.fromNormalized(0.68f, 0.60f, 0.84f, 0.82f, dogCls)
        )

        return listOf(
            DatasetImageItem(
                id = "sample_traffic_001",
                name = "traffic_cam_001.jpg",
                bitmap = trafficBitmap,
                width = trafficBitmap.width,
                height = trafficBitmap.height,
                boundingBoxes = trafficBoxes,
                isSample = true
            ),
            DatasetImageItem(
                id = "sample_urban_002",
                name = "urban_walkway_002.jpg",
                bitmap = urbanBitmap,
                width = urbanBitmap.width,
                height = urbanBitmap.height,
                boundingBoxes = urbanBoxes,
                isSample = true
            )
        )
    }

    private fun createTrafficSceneBitmap(): Bitmap {
        val width = 1000
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Sky gradient
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height * 0.45f,
                AndroidColor.rgb(135, 206, 235),
                AndroidColor.rgb(220, 238, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.45f, skyPaint)

        // Distant mountains / skyline
        val skylinePaint = Paint().apply {
            color = AndroidColor.rgb(90, 110, 130)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(0f, height * 0.45f)
            lineTo(width * 0.15f, height * 0.32f)
            lineTo(width * 0.35f, height * 0.40f)
            lineTo(width * 0.55f, height * 0.28f)
            lineTo(width * 0.75f, height * 0.38f)
            lineTo(width.toFloat(), height * 0.30f)
            lineTo(width.toFloat(), height * 0.45f)
            close()
        }
        canvas.drawPath(path, skylinePaint)

        // Asphalt Road
        val roadPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.45f, 0f, height.toFloat(),
                AndroidColor.rgb(45, 52, 60),
                AndroidColor.rgb(30, 35, 42),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.45f, width.toFloat(), height.toFloat(), roadPaint)

        // Road lane markings
        val lanePaint = Paint().apply {
            color = AndroidColor.rgb(250, 204, 21)
            strokeWidth = 10f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(width * 0.5f, height * 0.45f, width * 0.5f, height.toFloat(), lanePaint)

        val dashedWhitePaint = Paint().apply {
            color = AndroidColor.WHITE
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        for (i in 0..4) {
            val yStart = height * 0.50f + i * 50f
            canvas.drawLine(width * 0.25f, yStart, width * 0.25f, yStart + 30f, dashedWhitePaint)
            canvas.drawLine(width * 0.75f, yStart, width * 0.75f, yStart + 30f, dashedWhitePaint)
        }

        // Draw Car 1 (Blue Sedan)
        val carPaint = Paint().apply {
            color = AndroidColor.rgb(37, 99, 235)
            isAntiAlias = true
        }
        val carRect = RectF(width * 0.24f, height * 0.48f, width * 0.52f, height * 0.74f)
        canvas.drawRoundRect(carRect, 24f, 24f, carPaint)

        // Car 1 Windows
        val glassPaint = Paint().apply {
            color = AndroidColor.rgb(191, 219, 254)
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(carRect.left + 25f, carRect.top + 20f, carRect.right - 25f, carRect.centerY() - 10f),
            12f, 12f, glassPaint
        )

        // Car 1 Wheels
        val tirePaint = Paint().apply {
            color = AndroidColor.rgb(17, 24, 39)
            isAntiAlias = true
        }
        canvas.drawCircle(carRect.left + 50f, carRect.bottom - 10f, 26f, tirePaint)
        canvas.drawCircle(carRect.right - 50f, carRect.bottom - 10f, 26f, tirePaint)

        // Draw Car 2 (Red SUV)
        val car2Paint = Paint().apply {
            color = AndroidColor.rgb(220, 38, 38)
            isAntiAlias = true
        }
        val car2Rect = RectF(width * 0.58f, height * 0.52f, width * 0.88f, height * 0.76f)
        canvas.drawRoundRect(car2Rect, 20f, 20f, car2Paint)
        canvas.drawRoundRect(
            RectF(car2Rect.left + 30f, car2Rect.top + 15f, car2Rect.right - 30f, car2Rect.centerY()),
            10f, 10f, glassPaint
        )
        canvas.drawCircle(car2Rect.left + 55f, car2Rect.bottom - 10f, 28f, tirePaint)
        canvas.drawCircle(car2Rect.right - 55f, car2Rect.bottom - 10f, 28f, tirePaint)

        // Traffic Light on Overhead Pole
        val polePaint = Paint().apply {
            color = AndroidColor.rgb(75, 85, 99)
            strokeWidth = 12f
        }
        canvas.drawLine(width * 0.47f, 0f, width * 0.47f, height * 0.16f, polePaint)
        val tlBox = RectF(width * 0.42f, height * 0.16f, width * 0.52f, height * 0.36f)
        val tlPaint = Paint().apply { color = AndroidColor.rgb(31, 41, 55) }
        canvas.drawRoundRect(tlBox, 16f, 16f, tlPaint)

        // Lights: Red, Yellow, Green
        val redLight = Paint().apply { color = AndroidColor.rgb(239, 68, 68); isAntiAlias = true }
        val yelLight = Paint().apply { color = AndroidColor.rgb(100, 80, 20); isAntiAlias = true }
        val grnLight = Paint().apply { color = AndroidColor.rgb(20, 80, 40); isAntiAlias = true }
        canvas.drawCircle(tlBox.centerX(), tlBox.top + 30f, 18f, redLight)
        canvas.drawCircle(tlBox.centerX(), tlBox.centerY(), 18f, yelLight)
        canvas.drawCircle(tlBox.centerX(), tlBox.bottom - 30f, 18f, grnLight)

        // Bicycle / Cyclist
        val bikePaint = Paint().apply {
            color = AndroidColor.rgb(16, 185, 129)
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val bRect = RectF(width * 0.08f, height * 0.44f, width * 0.22f, height * 0.78f)
        canvas.drawCircle(bRect.left + 35f, bRect.bottom - 35f, 30f, bikePaint)
        canvas.drawCircle(bRect.right - 35f, bRect.bottom - 35f, 30f, bikePaint)
        canvas.drawLine(bRect.left + 35f, bRect.bottom - 35f, bRect.centerX(), bRect.centerY() + 30f, bikePaint)
        canvas.drawLine(bRect.right - 35f, bRect.bottom - 35f, bRect.centerX(), bRect.centerY() + 30f, bikePaint)

        return bitmap
    }

    private fun createUrbanStreetBitmap(): Bitmap {
        val width = 1000
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background buildings
        val wallPaint = Paint().apply { color = AndroidColor.rgb(241, 245, 249) }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.60f, wallPaint)

        // Shop windows
        val windowPaint = Paint().apply { color = AndroidColor.rgb(148, 163, 184) }
        for (i in 0..3) {
            val r = RectF(50f + i * 230f, 100f, 220f + i * 230f, height * 0.50f)
            canvas.drawRect(r, windowPaint)
        }

        // Sidewalk pavement
        val sidewalkPaint = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.60f, 0f, height.toFloat(),
                AndroidColor.rgb(203, 213, 225),
                AndroidColor.rgb(148, 163, 184),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.60f, width.toFloat(), height.toFloat(), sidewalkPaint)

        // Person 1 (walking)
        val person1Paint = Paint().apply { color = AndroidColor.rgb(124, 58, 237); isAntiAlias = true }
        val p1HeadPaint = Paint().apply { color = AndroidColor.rgb(254, 215, 170); isAntiAlias = true }
        val p1 = RectF(width * 0.18f, height * 0.38f, width * 0.34f, height * 0.84f)
        canvas.drawCircle(p1.centerX(), p1.top + 45f, 35f, p1HeadPaint)
        canvas.drawRoundRect(RectF(p1.left + 20f, p1.top + 85f, p1.right - 20f, p1.bottom - 100f), 16f, 16f, person1Paint)
        // Legs
        val legPaint = Paint().apply { color = AndroidColor.rgb(30, 41, 59); strokeWidth = 14f; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(p1.centerX() - 18f, p1.bottom - 100f, p1.left + 30f, p1.bottom - 10f, legPaint)
        canvas.drawLine(p1.centerX() + 18f, p1.bottom - 100f, p1.right - 30f, p1.bottom - 10f, legPaint)

        // Person 2 (standing)
        val person2Paint = Paint().apply { color = AndroidColor.rgb(14, 165, 233); isAntiAlias = true }
        val p2 = RectF(width * 0.38f, height * 0.42f, width * 0.52f, height * 0.82f)
        canvas.drawCircle(p2.centerX(), p2.top + 40f, 32f, p1HeadPaint)
        canvas.drawRoundRect(RectF(p2.left + 15f, p2.top + 80f, p2.right - 15f, p2.bottom - 90f), 14f, 14f, person2Paint)
        canvas.drawLine(p2.centerX() - 15f, p2.bottom - 90f, p2.centerX() - 15f, p2.bottom - 10f, legPaint)
        canvas.drawLine(p2.centerX() + 15f, p2.bottom - 90f, p2.centerX() + 15f, p2.bottom - 10f, legPaint)

        // Dog
        val dogPaint = Paint().apply { color = AndroidColor.rgb(217, 119, 6); isAntiAlias = true }
        val dogRect = RectF(width * 0.68f, height * 0.60f, width * 0.84f, height * 0.82f)
        canvas.drawRoundRect(RectF(dogRect.left + 20f, dogRect.centerY() - 20f, dogRect.right - 20f, dogRect.bottom - 40f), 20f, 20f, dogPaint)
        canvas.drawCircle(dogRect.left + 45f, dogRect.centerY() - 25f, 25f, dogPaint)
        // Dog legs
        val dogLegPaint = Paint().apply { color = AndroidColor.rgb(180, 83, 9); strokeWidth = 8f; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(dogRect.left + 35f, dogRect.bottom - 40f, dogRect.left + 35f, dogRect.bottom - 10f, dogLegPaint)
        canvas.drawLine(dogRect.left + 60f, dogRect.bottom - 40f, dogRect.left + 60f, dogRect.bottom - 10f, dogLegPaint)
        canvas.drawLine(dogRect.right - 60f, dogRect.bottom - 40f, dogRect.right - 60f, dogRect.bottom - 10f, dogLegPaint)
        canvas.drawLine(dogRect.right - 35f, dogRect.bottom - 40f, dogRect.right - 35f, dogRect.bottom - 10f, dogLegPaint)

        return bitmap
    }
}
