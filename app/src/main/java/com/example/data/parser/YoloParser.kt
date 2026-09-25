package com.example.data.parser

import com.example.data.model.BoundingBox
import com.example.data.model.LabelClass
import java.io.InputStream

object YoloParser {

    /**
     * Exports list of bounding boxes to standard YOLO format:
     * <class_index> <x_center> <y_center> <width> <height>
     */
    fun exportToYolo(boxes: List<BoundingBox>): String {
        return boxes.joinToString(separator = "\n") { it.toYoloFormat() }
    }

    /**
     * Generates classes.txt content for YOLO datasets
     */
    fun exportClassesTxt(classes: List<LabelClass>): String {
        return classes.sortedBy { it.classIndex }.joinToString(separator = "\n") { it.name }
    }

    /**
     * Generates standard YOLO data.yaml configuration
     */
    fun exportDataYaml(
        classes: List<LabelClass>,
        datasetName: String = "dataset"
    ): String {
        val sorted = classes.sortedBy { it.classIndex }
        val namesList = sorted.joinToString(separator = ", ") { "'${it.name}'" }
        return """
            # Ultralytics YOLO Dataset Configuration
            path: ../datasets/$datasetName
            train: images/train
            val: images/val
            test: images/test
            
            nc: ${sorted.size}
            names: [$namesList]
        """.trimIndent()
    }

    /**
     * Parses YOLO format text content into BoundingBoxes
     */
    fun parseYolo(
        content: String,
        classes: List<LabelClass>
    ): List<BoundingBox> {
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line -> BoundingBox.fromYolo(line, classes) }
            .toList()
    }

    fun parseYolo(
        inputStream: InputStream,
        classes: List<LabelClass>
    ): List<BoundingBox> {
        val text = inputStream.bufferedReader().use { it.readText() }
        return parseYolo(text, classes)
    }
}
