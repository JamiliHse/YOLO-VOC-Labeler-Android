package com.example.data.parser

import android.util.Xml
import androidx.compose.ui.graphics.Color
import com.example.data.model.BoundingBox
import com.example.data.model.LabelClass
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object PascalVocParser {

    /**
     * Serializes bounding boxes into standard PASCAL VOC XML format.
     */
    fun exportToXml(
        folder: String = "JPEGImages",
        filename: String,
        path: String,
        imageWidth: Int,
        imageHeight: Int,
        depth: Int = 3,
        boxes: List<BoundingBox>
    ): String {
        val safeW = imageWidth.coerceAtLeast(1)
        val safeH = imageHeight.coerceAtLeast(1)

        val objectsXml = boxes.joinToString(separator = "\n") { box ->
            val px = box.toPixelRect(safeW, safeH)
            val xmin = px.left.toInt().coerceAtLeast(0)
            val ymin = px.top.toInt().coerceAtLeast(0)
            val xmax = maxOf(xmin + 1, px.right.toInt().coerceAtMost(safeW))
            val ymax = maxOf(ymin + 1, px.bottom.toInt().coerceAtMost(safeH))

            """
    <object>
        <name>${box.labelClass.name}</name>
        <pose>Unspecified</pose>
        <truncated>0</truncated>
        <difficult>0</difficult>
        <bndbox>
            <xmin>$xmin</xmin>
            <ymin>$ymin</ymin>
            <xmax>$xmax</xmax>
            <ymax>$ymax</ymax>
        </bndbox>
    </object>""".trimEnd()
        }

        return """<annotation>
    <folder>$folder</folder>
    <filename>$filename</filename>
    <path>$path</path>
    <source>
        <database>LabelFlow_Dataset</database>
    </source>
    <size>
        <width>$safeW</width>
        <height>$safeH</height>
        <depth>$depth</depth>
    </size>
    <segmented>0</segmented>$objectsXml
</annotation>"""
    }

    /**
     * Parses a PASCAL VOC XML file into image dimensions and a list of BoundingBoxes.
     * Known classes are matched by name; novel classes are automatically generated.
     */
    fun parseVocXml(
        inputStream: InputStream,
        knownClasses: List<LabelClass>
    ): Pair<Pair<Int, Int>, List<BoundingBox>> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTag = ""
        var imgWidth = 0
        var imgHeight = 0

        val loadedBoxes = mutableListOf<BoundingBox>()

        // Object Temporary properties
        var currentName = ""
        var xmin = 0f
        var ymin = 0f
        var xmax = 0f
        var ymax = 0f

        val classColors = listOf(
            Color(0xFFE11D48), Color(0xFF0284C7), Color(0xFF059669),
            Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFFEA580C)
        )

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        when (currentTag.lowercase()) {
                            "width" -> if (imgWidth == 0) imgWidth = text.toIntOrNull() ?: 0
                            "height" -> if (imgHeight == 0) imgHeight = text.toIntOrNull() ?: 0
                            "name" -> currentName = text
                            "xmin" -> xmin = text.toFloatOrNull() ?: 0f
                            "ymin" -> ymin = text.toFloatOrNull() ?: 0f
                            "xmax" -> xmax = text.toFloatOrNull() ?: 0f
                            "ymax" -> ymax = text.toFloatOrNull() ?: 0f
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("object", ignoreCase = true)) {
                        if (currentName.isNotEmpty()) {
                            val cls = knownClasses.find { it.name.equals(currentName, ignoreCase = true) }
                                ?: LabelClass(
                                    name = currentName.lowercase(),
                                    color = classColors[(knownClasses.size + loadedBoxes.size) % classColors.size],
                                    classIndex = knownClasses.size + loadedBoxes.size
                                )

                            val safeW = if (imgWidth > 0) imgWidth.toFloat() else maxOf(xmax, 1f)
                            val safeH = if (imgHeight > 0) imgHeight.toFloat() else maxOf(ymax, 1f)

                            val normLeft = (xmin / safeW).coerceIn(0f, 1f)
                            val normTop = (ymin / safeH).coerceIn(0f, 1f)
                            val normRight = (xmax / safeW).coerceIn(0f, 1f)
                            val normBottom = (ymax / safeH).coerceIn(0f, 1f)

                            val box = BoundingBox.fromNormalized(normLeft, normTop, normRight, normBottom, cls)
                            loadedBoxes.add(box)
                        }
                        currentName = ""
                        xmin = 0f
                        ymin = 0f
                        xmax = 0f
                        ymax = 0f
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (imgWidth <= 0) imgWidth = 800
        if (imgHeight <= 0) imgHeight = 600

        return Pair(Pair(imgWidth, imgHeight), loadedBoxes)
    }
}
