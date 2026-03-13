package com.pbec.preboardexamchecker.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.data.models.Question
import com.pbec.preboardexamchecker.data.repository.ExamRepository
import com.pbec.preboardexamchecker.data.repository.QuestionRepository
import com.pbec.preboardexamchecker.utils.ExcelParser
import com.pbec.preboardexamchecker.utils.ExamBlueprint
import com.pbec.preboardexamchecker.utils.ExamBlueprints
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val questionRepository: QuestionRepository,
    private val firestore: FirebaseFirestore,
    private val excelParser: ExcelParser,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val logTag = "ExamViewModel"

    val subject: String = savedStateHandle.get<String>("subject") ?: "Unknown"

    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            examRepository.getExamsBySubject(subject).collect {
                _exams.value = it
            }
        }
    }

    fun generateExam(numQuestions: Int, selectedImportSessionIds: List<Long>) {
        viewModelScope.launch {
            try {
                val allQuestions = if (selectedImportSessionIds.isEmpty()) {
                    emptyList()
                } else {
                    questionRepository.getQuestionsByImportSessionIdsOnly(selectedImportSessionIds)
                }
                
                if (allQuestions.isEmpty()) {
                    _message.value = "No questions available in the selected question banks."
                    return@launch
                }

                val blueprint = ExamBlueprints.getBlueprint(subject)
                val useBlueprint = blueprint != null && numQuestions == 100

                var selectedQuestions = if (useBlueprint) {
                    generateFromBlueprint(allQuestions, blueprint!!)
                } else {
                    emptyList()
                }

                var usedRandomFallback = false
                if (selectedQuestions.isEmpty() && numQuestions > 0) {
                    selectedQuestions = allQuestions.shuffled().take(minOf(numQuestions, allQuestions.size))
                    usedRandomFallback = useBlueprint
                } else if (useBlueprint && selectedQuestions.size < 100) {
                    val missing = 100 - selectedQuestions.size
                    val remainingPool = allQuestions.filter { q -> selectedQuestions.none { it.id == q.id } }
                    selectedQuestions = selectedQuestions + remainingPool.shuffled().take(minOf(missing, remainingPool.size))
                    usedRandomFallback = true
                }

                val examName = "Exam ${exams.value.size + 1} (${subject})" 
                val newExam = Exam(
                    examName = examName,
                    subject = subject,
                    setAQuestionIds = selectedQuestions.shuffled().map { it.id },
                    setBQuestionIds = emptyList(),
                    createdAt = Date().time 
                )

                examRepository.insertExam(newExam)
                syncGeneratedExamToFirestore(
                    exam = newExam,
                    selectedImportSessionIds = selectedImportSessionIds,
                    generatedQuestionCount = selectedQuestions.size,
                    usedBlueprint = useBlueprint,
                    usedRandomFallback = usedRandomFallback
                )
                
                val sourceInfo = if (useBlueprint && !usedRandomFallback) {
                    "using the official blueprint"
                } else if (useBlueprint && usedRandomFallback) {
                    "with partial blueprint matching (some questions were added randomly from the selected banks)"
                } else {
                    "randomly from the selected question banks"
                }
                
                _message.value = "Exam '$examName' generated successfully with ${selectedQuestions.size} questions $sourceInfo!"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    private fun generateFromBlueprint(allQuestions: List<Question>, blueprint: ExamBlueprint): List<Question> {
        val selected = mutableListOf<Question>()
        val pool = allQuestions.toMutableList()

        for (spec in blueprint.specs) {
            val topicQuestions = pool.filter { question ->
                val qTopic = question.topic?.trim()?.lowercase(Locale.ROOT) ?: ""
                val specTopic = spec.topic.lowercase(Locale.ROOT)
                
                // Aggressive Matching:
                // 1. Check if the question topic contains important keywords from the spec title or its aliases
                val specKeywords = specTopic.split(" ", ",", "&").filter { it.length > 3 }
                val aliasKeywords = spec.aliases.flatMap { it.lowercase(Locale.ROOT).split(" ", ",", "&") }.filter { it.length > 3 }
                val allKeywords = (specKeywords + aliasKeywords).distinct()
                
                val isMatch = qTopic == specTopic || 
                             qTopic.contains(specTopic) || 
                             specTopic.contains(qTopic) ||
                             spec.aliases.any { qTopic.contains(it.lowercase(Locale.ROOT)) } ||
                             (allKeywords.isNotEmpty() && allKeywords.any { qTopic.contains(it) })
                
                isMatch
            }

            if (topicQuestions.isEmpty()) continue

            // Logic to pick from topicQuestions
            val objectives = topicQuestions.filter { it.category?.trim()?.contains("Objective", true) == true }.shuffled().toMutableList()
            val computations = topicQuestions.filter { it.category?.trim()?.contains("Computation", true) == true }.shuffled().toMutableList()

            val chosenObjs = objectives.take(spec.numObjective)
            objectives.removeAll(chosenObjs)
            selected.addAll(chosenObjs)

            val chosenComps = computations.take(spec.numComputation)
            computations.removeAll(chosenComps)
            selected.addAll(chosenComps)

            // Fill remaining needed for this topic from whatever is left in THIS topic's pool
            val currentTopicCount = selected.count { q -> topicQuestions.any { it.id == q.id } }
            val stillNeeded = spec.total - currentTopicCount
            if (stillNeeded > 0) {
                val remainingInTopic = (objectives + computations).shuffled()
                selected.addAll(remainingInTopic.take(stillNeeded))
            }
            
            pool.removeAll { q -> selected.any { it.id == q.id } }
        }
        
        return selected
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            try {
                examRepository.deleteExam(exam)
            } catch (e: Exception) {
                _message.value = "Error deleting exam: ${e.message}"
            }
        }
    }

    fun renameExam(exam: Exam, newName: String) {
        if (newName.isBlank()) {
            _message.value = "Exam name cannot be empty."
            return
        }
        viewModelScope.launch {
            try {
                val updatedExam = exam.copy(examName = newName)
                examRepository.updateExam(updatedExam)
            } catch (e: Exception) {
                _message.value = "Error renaming exam: ${e.message}"
            }
        }
    }

    fun importExcelFile(uri: Uri, selectedSubject: String, fileName: String) {
        viewModelScope.launch {
            try {
                val questions = excelParser.readQuestionsFromExcel(context, uri, selectedSubject, fileName)
                if (questions.isNotEmpty()) {
                    questionRepository.insertQuestions(questions)
                    _message.value = "Successfully imported ${questions.size} questions."
                } else {
                    _message.value = "No valid questions found."
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun syncGeneratedExamToFirestore(
        exam: Exam,
        selectedImportSessionIds: List<Long>,
        generatedQuestionCount: Int,
        usedBlueprint: Boolean,
        usedRandomFallback: Boolean
    ) {
        ensureFirebaseUser { uid ->
            firestore.collection("exams")
                .add(
                    mapOf(
                        "examName" to exam.examName,
                        "subject" to exam.subject,
                        "questionIds" to exam.questionIds,
                        "createdAt" to exam.createdAt,
                        "selectedImportSessionIds" to selectedImportSessionIds,
                        "generatedQuestionCount" to generatedQuestionCount,
                        "usedBlueprint" to usedBlueprint,
                        "usedRandomFallback" to usedRandomFallback,
                        "uploadedByUid" to uid,
                        "syncedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .addOnSuccessListener {
                    Log.d(logTag, "Synced generated exam '${exam.examName}' to Firestore.")
                }
                .addOnFailureListener { error ->
                    Log.e(logTag, "Firestore exam sync failed", error)
                    _message.value = "Exam generated locally, but Firebase exam sync failed: ${error.message}"
                }
        }
    }

    private fun ensureFirebaseUser(onReady: (String) -> Unit) {
        val existingUser = firebaseAuth.currentUser
        if (existingUser != null) {
            onReady(existingUser.uid)
            return
        }

        firebaseAuth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) {
                    onReady(uid)
                } else {
                    _message.value = "Firebase sign-in failed: missing user."
                }
            }
            .addOnFailureListener { error ->
                Log.e(logTag, "Anonymous Firebase sign-in failed", error)
                _message.value = "Firebase sign-in failed: ${error.message}"
            }
    }
}
