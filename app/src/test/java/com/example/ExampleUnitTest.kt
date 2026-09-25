package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.model.BoundingBox
import com.example.data.model.DefaultClasses
import com.example.data.model.LabelClass
import com.example.data.parser.YoloParser
import org.junit.Assert.*
import org.junit.Test

class DatasetAnnotationUnitTest {

    @Test
    fun testYoloFormatGenerationAndParsing() {
        val carClass = LabelClass(name = "car", color = Color.Blue, classIndex = 1)
        val box = BoundingBox.fromNormalized(
            left = 0.2f,
            top = 0.3f,
            right = 0.6f,
            bottom = 0.7f,
            labelClass = carClass
        )

        // center should be 0.4, 0.5, width = 0.4, height = 0.4
        assertEquals(0.4f, box.centerX, 0.0001f)
        assertEquals(0.5f, box.centerY, 0.0001f)
        assertEquals(0.4f, box.width, 0.0001f)
        assertEquals(0.4f, box.height, 0.0001f)

        val yoloLine = box.toYoloFormat()
        assertTrue(yoloLine.startsWith("1 "))

        val parsedBox = BoundingBox.fromYolo(yoloLine, listOf(carClass))
        assertNotNull(parsedBox)
        assertEquals(0.2f, parsedBox!!.left, 0.001f)
        assertEquals(0.3f, parsedBox.top, 0.001f)
        assertEquals(0.6f, parsedBox.right, 0.001f)
        assertEquals(0.7f, parsedBox.bottom, 0.001f)
        assertEquals(1, parsedBox.labelClass.classIndex)
    }

    @Test
    fun testCoordinateBoundsClamping() {
        val personClass = DefaultClasses[0]
        val clampedBox = BoundingBox.fromNormalized(
            left = -0.5f,
            top = -0.2f,
            right = 1.5f,
            bottom = 2.0f,
            labelClass = personClass
        )

        assertEquals(0.0f, clampedBox.left, 0.0001f)
        assertEquals(0.0f, clampedBox.top, 0.0001f)
        assertEquals(1.0f, clampedBox.right, 0.0001f)
        assertEquals(1.0f, clampedBox.bottom, 0.0001f)
    }

    @Test
    fun testYoloDataYamlExport() {
        val yaml = YoloParser.exportDataYaml(DefaultClasses, "traffic_ds")
        assertTrue(yaml.contains("nc: ${DefaultClasses.size}"))
        assertTrue(yaml.contains("'person'"))
        assertTrue(yaml.contains("'car'"))
    }
}
