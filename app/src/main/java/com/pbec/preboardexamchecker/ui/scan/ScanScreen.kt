package com.pbec.preboardexamchecker.ui.scan

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.ui.exams.MathTextView
import com.pbec.preboardexamchecker.utils.AutoSheetDetector
import com.pbec.preboardexamchecker.utils.ClassRecordExcelWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ScanScreen(navController: NavController, viewModel: ScanViewModel = hiltViewModel()) {
    val selectedExam by viewModel.selectedExam.collectAsState()
    val currentExam = selectedExam // Local variable for smart casting
    val availableExams by viewModel.availableExams.collectAsState()
    val selectedCluster by viewModel.selectedCluster.collectAsState()
    val examsInSelectedCluster = remember(availableExams, selectedCluster) {
        val cluster = selectedCluster
        if (cluster.isNullOrBlank()) emptyList() else availableExams.filter { it.subject == cluster }
    }

    val isTopLeftDetected by viewModel.isTopLeftDetected.collectAsState()
    val isTopRightDetected by viewModel.isTopRightDetected.collectAsState()
    val isBottomLeftDetected by viewModel.isBottomLeftDetected.collectAsState()
    val isBottomRightDetected by viewModel.isBottomRightDetected.collectAsState()
    val allFourCornersDetected by viewModel.allFourCornersDetected.collectAsState()

    val context = LocalContext.current
    var permissionState by remember { mutableIntStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("ScanScreen", "Permission result: isGranted=$isGranted")
        permissionState = if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    LaunchedEffect(currentExam) {
        if (currentExam != null) {
            permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    when {
        currentExam == null -> {
            if (selectedCluster == null) {
                ClusterSelectionScreen(
                    clusters = ScanViewModel.EXAM_CLUSTERS,
                    onClusterSelected = { cluster -> viewModel.selectCluster(cluster) }
                )
            } else {
                ExamSelectionScreen(
                    cluster = selectedCluster!!,
                    exams = examsInSelectedCluster,
                    onExamSelected = { exam -> viewModel.selectExam(exam) },
                    onBackToClusters = { viewModel.selectCluster(null) }
                )
            }
        }
        permissionState != PackageManager.PERMISSION_GRANTED -> {
            CameraPermissionScreen(
                navController = navController,
                viewModel = viewModel,
                requestPermission = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }
        else -> {
            CaptureMode(
                navController = navController,
                selectedExam = currentExam,
                isTopLeftDetected = isTopLeftDetected,
                isTopRightDetected = isTopRightDetected,
                isBottomLeftDetected = isBottomLeftDetected,
                isBottomRightDetected = isBottomRightDetected,
                allFourCornersDetected = allFourCornersDetected,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ClusterSelectionScreen(
    clusters: List<String>,
    onClusterSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select Cluster",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Before scanning, choose the cluster first.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(clusters) { cluster ->
                Button(
                    onClick = { onClusterSelected(cluster) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(cluster, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExamSelectionScreen(
    cluster: String,
    exams: List<Exam>,
    onExamSelected: (Exam) -> Unit,
    onBackToClusters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = cluster,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Select Generated Exam",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Select the exam in this cluster so the correct answer key is used during scanning.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (exams.isEmpty()) {
            Text("No generated exams found for this cluster.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onBackToClusters) {
                Text("Back to Clusters")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(exams) { exam ->
                    Button(
                        onClick = { onExamSelected(exam) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(exam.examName, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBackToClusters) {
                Text("Back to Clusters")
            }
        }
    }
}

@Composable
fun CameraPermissionScreen(
    navController: NavController,
    viewModel: ScanViewModel,
    requestPermission: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val showRationale = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activity?.let {
            showRationale.value = ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (showRationale.value) {
                "Camera permission is crucial for scanning. Please enable it in settings or grant it."
            } else {
                "Camera permission is required to scan answer sheets."
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = { requestPermission() },
            modifier = Modifier.padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Request Permission")
        }
        
        Button(
            onClick = {
                viewModel.selectExam(null)
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Cancel")
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CaptureMode(
    navController: NavController,
    selectedExam: Exam,
    isTopLeftDetected: Boolean,
    isTopRightDetected: Boolean,
    isBottomLeftDetected: Boolean,
    isBottomRightDetected: Boolean,
    allFourCornersDetected: Boolean,
    viewModel: ScanViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var pendingEvaluation by remember { mutableStateOf<ScanEvaluationResult?>(null) }
    var classRecordUri by remember { mutableStateOf(appPrefs.getString(CLASS_RECORD_URI_KEY, null)?.let(Uri::parse)) }
    val classRecordPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No class record selected.")
            }
            return@rememberLauncherForActivityResult
        }

        val evaluation = pendingEvaluation
        classRecordUri = uri
        appPrefs.edit().putString(CLASS_RECORD_URI_KEY, uri.toString()).apply()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        if (evaluation == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Class record selected. Capture to auto-save scan results.")
            }
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ClassRecordExcelWriter.appendResult(context, uri, evaluation)
                }
            }.onSuccess {
                snackbarHostState.showSnackbar(
                    "Saved: ${evaluation.studentId} - ${evaluation.correctCount}/${evaluation.totalItems} (${String.format(Locale.US, "%.2f", evaluation.scorePercent)}%)"
                )
                pendingEvaluation = null
            }.onFailure { error ->
                snackbarHostState.showSnackbar("Failed to update class record: ${error.message ?: "Unknown error"}")
            }
        }
    }
    
    val screenWidthPx = remember { mutableFloatStateOf(0f) }
    val density = remember { mutableFloatStateOf(0f) }
    
    val randomQuestion by viewModel.randomQuestionToVerify.collectAsState()

    SideEffect {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            screenWidthPx.floatValue = windowMetrics.bounds.width().toFloat()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidthPx.floatValue = displayMetrics.widthPixels.toFloat()
        }
        density.floatValue = context.resources.displayMetrics.density
    }

    var cameraPreviewHeightPx by remember { mutableIntStateOf(0) }
    var cameraPreviewActualHeightPx by remember { mutableFloatStateOf(2448f) } 
    val imageAnalyzer = remember(viewModel) {
        ImageAnalyzer(viewModel, context.applicationContext)
    }

    val topLeftColor by animateColorAsState(if (isTopLeftDetected) Color.Green else Color.Red, label = "topLeftColor")
    val topRightColor by animateColorAsState(if (isTopRightDetected) Color.Green else Color.Red, label = "topRightColor")
    val bottomLeftColor by animateColorAsState(if (isBottomLeftDetected) Color.Green else Color.Red, label = "bottomLeftColor")
    val bottomRightColor by animateColorAsState(if (isBottomRightDetected) Color.Green else Color.Red, label = "bottomRightColor")

    val screenWidthDp = screenWidthPx.floatValue / density.floatValue
    val guideSizeDp = screenWidthDp * 0.3f
    val aspectRatioWidth = 8f
    val aspectRatioHeight = 13f
    val targetAspectRatio = aspectRatioWidth / aspectRatioHeight

    val finalRectWidthDp = remember(screenWidthDp, cameraPreviewHeightPx.toFloat(), targetAspectRatio, density.floatValue) {
        val cameraPreviewHeightDp = if (density.floatValue > 0) cameraPreviewHeightPx.toFloat() / density.floatValue else 1f
        val cameraPreviewAspectRatio = screenWidthDp / cameraPreviewHeightDp
        if (cameraPreviewAspectRatio > targetAspectRatio) cameraPreviewHeightDp * targetAspectRatio else screenWidthDp
    }

    val finalRectHeightDp = remember(screenWidthDp, cameraPreviewHeightPx.toFloat(), targetAspectRatio, density.floatValue) {
        val cameraPreviewHeightDp = if (density.floatValue > 0) cameraPreviewHeightPx.toFloat() / density.floatValue else 1f
        val cameraPreviewAspectRatio = screenWidthDp / cameraPreviewHeightDp
        if (cameraPreviewAspectRatio > targetAspectRatio) cameraPreviewHeightDp else screenWidthDp / targetAspectRatio
    }

    val offsetXDp = remember(screenWidthDp, finalRectWidthDp) { (screenWidthDp - finalRectWidthDp) / 2f }
    val offsetYDp = remember(cameraPreviewHeightPx, finalRectHeightDp, density.floatValue) { 
        val hDp = if (density.floatValue > 0) cameraPreviewHeightPx.toFloat() / density.floatValue else 0f
        (hDp - finalRectHeightDp) / 2f 
    }

    LaunchedEffect(cameraPreviewHeightPx, finalRectWidthDp, finalRectHeightDp, offsetXDp, offsetYDp, guideSizeDp, cameraPreviewActualHeightPx) {
        if (cameraPreviewHeightPx > 0 && cameraPreviewActualHeightPx > 0 && density.floatValue > 0) {
            imageAnalyzer.updateExpectedRectDimensions(
                expectedRectWidthDp = finalRectWidthDp,
                expectedRectHeightDp = finalRectHeightDp,
                screenWidthPx = screenWidthPx.floatValue,
                cameraPreviewActualHeightPx = cameraPreviewActualHeightPx,
                density = density.floatValue,
                guideOffsetX_Dp = offsetXDp,
                guideOffsetY_Dp = offsetYDp,
                guideSizeDp = guideSizeDp
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                // Random Verification Question Card
                randomQuestion?.let { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Verification Question", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.pickRandomQuestionToVerify(selectedExam) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = "New Random Question", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            MathTextView(text = question.questionText)
                            Text("Answer: ${question.correctAnswer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.selectExam(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Exit", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            classRecordPickerLauncher.launch(
                                arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp).padding(start = 6.dp)
                    ) {
                        Text(if (classRecordUri == null) "Record" else "Record ✓", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val bitmap = previewViewRef?.bitmap
                            if (bitmap == null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Unable to capture image. Please try again.")
                                }
                            } else {
                                coroutineScope.launch {
                                    try {
                                        val savedPath = saveCapturedBitmap(context, bitmap)
                                        if (savedPath != null) {
                                            snackbarHostState.showSnackbar("Capture saved. Auto-detecting sheet...")
                                            val detected = withContext(Dispatchers.Default) {
                                                AutoSheetDetector.detect(
                                                    bitmap = bitmap,
                                                    totalItems = selectedExam.questionIds.size
                                                )
                                            }

                                            val autoStudentId = detected.studentId.takeIf { it.isNotBlank() }
                                                ?: "UNKNOWN-${System.currentTimeMillis() % 100000}"
                                            val evaluation = viewModel.evaluateCapturedAnswers(
                                                exam = selectedExam,
                                                studentId = autoStudentId,
                                                normalizedAnswers = detected.answers
                                            )
                                            pendingEvaluation = evaluation

                                            val targetUri = classRecordUri
                                            if (targetUri == null) {
                                                classRecordPickerLauncher.launch(
                                                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                                )
                                            } else {
                                                runCatching {
                                                    withContext(Dispatchers.IO) {
                                                        ClassRecordExcelWriter.appendResult(context, targetUri, evaluation)
                                                    }
                                                }.onSuccess {
                                                    snackbarHostState.showSnackbar(
                                                        "Auto-saved: ${evaluation.studentId} - ${evaluation.correctCount}/${evaluation.totalItems} (${String.format(Locale.US, "%.2f", evaluation.scorePercent)}%)"
                                                    )
                                                    pendingEvaluation = null
                                                }.onFailure { error ->
                                                    snackbarHostState.showSnackbar("Save failed: ${error.message ?: "Unknown error"}")
                                                }
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("Capture failed. Please try again.")
                                        }
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                            }
                        },
                        enabled = allFourCornersDetected,
                        colors = ButtonDefaults.buttonColors(containerColor = if (allFourCornersDetected) MaterialTheme.colorScheme.primary else Color.Gray),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 6.dp)
                    ) {
                        Text(if (allFourCornersDetected) "Capture" else "Scan", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.resetAllDetection() },
                        enabled = isTopLeftDetected || isTopRightDetected || isBottomLeftDetected || isBottomRightDetected,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Re-scan", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onGloballyPositioned { layoutCoordinates ->
                    cameraPreviewHeightPx = layoutCoordinates.size.height
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef = this
                    }
                },
                update = { previewView ->
                    previewViewRef = previewView
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(android.util.Size(2448, 2448))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                var firstFrameProcessed = false
                                analysis.setAnalyzer(cameraExecutor) { image ->
                                    if (!firstFrameProcessed) {
                                        cameraPreviewActualHeightPx = image.height.toFloat()
                                        firstFrameProcessed = true
                                    }
                                    image.close()
                                }
                                analysis.setAnalyzer(cameraExecutor, imageAnalyzer)
                            }
                        try {
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (exc: Exception) {
                            Log.e("ScanScreen", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Overlays for corners
            Box(modifier = Modifier.size(guideSizeDp.dp).offset(x = offsetXDp.dp, y = offsetYDp.dp).border(2.dp, topLeftColor))
            Box(modifier = Modifier.size(guideSizeDp.dp).offset(x = (offsetXDp + finalRectWidthDp - guideSizeDp).dp, y = offsetYDp.dp).border(2.dp, topRightColor))
            Box(modifier = Modifier.size(guideSizeDp.dp).offset(x = offsetXDp.dp, y = (offsetYDp + finalRectHeightDp - guideSizeDp).dp).border(2.dp, bottomLeftColor))
            Box(modifier = Modifier.size(guideSizeDp.dp).offset(x = (offsetXDp + finalRectWidthDp - guideSizeDp).dp, y = (offsetYDp + finalRectHeightDp - guideSizeDp).dp).border(2.dp, bottomRightColor))

            Text(
                text = "Exam: ${selectedExam.examName}",
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                fontSize = 14.sp
            )
            
            if (allFourCornersDetected) {
                Text(
                    text = "Ready to Capture",
                    color = Color.Green,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)
                )
            }
        }
    }

}

private fun saveCapturedBitmap(context: Context, bitmap: Bitmap): String? {
    return try {
        val capturesDir = File(context.getExternalFilesDir(null), "captures").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(capturesDir, "capture_$timestamp.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file.absolutePath
    } catch (e: Exception) {
        Log.e("ScanScreen", "Failed to save captured bitmap", e)
        null
    }
}

private const val CLASS_RECORD_URI_KEY = "class_record_uri"
