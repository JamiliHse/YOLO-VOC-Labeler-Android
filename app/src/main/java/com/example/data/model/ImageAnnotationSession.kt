package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

data class DatasetImageItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val width: Int = bitmap?.width ?: 1,
    val height: Int = bitmap?.height ?: 1,
    val boundingBoxes: List<BoundingBox> = emptyList(),
    val isSample: Boolean = false
)
