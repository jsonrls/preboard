package com.pbec.preboardexamchecker.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log // Import Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.data.models.Question
import com.pbec.preboardexamchecker.data.repository.ExamRepository
import com.pbec.preboardexamchecker.data.repository.QuestionRepository
import com.pbec.preboardexamchecker.utils.PdfExportUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamContentViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository,
    private val pdfExportUtil: PdfExportUtil,
    @ApplicationContext private val applicationContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val examId: Long = savedStateHandle.get<String>("examId")?.toLongOrNull() ?: -1L

    private val _exam = MutableStateFlow<Exam?>(null)
    val exam: StateFlow<Exam?> = _exam.asStateFlow()

    // FIX: Corrected type from List<List<Question>> to List<Question>
    private val _questionsInSet = MutableStateFlow<List<Question>>(emptyList())
    // FIX: Corrected type from List<List<Question>> to List<Question>
    val questionsInSet: StateFlow<List<Question>> = _questionsInSet.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadExamContent()
    }

    private fun loadExamContent() {
        viewModelScope.launch {
            try {
                val currentExam = examRepository.getExamById(examId)
                _exam.value = currentExam

                currentExam?.let { exam ->
                    val questionIdsToLoad = exam.questionIds

                    if (questionIdsToLoad.isEmpty()) {
                        _message.value = "No questions found in this exam."
                        return@launch
                    }

                    val allSubjectQuestions = questionRepository.getAllQuestionsForSubjectOnce(exam.subject)

                    val questions = mutableListOf<Question>()
                    for (id in questionIdsToLoad) {
                        val question = allSubjectQuestions.firstOrNull { it.id == id }
                        if (question != null) {
                            questions.add(question)
                        }
                    }
                    // FIX: Ensure the assigned value matches the StateFlow type
                    _questionsInSet.value = questions
                }
            } catch (e: Exception) {
                _message.value = "Error loading exam content: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun exportExamContentToPdf(outputUri: Uri) {
        viewModelScope.launch {
            val currentExam = _exam.value
            val currentQuestions = _questionsInSet.value

            if (currentExam == null || currentQuestions.isEmpty()) {
                _message.value = "No exam content to export."
                return@launch
            }

            currentQuestions.forEachIndexed { index, question ->
                Log.d("ExamContentViewModel", "Question ${index + 1}: ${question.questionText}")
                Log.d("ExamContentViewModel", "Options: A=${question.optionA}, B=${question.optionB}, C=${question.optionC}, D=${question.optionD}")
            }

            _message.value = "Exporting PDF... Please wait."
            try {
                pdfExportUtil.exportExamToPdf(outputUri, currentExam.examName, currentExam.subject, currentQuestions)
                _message.value = "PDF exported successfully!"
            } catch (e: Exception) {
                _message.value = "Error exporting PDF: ${e.message}"
                Log.e("ExamContentViewModel", "Error exporting PDF", e)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}