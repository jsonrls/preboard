package com.pbec.preboardexamchecker.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.pbec.preboardexamchecker.data.models.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _selectedYearLevel = MutableStateFlow("All")
    val selectedYearLevel = _selectedYearLevel.asStateFlow()
    private val _selectedBlock = MutableStateFlow("All")
    val selectedBlock = _selectedBlock.asStateFlow()
    private val _selectedCourse = MutableStateFlow("All")
    val selectedCourse = _selectedCourse.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val _studentsState = MutableStateFlow<List<Student>>(emptyList())
    val studentsState: StateFlow<List<Student>> = _studentsState.asStateFlow()
    val allStudents: StateFlow<List<Student>> = _students.asStateFlow()

    init {
        observeStudents()
        
        viewModelScope.launch {
            combine(_students, _searchQuery, _selectedYearLevel, _selectedBlock, _selectedCourse) { students, query, yearLevel, block, course ->
                students.filter { student ->
                    val queryMatch = query.isBlank() ||
                        student.name.contains(query, ignoreCase = true) ||
                        student.studentId.contains(query, ignoreCase = true)
                    val yearMatch = yearLevel == "All" || student.yearLevel.equals(yearLevel, ignoreCase = true)
                    val blockMatch = block == "All" || student.section.equals(block, ignoreCase = true)
                    val courseMatch = course == "All" || student.program.equals(course, ignoreCase = true)

                    queryMatch && yearMatch && blockMatch && courseMatch
                }
            }.collect {
                _studentsState.value = it
            }
        }
    }

    private fun observeStudents() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            _isLoading.value = false
            _uiEvent.tryEmit(UiEvent.ShowSnackbar("User not authenticated. Please log in."))
            return
        }

        _isLoading.value = true
        firestore.collection("students")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    val message = if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        "Permission Denied: Please ensure you are logged in as an authorized user."
                    } else {
                        "Error loading students: ${error.message}"
                    }
                    _uiEvent.tryEmit(UiEvent.ShowSnackbar(message))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val studentList = snapshot.toObjects(Student::class.java)
                    _students.value = studentList
                }
            }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateYearLevelFilter(yearLevel: String) {
        _selectedYearLevel.value = yearLevel
    }

    fun updateBlockFilter(block: String) {
        _selectedBlock.value = block
    }

    fun updateCourseFilter(course: String) {
        _selectedCourse.value = course
    }

    fun clearFilters() {
        _selectedYearLevel.value = "All"
        _selectedBlock.value = "All"
        _selectedCourse.value = "All"
    }

    fun addStudent(name: String, studentId: String, program: String, yearLevel: String, section: String) {
        val student = hashMapOf(
            "name" to name,
            "studentId" to studentId,
            "program" to program,
            "yearLevel" to yearLevel,
            "section" to section,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("students")
            .add(student)
            .addOnSuccessListener {
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Student added successfully"))
            }
            .addOnFailureListener { e ->
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Failed to add student: ${e.message}"))
            }
    }

    fun updateStudent(documentId: String, name: String, studentId: String, program: String, yearLevel: String, section: String) {
        val updates = mapOf(
            "name" to name,
            "studentId" to studentId,
            "program" to program,
            "yearLevel" to yearLevel,
            "section" to section
        )

        firestore.collection("students").document(documentId)
            .update(updates)
            .addOnSuccessListener {
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Student updated successfully"))
            }
            .addOnFailureListener { e ->
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Failed to update student: ${e.message}"))
            }
    }

    fun deleteStudent(documentId: String) {
        firestore.collection("students").document(documentId)
            .delete()
            .addOnSuccessListener {
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Student deleted successfully"))
            }
            .addOnFailureListener { e ->
                _uiEvent.tryEmit(UiEvent.ShowSnackbar("Failed to delete student: ${e.message}"))
            }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
