package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.model.BoundingBox
import com.example.data.model.LabelClass
import java.util.Locale

@Composable
fun BoxHierarchyList(
    boxes: List<BoundingBox>,
    selectedBoxId: String?,
    classes: List<LabelClass>,
    imageWidth: Int,
    imageHeight: Int,
    onSelectBox: (String) -> Unit,
    onDeleteBox: (String) -> Unit,
    onChangeBoxClass: (String, LabelClass) -> Unit,
    onClearAllBoxes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Annotations (${boxes.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${imageWidth} x ${imageHeight} px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (boxes.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAllBoxes,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (boxes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CropFree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No boxes drawn yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Drag on the canvas to annotate objects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(boxes) { box ->
                        val isSelected = box.id == selectedBoxId
                        val pixelRect = box.toPixelRect(imageWidth, imageHeight)

                        var showClassMenu by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, box.labelClass.color)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBox(box.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(box.labelClass.color)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            box.labelClass.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }

                                    // Class dropdown switch button
                                    Box {
                                        IconButton(
                                            onClick = { showClassMenu = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Label,
                                                contentDescription = "Change Class",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showClassMenu,
                                            onDismissRequest = { showClassMenu = false }
                                        ) {
                                            classes.forEach { cls ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clip(CircleShape)
                                                                    .background(cls.color)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(cls.name)
                                                        }
                                                    },
                                                    onClick = {
                                                        showClassMenu = false
                                                        onChangeBoxClass(box.id, cls)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteBox(box.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete Box",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Coordinate Readout
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "x:[%d, %d] y:[%d, %d] | %dx%d px",
                                        pixelRect.left.toInt(),
                                        pixelRect.right.toInt(),
                                        pixelRect.top.toInt(),
                                        pixelRect.bottom.toInt(),
                                        pixelRect.width.toInt(),
                                        pixelRect.height.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "YOLO: ${box.toYoloFormat()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
