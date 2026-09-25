package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BoundingBox
import com.example.data.model.LabelClass
import com.example.data.parser.PascalVocParser
import com.example.data.parser.YoloParser

enum class ExportFormat(val title: String, val extension: String, val mimeType: String) {
    VOC_XML("PASCAL VOC", "xml", "text/xml"),
    YOLO_TXT("YOLO", "txt", "text/plain"),
    CLASSES_TXT("classes.txt", "txt", "text/plain"),
    DATA_YAML("data.yaml", "yaml", "text/yaml")
}

@Composable
fun ExportPreviewDialog(
    imageName: String,
    imageWidth: Int,
    imageHeight: Int,
    boxes: List<BoundingBox>,
    classes: List<LabelClass>,
    onSaveSaf: (format: ExportFormat, content: String, defaultFilename: String) -> Unit,
    onShareFile: (filename: String, content: String, mimeType: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ExportFormat.VOC_XML) }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    val baseName = imageName.substringBeforeLast(".")

    val vocContent = remember(imageName, imageWidth, imageHeight, boxes) {
        PascalVocParser.exportToXml(
            folder = "JPEGImages",
            filename = imageName,
            path = imageName,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            boxes = boxes
        )
    }

    val yoloContent = remember(boxes) {
        YoloParser.exportToYolo(boxes)
    }

    val classesContent = remember(classes) {
        YoloParser.exportClassesTxt(classes)
    }

    val yamlContent = remember(classes) {
        YoloParser.exportDataYaml(classes, datasetName = "dataset")
    }

    val currentContent = when (selectedTab) {
        ExportFormat.VOC_XML -> vocContent
        ExportFormat.YOLO_TXT -> yoloContent
        ExportFormat.CLASSES_TXT -> classesContent
        ExportFormat.DATA_YAML -> yamlContent
    }

    val defaultFilename = when (selectedTab) {
        ExportFormat.VOC_XML -> "$baseName.xml"
        ExportFormat.YOLO_TXT -> "$baseName.txt"
        ExportFormat.CLASSES_TXT -> "classes.txt"
        ExportFormat.DATA_YAML -> "data.yaml"
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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Export Dataset Annotations",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "$imageName (${boxes.size} bounding boxes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Format Tabs
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExportFormat.entries.forEach { format ->
                        Tab(
                            selected = selectedTab == format,
                            onClick = { selectedTab = format },
                            text = { Text(format.title) }
                        )
                    }
                }

                // Code Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()

                    Text(
                        text = if (currentContent.isEmpty()) "// No annotations present" else currentContent,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                    )
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentContent))
                            showCopiedSnackbar = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }

                    OutlinedButton(
                        onClick = {
                            onShareFile(defaultFilename, currentContent, selectedTab.mimeType)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            onSaveSaf(selectedTab, currentContent, defaultFilename)
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save File")
                    }
                }

                if (showCopiedSnackbar) {
                    Text(
                        "Copied to clipboard!",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
