package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.example.data.model.BoundingBox
import com.example.data.model.DatasetImageItem
import com.example.data.model.DefaultClasses
import com.example.data.model.LabelClass

data class LabelingUiState(
    val datasetImages: List<DatasetImageItem> = emptyList(),
    val currentImageIndex: Int = 0,
    val currentBitmap: Bitmap? = null,
    val currentImageUri: Uri? = null,
    val currentImageName: String = "image_001.jpg",
    val availableClasses: List<LabelClass> = DefaultClasses,
    val selectedClass: LabelClass = DefaultClasses.first(),
    val boundingBoxes: List<BoundingBox> = emptyList(),
    val selectedBoxId: String? = null,
    val zoom: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val isPanZoomMode: Boolean = false,
    val undoStack: List<List<BoundingBox>> = emptyList(),
    val redoStack: List<List<BoundingBox>> = emptyList(),
    val showClassDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showCropDialog: Boolean = false,
    val showBoxInspectorSheet: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)
