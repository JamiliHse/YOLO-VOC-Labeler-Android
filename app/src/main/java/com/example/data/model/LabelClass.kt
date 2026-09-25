package com.example.data.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class LabelClass(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Color,
    val classIndex: Int
)

val DefaultClasses = listOf(
    LabelClass(name = "person", color = Color(0xFFE11D48), classIndex = 0),     // Rose
    LabelClass(name = "car", color = Color(0xFF0284C7), classIndex = 1),        // Sky Blue
    LabelClass(name = "bicycle", color = Color(0xFF059669), classIndex = 2),    // Emerald
    LabelClass(name = "motorcycle", color = Color(0xFFD97706), classIndex = 3), // Amber
    LabelClass(name = "dog", color = Color(0xFF7C3AED), classIndex = 4),        // Violet
    LabelClass(name = "traffic_light", color = Color(0xFFEA580C), classIndex = 5), // Orange
    LabelClass(name = "chair", color = Color(0xFF0D9488), classIndex = 6)       // Teal
)

val ClassPaletteColors = listOf(
    Color(0xFFE11D48), // Rose
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF059669), // Emerald
    Color(0xFFD97706), // Amber
    Color(0xFF7C3AED), // Violet
    Color(0xFFEA580C), // Orange
    Color(0xFF0D9488), // Teal
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
    Color(0xFF10B981), // Mint
    Color(0xFFF59E0B), // Golden
    Color(0xFF3B82F6), // Royal Blue
    Color(0xFF6366F1), // Indigo
    Color(0xFF84CC16)  // Lime
)
