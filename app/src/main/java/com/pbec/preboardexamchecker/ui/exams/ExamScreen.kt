package com.pbec.preboardexamchecker.ui.exams

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.ui.Screen
import com.pbec.preboardexamchecker.ui.viewmodels.ExamBankViewModel
import com.pbec.preboardexamchecker.ui.viewmodels.ExamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    navController: NavController,
    subject: String,
    viewModel: ExamViewModel = hiltViewModel(),
    examBankViewModel: ExamBankViewModel = hiltViewModel()
) {
    val exams by viewModel.exams.collectAsState()
    val message by viewModel.message.collectAsState()
    val questionsByImportSession by examBankViewModel.questionsByImportSession.collectAsState()

    var showGenerateExamDialog by remember { mutableStateOf(false) }
    var numberOfQuestionsInput by remember { mutableStateOf("100") }
    var numberOfQuestionsInputError by remember { mutableStateOf(false) }
    var selectedImportSessionIds by remember { mutableStateOf(setOf<String>()) }
    var importSessionSelectionError by remember { mutableStateOf(false) }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var examToRename by remember { mutableStateOf<Exam?>(null) }
    var newExamNameInput by remember { mutableStateOf("") }

    val sortedImportSessionIds = remember(questionsByImportSession.keys) {
        val sessionKeys = questionsByImportSession.keys
        val manualSession = sessionKeys.filter { it == "manual" || it.startsWith("manual_") }
        val importedSessions = sessionKeys.filter { it != "manual" && !it.startsWith("manual_") }.sortedDescending()
        importedSessions + manualSession
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$subject Exams") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)  
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)  
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exams.isEmpty()) {
                    Text(
                        text = if (questionsByImportSession.isEmpty()) {
                            "No exams or questions yet. Go to the exam bank to import questions."
                        } else {
                            "No exams yet. Generate an exam from the question bank."
                        },
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(top = 16.dp) 
                    ) {
                        itemsIndexed(exams.reversed(), key = { _, exam -> exam.id }) { _, exam ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        navController.navigate(Screen.ExamDetails.createRoute(exam.id))
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.examName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Created: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(exam.createdAt))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                examToRename = exam
                                                newExamNameInput = exam.examName
                                                showRenameDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename Exam",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                examToDelete = exam
                                                showDeleteConfirmationDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Exam",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            FloatingActionButton(
                onClick = { showGenerateExamDialog = true }
            ) {
                Icon(Icons.Filled.Add, "Generate Exam")
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (exams.isNotEmpty()) {
                            // TODO: Implement PDF export logic
                        }
                    }
                ) {
                    Icon(Icons.Filled.FileDownload, "Export PDF Exam")
                }

                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ExamBank.createRoute(subject)) }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, "Go to Exam Bank")
                }
            }
        }

        message?.let {
            AlertDialog(
                onDismissRequest = { viewModel.clearMessage() },
                title = { Text("Information") },
                text = { Text(it) },
                confirmButton = {
                    Button(onClick = { viewModel.clearMessage() }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showGenerateExamDialog) {
            AlertDialog(
                onDismissRequest = {
                    showGenerateExamDialog = false
                    numberOfQuestionsInputError = false
                    importSessionSelectionError = false
                    selectedImportSessionIds = emptySet()
                },
                title = { Text("Generate Exam") },
                text = {
                    Column {
                        Text("Enter the number of questions (1-1000):")
                        OutlinedTextField(
                            value = numberOfQuestionsInput,
                            onValueChange = { newValue ->
                                val filteredValue = newValue.filter { it.isDigit() }
                                if (filteredValue.length <= 4) {
                                    numberOfQuestionsInput = filteredValue
                                    numberOfQuestionsInputError = try {
                                        val num = filteredValue.toInt()
                                        num < 1 || num > 1000
                                    } catch (e: NumberFormatException) {
                                        filteredValue.isNotEmpty()
                                    }
                                }
                            },
                            isError = numberOfQuestionsInputError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Number of Questions") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        if (numberOfQuestionsInputError) {
                            Text(
                                "Please enter a number between 1 and 1000.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select question banks to generate from:")
                        if (questionsByImportSession.isEmpty()) {
                            Text(
                                "No question banks available.",
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                itemsIndexed(sortedImportSessionIds, key = { _, id -> id }) { index, importSessionId ->
                                    val questionsInSession = questionsByImportSession[importSessionId] ?: emptyList()
                                    val headerText = if (importSessionId == "manual" || importSessionId.startsWith("manual_")) {
                                        "Manually Added"
                                    } else {
                                        questionsInSession.firstOrNull()?.fileName ?: "Imported File"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = importSessionId in selectedImportSessionIds,
                                            onCheckedChange = { isChecked ->
                                                selectedImportSessionIds = if (isChecked) {
                                                    selectedImportSessionIds + importSessionId
                                                } else {
                                                    selectedImportSessionIds - importSessionId
                                                }
                                                importSessionSelectionError = selectedImportSessionIds.isEmpty()
                                            }
                                        )
                                        Text(
                                            text = if (importSessionId == "manual" || importSessionId.startsWith("manual_")) {
                                                "$headerText (${questionsInSession.size} questions)"
                                            } else {
                                                "$headerText #${String.format(Locale.getDefault(), "%03d", index + 1)} (${questionsInSession.size} questions)"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedImportSessionIds = sortedImportSessionIds.toSet()
                                        importSessionSelectionError = false
                                    }
                                ) {
                                    Text("Select All")
                                }
                                TextButton(
                                    onClick = {
                                        selectedImportSessionIds = emptySet()
                                        importSessionSelectionError = true
                                    }
                                ) {
                                    Text("Deselect All")
                                }
                            }
                        }
                        if (importSessionSelectionError) {
                            Text(
                                "Please select at least one question bank.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val numQuestions = numberOfQuestionsInput.toIntOrNull()
                            importSessionSelectionError = selectedImportSessionIds.isEmpty()
                            if (numQuestions != null && numQuestions >= 1 && numQuestions <= 1000 && !importSessionSelectionError) {
                                viewModel.generateExam(numQuestions, selectedImportSessionIds.toList())
                                showGenerateExamDialog = false
                                numberOfQuestionsInputError = false
                                importSessionSelectionError = false
                                selectedImportSessionIds = emptySet()
                            } else {
                                numberOfQuestionsInputError = numQuestions == null || numQuestions < 1 || numQuestions > 1000
                                importSessionSelectionError = selectedImportSessionIds.isEmpty()
                            }
                        },
                        enabled = !numberOfQuestionsInputError && numberOfQuestionsInput.isNotEmpty() && !importSessionSelectionError
                    ) {
                        Text("Generate")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showGenerateExamDialog = false
                        numberOfQuestionsInputError = false
                        importSessionSelectionError = false
                        selectedImportSessionIds = emptySet()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete exam '${examToDelete?.examName}'? This cannot be undone.") },
                confirmButton = {
                    Button(onClick = {
                        examToDelete?.let { viewModel.deleteExam(it) }
                        showDeleteConfirmationDialog = false
                        examToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDeleteConfirmationDialog = false
                        examToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Exam") },
                text = {
                    OutlinedTextField(
                        value = newExamNameInput,
                        onValueChange = { newExamNameInput = it },
                        label = { Text("Exam Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        examToRename?.let { viewModel.renameExam(it, newExamNameInput) }
                        showRenameDialog = false
                        examToRename = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRenameDialog = false
                        examToRename = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}