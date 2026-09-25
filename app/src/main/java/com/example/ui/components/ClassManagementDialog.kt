package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassPaletteColors
import com.example.data.model.LabelClass

@Composable
fun ClassManagementDialog(
    classes: List<LabelClass>,
    onAddClass: (String, Color) -> Unit,
    onDeleteClass: (LabelClass) -> Unit,
    onDismiss: () -> Unit
) {
    var newClassName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ClassPaletteColors.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manage Dataset Classes",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // New Class Input
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = {
                        newClassName = it
                        errorMessage = null
                    },
                    label = { Text("New Class Name (e.g. helmet, traffic_sign)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("Pick Color Swatch", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ClassPaletteColors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val cleanName = newClassName.trim().lowercase().replace("\\s+".toRegex(), "_")
                        if (cleanName.isEmpty()) {
                            errorMessage = "Name cannot be empty"
                        } else if (classes.any { it.name.equals(cleanName, ignoreCase = true) }) {
                            errorMessage = "Class '$cleanName' already exists"
                        } else {
                            onAddClass(cleanName, selectedColor)
                            newClassName = ""
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Class")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    "Existing Classes (${classes.size})",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(classes) { cls ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(cls.color)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        cls.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "[id: ${cls.classIndex}]",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (classes.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteClass(cls) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Class",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
