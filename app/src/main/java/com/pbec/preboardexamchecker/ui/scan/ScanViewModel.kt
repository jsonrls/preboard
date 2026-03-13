package com.pbec.preboardexamchecker.ui.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.data.models.Question
import com.pbec.preboardexamchecker.data.repository.ExamRepository
import com.pbec.preboardexamchecker.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {
    companion object {
        val EXAM_CLUSTERS = listOf("Mathematics", "ESAS", "Professional EE")
    }

    private val _selectedExam = MutableStateFlow<Exam?>(null)
    val selectedExam: StateFlow<Exam?> = _selectedExam.asStateFlow()
    private val _selectedCluster = MutableStateFlow<String?>(null)
    val selectedCluster: StateFlow<String?> = _selectedCluster.asStateFlow()

    private val _availableExams = MutableStateFlow<List<Exam>>(emptyList())
    val availableExams: StateFlow<List<Exam>> = _availableExams.asStateFlow()

    private val _randomQuestionToVerify = MutableStateFlow<Question?>(null)
    val randomQuestionToVerify: StateFlow<Question?> = _randomQuestionToVerify.asStateFlow()

    init {
        loadAvailableExams()
    }

    private fun loadAvailableExams() {
        viewModelScope.launch {
            // Loading all exams across all subjects for scanning selection
            // In a real app, you might filter by recent or currently active program
            examRepository.getAllExams().collect { exams ->
                _availableExams.value = exams
            }
        }
    }

    fun selectExam(exam: Exam?) {
        _selectedExam.value = exam
        if (exam != null) {
            pickRandomQuestionToVerify(exam)
        } else {
            _randomQuestionToVerify.value = null
        }
    }

    fun selectCluster(cluster: String?) {
        _selectedCluster.value = cluster
        _selectedExam.value = null
        _randomQuestionToVerify.value = null
    }

    fun pickRandomQuestionToVerify(exam: Exam) {
        viewModelScope.launch {
            try {
                // Pick random question from the generated single set.
                val questionIds = exam.questionIds
                if (questionIds.isNotEmpty()) {
                    val randomId = questionIds.random(Random(System.currentTimeMillis()))
                    val allQuestions = questionRepository.getAllQuestionsForSubjectOnce(exam.subject)
                    _randomQuestionToVerify.value = allQuestions.find { it.id == randomId }
                }
            } catch (e: Exception) {
                Log.e("ScanViewModel", "Error picking random question", e)
            }
        }
    }

    private val _isTopLeftDetected = MutableStateFlow(false)
    val isTopLeftDetected: StateFlow<Boolean> = _isTopLeftDetected.asStateFlow()

    private val _isTopRightDetected = MutableStateFlow(false)
    val isTopRightDetected: StateFlow<Boolean> = _isTopRightDetected.asStateFlow()

    private val _isBottomLeftDetected = MutableStateFlow(false)
    val isBottomLeftDetected: StateFlow<Boolean> = _isBottomLeftDetected.asStateFlow()

    private val _isBottomRightDetected = MutableStateFlow(false)
    val isBottomRightDetected: StateFlow<Boolean> = _isBottomRightDetected.asStateFlow()

    fun updateCornerDetection(corner: Corner, isDetected: Boolean) {
        when (corner) {
            Corner.TOP_LEFT -> _isTopLeftDetected.update { isDetected }
            Corner.TOP_RIGHT -> _isTopRightDetected.update { isDetected }
            Corner.BOTTOM_LEFT -> _isBottomLeftDetected.update { isDetected }
            Corner.BOTTOM_RIGHT -> _isBottomRightDetected.update { isDetected }
        }
    }

    fun resetAllDetection() {
        _isTopLeftDetected.update { false }
        _isTopRightDetected.update { false }
        _isBottomLeftDetected.update { false }
        _isBottomRightDetected.update { false }
    }

    val allFourCornersDetected: StateFlow<Boolean> = combine(
        _isTopLeftDetected,
        _isTopRightDetected,
        _isBottomLeftDetected,
        _isBottomRightDetected
    ) { tl, tr, bl, br ->
        tl && tr && bl && br
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
}
