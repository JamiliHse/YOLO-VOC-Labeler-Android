package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LabelClass
import com.example.data.repository.DatasetStorageManager
import com.example.ui.canvas.AnnotationCanvas
import com.example.ui.components.*
import com.example.ui.viewmodel.LabelingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLabelingScreen(
    viewModel: LabelingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageManager = remember { DatasetStorageManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Pending SAF Save state
    var pendingSaveContent by remember { mutableStateOf<String?>(null) }
    var pendingMimeType by remember { mutableStateOf("text/xml") }

    // SAF Document Creator launcher for saving XML or YOLO TXT
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(pendingMimeType)
    ) { uri: Uri? ->
        if (uri != null && pendingSaveContent != null) {
            coroutineScope.launch {
                val result = storageManager.saveAnnotationToFile(uri, pendingSaveContent!!)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Annotation file saved successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to save: ${result.exceptionOrNull()?.message}")
                }
                pendingSaveContent = null
            }
        }
    }

    // Media Gallery Picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importImageFromUri(context, it) }
    }

    // Camera Capture launcher (direct preview thumbnail / bitmap)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.importImageFromBitmap(it) }
    }

    // Import existing PASCAL VOC XML launcher
    val openVocXmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.importPascalVocXml(stream)
            }
        }
    }

    // Import existing YOLO TXT launcher
    val openYoloTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.importYoloTxt(stream)
            }
        }
    }

    // Observe info and error messages
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearErrorMessage()
        }
    }

    var showImportMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "LabelFlow",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${state.boundingBoxes.size} boxes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                state.currentImageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Undo & Redo
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = state.undoStack.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = state.redoStack.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }

                        // Crop Tool button
                        IconButton(
                            onClick = { viewModel.setShowCropDialog(true) },
                            enabled = state.currentBitmap != null
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = "Crop Image")
                        }

                        // Import Menu button
                        Box {
                            IconButton(onClick = { showImportMenu = true }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Media")
                            }
                            DropdownMenu(
                                expanded = showImportMenu,
                                onDismissRequest = { showImportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Choose from Gallery") },
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                    onClick = {
                                        showImportMenu = false
                                        galleryPickerLauncher.launch("image/*")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Capture with Camera") },
                                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                                    onClick = {
                                        showImportMenu = false
                                        takePictureLauncher.launch(null)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Import PASCAL VOC (.xml)") },
                                    leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                    onClick = {
                                        showImportMenu = false
                                        openVocXmlLauncher.launch("text/xml")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import YOLO Labels (.txt)") },
                                    leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                                    onClick = {
                                        showImportMenu = false
                                        openYoloTxtLauncher.launch("text/plain")
                                    }
                                )
                            }
                        }

                        // Export Button
                        FilledTonalButton(
                            onClick = { viewModel.setShowExportDialog(true) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export")
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(8.dp)
                        ) {
                            // Quick Class Selector Strip
                            ClassSelectorStrip(
                                classes = state.availableClasses,
                                selectedClass = state.selectedClass,
                                onSelectClass = { viewModel.setSelectedClass(it) },
                                onManageClasses = { viewModel.setShowClassDialog(true) }
                            )

                            Spacer(Modifier.height(4.dp))

                            // Bottom Canvas Controls Row
                            CanvasToolbarRow(
                                zoom = state.zoom,
                                isPanZoomMode = state.isPanZoomMode,
                                onTogglePanZoom = { viewModel.togglePanZoomMode() },
                                onZoomIn = { viewModel.zoomIn() },
                                onZoomOut = { viewModel.zoomOut() },
                                onResetZoom = { viewModel.resetZoom() },
                                onOpenInspector = { viewModel.setShowBoxInspectorSheet(true) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (isWideScreen) {
                // Wide Screen / Tablet / Landscape Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left Workspace Image Selector Thumbnails & Tools
                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(onClick = { viewModel.togglePanZoomMode() }) {
                                Icon(
                                    if (state.isPanZoomMode) Icons.Default.PanTool else Icons.Default.BorderColor,
                                    contentDescription = "Toggle Mode",
                                    tint = if (state.isPanZoomMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.zoomIn() }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                            }
                            IconButton(onClick = { viewModel.zoomOut() }) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                            }
                            IconButton(onClick = { viewModel.resetZoom() }) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Zoom")
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                            // Image Thumbnails in Session
                            Text(
                                "Images",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            state.datasetImages.forEachIndexed { idx, item ->
                                val isSelected = idx == state.currentImageIndex
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clickable { viewModel.selectDatasetImage(idx) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "#${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Center Annotation Canvas Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Class Strip on Top of Canvas
                        ClassSelectorStrip(
                            classes = state.availableClasses,
                            selectedClass = state.selectedClass,
                            onSelectClass = { viewModel.setSelectedClass(it) },
                            onManageClasses = { viewModel.setShowClassDialog(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                        ) {
                            AnnotationCanvas(
                                bitmap = state.currentBitmap,
                                boxes = state.boundingBoxes,
                                activeClass = state.selectedClass,
                                selectedBoxId = state.selectedBoxId,
                                zoom = state.zoom,
                                panOffset = state.panOffset,
                                isPanZoomMode = state.isPanZoomMode,
                                onBoxCreated = { viewModel.addBoundingBox(it) },
                                onBoxUpdated = { viewModel.updateBoundingBox(it) },
                                onBoxSelected = { viewModel.selectBoundingBox(it) },
                                onTransformChanged = { z, p -> viewModel.setTransform(z, p) }
                            )
                        }
                    }

                    // Right Inspector Panel
                    BoxHierarchyList(
                        boxes = state.boundingBoxes,
                        selectedBoxId = state.selectedBoxId,
                        classes = state.availableClasses,
                        imageWidth = state.currentBitmap?.width ?: 1,
                        imageHeight = state.currentBitmap?.height ?: 1,
                        onSelectBox = { viewModel.selectBoundingBox(it) },
                        onDeleteBox = { viewModel.deleteBoundingBox(it) },
                        onChangeBoxClass = { id, cls -> viewModel.changeBoxClass(id, cls) },
                        onClearAllBoxes = { viewModel.clearAllBoxes() },
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Mobile Portrait / Compact Layout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF0F172A))
                ) {
                    AnnotationCanvas(
                        bitmap = state.currentBitmap,
                        boxes = state.boundingBoxes,
                        activeClass = state.selectedClass,
                        selectedBoxId = state.selectedBoxId,
                        zoom = state.zoom,
                        panOffset = state.panOffset,
                        isPanZoomMode = state.isPanZoomMode,
                        onBoxCreated = { viewModel.addBoundingBox(it) },
                        onBoxUpdated = { viewModel.updateBoundingBox(it) },
                        onBoxSelected = { viewModel.selectBoundingBox(it) },
                        onTransformChanged = { z, p -> viewModel.setTransform(z, p) }
                    )

                    // Quick Session Image Switcher floating badge
                    if (state.datasetImages.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                state.datasetImages.forEachIndexed { idx, item ->
                                    val isSelected = idx == state.currentImageIndex
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectDatasetImage(idx) },
                                        label = { Text("Image ${idx + 1}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mobile Bottom Sheet for Box Hierarchy Inspector
        if (state.showBoxInspectorSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setShowBoxInspectorSheet(false) }
            ) {
                BoxHierarchyList(
                    boxes = state.boundingBoxes,
                    selectedBoxId = state.selectedBoxId,
                    classes = state.availableClasses,
                    imageWidth = state.currentBitmap?.width ?: 1,
                    imageHeight = state.currentBitmap?.height ?: 1,
                    onSelectBox = {
                        viewModel.selectBoundingBox(it)
                        viewModel.setShowBoxInspectorSheet(false)
                    },
                    onDeleteBox = { viewModel.deleteBoundingBox(it) },
                    onChangeBoxClass = { id, cls -> viewModel.changeBoxClass(id, cls) },
                    onClearAllBoxes = { viewModel.clearAllBoxes() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }

        // Crop Dialog
        if (state.showCropDialog && state.currentBitmap != null) {
            CropToolDialog(
                bitmap = state.currentBitmap!!,
                onDismiss = { viewModel.setShowCropDialog(false) },
                onCropApplied = { cropped -> viewModel.applyCroppedBitmap(cropped) }
            )
        }

        // Class Management Dialog
        if (state.showClassDialog) {
            ClassManagementDialog(
                classes = state.availableClasses,
                onAddClass = { name, color -> viewModel.addClass(name, color) },
                onDeleteClass = { viewModel.deleteClass(it) },
                onDismiss = { viewModel.setShowClassDialog(false) }
            )
        }

        // Export Dialog
        if (state.showExportDialog) {
            ExportPreviewDialog(
                imageName = state.currentImageName,
                imageWidth = state.currentBitmap?.width ?: 1,
                imageHeight = state.currentBitmap?.height ?: 1,
                boxes = state.boundingBoxes,
                classes = state.availableClasses,
                onSaveSaf = { format, content, defaultFilename ->
                    pendingSaveContent = content
                    pendingMimeType = format.mimeType
                    createDocumentLauncher.launch(defaultFilename)
                },
                onShareFile = { filename, content, mimeType ->
                    coroutineScope.launch {
                        val shareRes = storageManager.createShareableFile(filename, content)
                        if (shareRes.isSuccess) {
                            val uri = shareRes.getOrNull()!!
                            val intent = storageManager.buildShareIntent(uri, mimeType, "Dataset Label - $filename")
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Dataset Label"))
                        } else {
                            snackbarHostState.showSnackbar("Share failed: ${shareRes.exceptionOrNull()?.message}")
                        }
                    }
                },
                onDismiss = { viewModel.setShowExportDialog(false) }
            )
        }
    }
}

@Composable
private fun ClassSelectorStrip(
    classes: List<LabelClass>,
    selectedClass: LabelClass,
    onSelectClass: (LabelClass) -> Unit,
    onManageClasses: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(classes) { cls ->
                val isSelected = cls.id == selectedClass.id
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) cls.color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) cls.color else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onSelectClass(cls) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(cls.color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cls.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onManageClasses,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Manage Classes", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CanvasToolbarRow(
    zoom: Float,
    isPanZoomMode: Boolean,
    onTogglePanZoom: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onOpenInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode toggle: Draw Box vs Pan/Zoom
        FilterChip(
            selected = isPanZoomMode,
            onClick = onTogglePanZoom,
            label = { Text(if (isPanZoomMode) "Pan & Zoom Mode" else "Draw Box Mode") },
            leadingIcon = {
                Icon(
                    if (isPanZoomMode) Icons.Default.PanTool else Icons.Default.BorderColor,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Zoom Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            Text(
                text = "${(zoom * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable { onResetZoom() }
                    .padding(horizontal = 4.dp)
            )
            IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
        }

        // Open Inspector Sheet button
        OutlinedButton(
            onClick = onOpenInspector,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("List")
        }
    }
}
