package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BoundingBox
import com.example.data.model.DatasetImageItem
import com.example.data.model.LabelClass
import com.example.data.parser.PascalVocParser
import com.example.data.parser.YoloParser
import com.example.data.sample.SampleDatasetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class LabelingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LabelingUiState())
    val uiState: StateFlow<LabelingUiState> = _uiState.asStateFlow()

    init {
        loadInitialSampleDataset()
    }

    private fun loadInitialSampleDataset() {
        val samples = SampleDatasetProvider.getSampleImages()
        if (samples.isNotEmpty()) {
            val first = samples.first()
            _uiState.update {
                it.copy(
                    datasetImages = samples,
                    currentImageIndex = 0,
                    currentBitmap = first.bitmap,
                    currentImageName = first.name,
                    boundingBoxes = first.boundingBoxes,
                    selectedBoxId = first.boundingBoxes.firstOrNull()?.id
                )
            }
        }
    }

    fun selectDatasetImage(index: Int) {
        val state = _uiState.value
        if (index < 0 || index >= state.datasetImages.size) return

        // Save current bounding boxes to current image item before switching
        val updatedList = state.datasetImages.toMutableList()
        val currentItem = updatedList[state.currentImageIndex]
        updatedList[state.currentImageIndex] = currentItem.copy(boundingBoxes = state.boundingBoxes)

        val nextItem = updatedList[index]
        _uiState.update {
            it.copy(
                datasetImages = updatedList,
                currentImageIndex = index,
                currentBitmap = nextItem.bitmap,
                currentImageUri = nextItem.uri,
                currentImageName = nextItem.name,
                boundingBoxes = nextItem.boundingBoxes,
                selectedBoxId = null,
                zoom = 1f,
                panOffset = Offset.Zero,
                undoStack = emptyList(),
                redoStack = emptyList()
            )
        }
    }

    fun importImageFromUri(context: Context, uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }

                val resolvedName = fileName ?: "imported_${System.currentTimeMillis()}.jpg"
                val newItem = DatasetImageItem(
                    name = resolvedName,
                    uri = uri,
                    bitmap = bitmap,
                    width = bitmap.width,
                    height = bitmap.height,
                    boundingBoxes = emptyList()
                )

                _uiState.update {
                    val updated = it.datasetImages + newItem
                    it.copy(
                        datasetImages = updated,
                        currentImageIndex = updated.lastIndex,
                        currentBitmap = bitmap,
                        currentImageUri = uri,
                        currentImageName = resolvedName,
                        boundingBoxes = emptyList(),
                        selectedBoxId = null,
                        zoom = 1f,
                        panOffset = Offset.Zero,
                        infoMessage = "Image imported: $resolvedName (${bitmap.width}x${bitmap.height})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load image: ${e.message}") }
            }
        }
    }

    fun importImageFromBitmap(bitmap: Bitmap, name: String = "camera_${System.currentTimeMillis()}.jpg") {
        val newItem = DatasetImageItem(
            name = name,
            bitmap = bitmap,
            width = bitmap.width,
            height = bitmap.height,
            boundingBoxes = emptyList()
        )
        _uiState.update {
            val updated = it.datasetImages + newItem
            it.copy(
                datasetImages = updated,
                currentImageIndex = updated.lastIndex,
                currentBitmap = bitmap,
                currentImageUri = null,
                currentImageName = name,
                boundingBoxes = emptyList(),
                selectedBoxId = null,
                zoom = 1f,
                panOffset = Offset.Zero,
                infoMessage = "New image captured: $name"
            )
        }
    }

    fun applyCroppedBitmap(cropped: Bitmap) {
        val current = _uiState.value.currentImageName
        val newName = if (current.contains("_crop")) current else "cropped_$current"
        _uiState.update {
            it.copy(
                currentBitmap = cropped,
                currentImageName = newName,
                // Clear or adapt boxes for cropped image to maintain dataset integrity
                boundingBoxes = emptyList(),
                selectedBoxId = null,
                zoom = 1f,
                panOffset = Offset.Zero,
                showCropDialog = false,
                infoMessage = "Image cropped to ${cropped.width}x${cropped.height} px"
            )
        }
    }

    fun addBoundingBox(box: BoundingBox) {
        saveSnapshotForUndo()
        _uiState.update {
            val newBoxes = it.boundingBoxes + box
            it.copy(
                boundingBoxes = newBoxes,
                selectedBoxId = box.id
            )
        }
    }

    fun updateBoundingBox(updatedBox: BoundingBox) {
        _uiState.update {
            val list = it.boundingBoxes.map { b -> if (b.id == updatedBox.id) updatedBox else b }
            it.copy(boundingBoxes = list)
        }
    }

    fun selectBoundingBox(boxId: String?) {
        _uiState.update { it.copy(selectedBoxId = boxId) }
    }

    fun deleteBoundingBox(boxId: String) {
        saveSnapshotForUndo()
        _uiState.update {
            val list = it.boundingBoxes.filterNot { b -> b.id == boxId }
            it.copy(
                boundingBoxes = list,
                selectedBoxId = if (it.selectedBoxId == boxId) null else it.selectedBoxId
            )
        }
    }

    fun clearAllBoxes() {
        if (_uiState.value.boundingBoxes.isEmpty()) return
        saveSnapshotForUndo()
        _uiState.update { it.copy(boundingBoxes = emptyList(), selectedBoxId = null) }
    }

    fun changeBoxClass(boxId: String, newClass: LabelClass) {
        saveSnapshotForUndo()
        _uiState.update {
            val list = it.boundingBoxes.map { b ->
                if (b.id == boxId) b.copy(labelClass = newClass) else b
            }
            it.copy(boundingBoxes = list)
        }
    }

    fun setSelectedClass(cls: LabelClass) {
        _uiState.update {
            it.copy(selectedClass = cls)
        }
        // If a box is selected, also instantly reclassify it
        val selectedId = _uiState.value.selectedBoxId
        if (selectedId != null) {
            changeBoxClass(selectedId, cls)
        }
    }

    fun addClass(name: String, color: Color) {
        _uiState.update {
            val nextIndex = it.availableClasses.size
            val newCls = LabelClass(name = name, color = color, classIndex = nextIndex)
            val updated = it.availableClasses + newCls
            it.copy(availableClasses = updated, selectedClass = newCls)
        }
    }

    fun deleteClass(cls: LabelClass) {
        _uiState.update {
            if (it.availableClasses.size <= 1) return@update it
            val updated = it.availableClasses.filterNot { c -> c.id == cls.id }
            val fallback = updated.first()
            val newBoxes = it.boundingBoxes.map { b ->
                if (b.labelClass.id == cls.id) b.copy(labelClass = fallback) else b
            }
            it.copy(
                availableClasses = updated,
                selectedClass = if (it.selectedClass.id == cls.id) fallback else it.selectedClass,
                boundingBoxes = newBoxes
            )
        }
    }

    private fun saveSnapshotForUndo() {
        val currentBoxes = _uiState.value.boundingBoxes
        _uiState.update {
            it.copy(
                undoStack = (it.undoStack + listOf(currentBoxes)).takeLast(25),
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.undoStack.isEmpty()) return
        val previousBoxes = state.undoStack.last()
        val remainingUndo = state.undoStack.dropLast(1)
        _uiState.update {
            it.copy(
                boundingBoxes = previousBoxes,
                undoStack = remainingUndo,
                redoStack = it.redoStack + listOf(state.boundingBoxes),
                selectedBoxId = null
            )
        }
    }

    fun redo() {
        val state = _uiState.value
        if (state.redoStack.isEmpty()) return
        val nextBoxes = state.redoStack.last()
        val remainingRedo = state.redoStack.dropLast(1)
        _uiState.update {
            it.copy(
                boundingBoxes = nextBoxes,
                redoStack = remainingRedo,
                undoStack = it.undoStack + listOf(state.boundingBoxes),
                selectedBoxId = null
            )
        }
    }

    fun importPascalVocXml(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (dimensions, parsedBoxes) = PascalVocParser.parseVocXml(
                    inputStream,
                    _uiState.value.availableClasses
                )

                // Add any newly discovered classes from XML
                val currentClasses = _uiState.value.availableClasses.toMutableList()
                parsedBoxes.forEach { box ->
                    if (currentClasses.none { it.name.equals(box.labelClass.name, ignoreCase = true) }) {
                        currentClasses.add(box.labelClass)
                    }
                }

                saveSnapshotForUndo()
                _uiState.update {
                    it.copy(
                        availableClasses = currentClasses,
                        boundingBoxes = parsedBoxes,
                        selectedBoxId = parsedBoxes.firstOrNull()?.id,
                        infoMessage = "Imported ${parsedBoxes.size} annotations from VOC XML"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to parse VOC XML: ${e.message}") }
            }
        }
    }

    fun importYoloTxt(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsedBoxes = YoloParser.parseYolo(
                    inputStream,
                    _uiState.value.availableClasses
                )
                saveSnapshotForUndo()
                _uiState.update {
                    it.copy(
                        boundingBoxes = parsedBoxes,
                        selectedBoxId = parsedBoxes.firstOrNull()?.id,
                        infoMessage = "Imported ${parsedBoxes.size} annotations from YOLO TXT"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to parse YOLO labels: ${e.message}") }
            }
        }
    }

    fun setTransform(zoom: Float, pan: Offset) {
        _uiState.update { it.copy(zoom = zoom, panOffset = pan) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoom = (it.zoom + 0.5f).coerceAtMost(6f)) }
    }

    fun zoomOut() {
        _uiState.update {
            val z = (it.zoom - 0.5f).coerceAtLeast(1f)
            it.copy(zoom = z, panOffset = if (z == 1f) Offset.Zero else it.panOffset)
        }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoom = 1f, panOffset = Offset.Zero) }
    }

    fun togglePanZoomMode() {
        _uiState.update { it.copy(isPanZoomMode = !it.isPanZoomMode) }
    }

    fun setShowClassDialog(show: Boolean) {
        _uiState.update { it.copy(showClassDialog = show) }
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun setShowCropDialog(show: Boolean) {
        _uiState.update { it.copy(showCropDialog = show) }
    }

    fun setShowBoxInspectorSheet(show: Boolean) {
        _uiState.update { it.copy(showBoxInspectorSheet = show) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
