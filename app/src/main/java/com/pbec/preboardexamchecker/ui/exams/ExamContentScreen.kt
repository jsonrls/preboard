package com.pbec.preboardexamchecker.ui.exams

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.pbec.preboardexamchecker.ui.viewmodels.ExamContentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamContentScreen(
    navController: NavController,
    examContentViewModel: ExamContentViewModel = hiltViewModel()
) {
    val exam by examContentViewModel.exam.collectAsState()
    val questionsInSet by examContentViewModel.questionsInSet.collectAsState()
    val message by examContentViewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                examContentViewModel.clearMessage() 
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri: Uri? ->
            uri?.let {
                examContentViewModel.exportExamContentToPdf(it)
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar("PDF export cancelled.")
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = exam?.examName ?: "Exam Content") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        createDocumentLauncher.launch("${exam?.examName}_${System.currentTimeMillis()}.pdf")
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Export PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(0,0,0,0) 
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (questionsInSet.isEmpty() && message == null) {
                Text("Loading exam content...", modifier = Modifier.padding(16.dp))
            } else if (message != null && !message!!.contains("Exporting PDF")) {
                Text(message!!, modifier = Modifier.padding(16.dp))
            } else if (questionsInSet.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp) 
                ) {
                    itemsIndexed(questionsInSet, key = { _, question -> question.id }) { index, question ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("${index + 1}. ", style = MaterialTheme.typography.bodyLarge)
                                MathTextView(
                                    text = question.questionText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(modifier = Modifier.padding(start = 24.dp)) {
                                OptionRow("A", question.optionA)
                                OptionRow("B", question.optionB)
                                OptionRow("C", question.optionC)
                                OptionRow("D", question.optionD)
                            }
                            
                            question.correctAnswer?.let {
                                Text(
                                    "Correct Answer: $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionRow(prefix: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$prefix. ", style = MaterialTheme.typography.bodyLarge)
        MathTextView(
            text = text,
            modifier = Modifier.weight(1f)
        )
    }
}
