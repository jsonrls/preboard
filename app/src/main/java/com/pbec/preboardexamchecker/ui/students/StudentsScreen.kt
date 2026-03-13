package com.pbec.preboardexamchecker.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pbec.preboardexamchecker.data.models.Student
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel = hiltViewModel()
) {
    val students by viewModel.studentsState.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedYearLevel by viewModel.selectedYearLevel.collectAsState()
    val selectedBlock by viewModel.selectedBlock.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val yearLevelOptions = remember(allStudents) {
        listOf("All") + allStudents.map { it.yearLevel.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val blockOptions = remember(allStudents) {
        listOf("All") + allStudents.map { it.section.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val courseOptions = remember(allStudents) {
        listOf("All") + allStudents.map { it.program.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is StudentsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search Students...") },
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .height(50.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && students.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentFilterDropdown(
                            label = "Year",
                            selectedValue = selectedYearLevel,
                            options = yearLevelOptions,
                            onValueSelected = viewModel::updateYearLevelFilter,
                            modifier = Modifier.weight(1f)
                        )
                        StudentFilterDropdown(
                            label = "Block",
                            selectedValue = selectedBlock,
                            options = blockOptions,
                            onValueSelected = viewModel::updateBlockFilter,
                            modifier = Modifier.weight(1f)
                        )
                        StudentFilterDropdown(
                            label = "Course",
                            selectedValue = selectedCourse,
                            options = courseOptions,
                            onValueSelected = viewModel::updateCourseFilter,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (students.isEmpty()) {
                        EmptyListPlaceholder(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            isSearching = searchQuery.isNotEmpty()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(students, key = { it.id }) { student ->
                                StudentItem(
                                    student = student,
                                    onClick = {
                                        selectedStudent = student
                                        showAddEditDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditStudentDialog(
            student = selectedStudent,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, sid, prog, year, sect ->
                if (selectedStudent == null) {
                    viewModel.addStudent(name, sid, prog, year, sect)
                } else {
                    viewModel.updateStudent(selectedStudent!!.id, name, sid, prog, year, sect)
                }
                showAddEditDialog = false
            },
            onDelete = {
                showDeleteConfirm = true
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${selectedStudent?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedStudent?.let { viewModel.deleteStudent(it.id) }
                    showDeleteConfirm = false
                    showAddEditDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(56.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StudentItem(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(name = student.name)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${student.studentId}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${student.yearLevel} - ${student.section}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StudentAvatar(name: String) {
    val initials = name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyListPlaceholder(modifier: Modifier, isSearching: Boolean) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No students found matching your search." else "No students enrolled yet.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentDialog(
    student: Student?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(student?.name ?: "") }
    var studentId by remember { mutableStateOf(student?.studentId ?: "") }
    var program by remember { mutableStateOf(student?.program ?: "") }
    var yearLevel by remember { mutableStateOf(student?.yearLevel ?: "") }
    var section by remember { mutableStateOf(student?.section ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "Add Student" else "Edit Student") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = program, onValueChange = { program = it }, label = { Text("Program") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = yearLevel, onValueChange = { yearLevel = it }, label = { Text("Year Level") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, studentId, program, yearLevel, section) },
                enabled = name.isNotBlank() && studentId.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (student != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
