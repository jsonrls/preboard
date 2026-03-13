package com.pbec.preboardexamchecker.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pbec.preboardexamchecker.data.models.Exam
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    suspend fun insertExam(
        exam: Exam,
        selectedImportSessionIds: List<Long> = emptyList(),
        selectedQuestionBankIds: List<String> = emptyList(),
        generatedQuestionCount: Int? = null,
        usedBlueprint: Boolean? = null,
        usedRandomFallback: Boolean? = null
    ): Long {
        val uid = ensureFirebaseUserUid()
        val teacherId = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("teacher_id", null)
            ?: uid
        val normalizedExam = if (exam.id == 0L) exam.copy(id = generateId()) else exam
        val existingDocs = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("id", normalizedExam.id)
            .get()
            .await()

        val targetDoc = existingDocs.documents.firstOrNull()?.reference
            ?: firestore.collection("exams").document()

        val payload = mutableMapOf<String, Any>(
            "id" to normalizedExam.id,
            "examName" to normalizedExam.examName,
            "subject" to normalizedExam.subject,
            "setAQuestionIds" to normalizedExam.setAQuestionIds,
            "setBQuestionIds" to normalizedExam.setBQuestionIds,
            "questionIds" to normalizedExam.questionIds,
            "createdAt" to normalizedExam.createdAt,
            "uploadedByUid" to uid,
            "uploadedByTeacherId" to teacherId,
            "syncedAt" to com.google.firebase.Timestamp.now()
        )
        if (selectedImportSessionIds.isNotEmpty()) payload["selectedImportSessionIds"] = selectedImportSessionIds
        if (selectedQuestionBankIds.isNotEmpty()) payload["selectedQuestionBankIds"] = selectedQuestionBankIds
        if (generatedQuestionCount != null) payload["generatedQuestionCount"] = generatedQuestionCount
        if (usedBlueprint != null) payload["usedBlueprint"] = usedBlueprint
        if (usedRandomFallback != null) payload["usedRandomFallback"] = usedRandomFallback

        targetDoc.set(payload).await()

        return normalizedExam.id
    }

    fun getAllExams(): Flow<List<Exam>> {
        return callbackFlow {
            val uid = runCatching { ensureFirebaseUserUid() }.getOrElse {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }

            val listener = firestore.collection("exams")
                .whereEqualTo("uploadedByUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val exams = snapshot?.documents
                        ?.mapNotNull { it.toExam() }
                        ?.sortedByDescending { it.createdAt }
                        .orEmpty()
                    trySend(exams)
                }
            awaitClose { listener.remove() }
        }
    }

    fun getExamsBySubject(subject: String): Flow<List<Exam>> {
        return callbackFlow {
            val uid = runCatching { ensureFirebaseUserUid() }.getOrElse {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }

            val listener = firestore.collection("exams")
                .whereEqualTo("uploadedByUid", uid)
                .whereEqualTo("subject", subject)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val exams = snapshot?.documents
                        ?.mapNotNull { it.toExam() }
                        ?.sortedByDescending { it.createdAt }
                        .orEmpty()
                    trySend(exams)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun getExamsBySubjectOnce(subject: String): List<Exam> {
        val uid = ensureFirebaseUserUid()
        val snapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("subject", subject)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toExam() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun getExamById(examId: Long): Exam? {
        val uid = ensureFirebaseUserUid()
        val snapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("id", examId)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toExam()
    }

    suspend fun deleteExam(exam: Exam): Int {
        val uid = ensureFirebaseUserUid()
        val byIdSnapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("id", exam.id)
            .get()
            .await()
        if (!byIdSnapshot.isEmpty) {
            byIdSnapshot.documents.forEach { it.reference.delete().await() }
            return byIdSnapshot.size()
        }

        val fallbackSnapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("subject", exam.subject)
            .whereEqualTo("examName", exam.examName)
            .whereEqualTo("createdAt", exam.createdAt)
            .get()
            .await()
        fallbackSnapshot.documents.forEach { it.reference.delete().await() }
        return fallbackSnapshot.size()
    }

    suspend fun deleteExams(exams: List<Exam>): Int {
        if (exams.isEmpty()) return 0
        var deleted = 0
        exams.forEach { exam ->
            deleted += deleteExam(exam)
        }
        return deleted
    }

    suspend fun updateExam(exam: Exam): Int {
        insertExam(exam)
        return 1
    }

    suspend fun deleteExamsLinkedToImportSession(
        subject: String,
        importSessionId: Long,
        questionIdsInSession: Set<Long>
    ): Int {
        val uid = ensureFirebaseUserUid()
        val snapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("subject", subject)
            .get()
            .await()

        val docsToDelete = snapshot.documents.filter { doc ->
            val examQuestionIds = extractQuestionIds(doc).toSet()
            val linkedByQuestions = questionIdsInSession.isNotEmpty() && examQuestionIds.any(questionIdsInSession::contains)
            val selectedImportSessionIds = (doc.get("selectedImportSessionIds") as? List<*>)?.mapNotNull { value ->
                when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull()
                    else -> null
                }
            }.orEmpty()
            val linkedBySessionMetadata = selectedImportSessionIds.contains(importSessionId)
            linkedByQuestions || linkedBySessionMetadata
        }

        docsToDelete.forEach { it.reference.delete().await() }
        return docsToDelete.size
    }

    suspend fun deleteExamsLinkedToQuestionBank(
        subject: String,
        questionBankId: String,
        questionIdsInBank: Set<Long>
    ): Int {
        val uid = ensureFirebaseUserUid()
        val snapshot = firestore.collection("exams")
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("subject", subject)
            .get()
            .await()

        val docsToDelete = snapshot.documents.filter { doc ->
            val examQuestionIds = extractQuestionIds(doc).toSet()
            val linkedByQuestions = questionIdsInBank.isNotEmpty() && examQuestionIds.any(questionIdsInBank::contains)
            val selectedQuestionBankIds = (doc.get("selectedQuestionBankIds") as? List<*>)?.mapNotNull { value ->
                value as? String
            }.orEmpty()
            val linkedByBankMetadata = selectedQuestionBankIds.contains(questionBankId)
            linkedByQuestions || linkedByBankMetadata
        }

        docsToDelete.forEach { it.reference.delete().await() }
        return docsToDelete.size
    }

    private suspend fun ensureFirebaseUserUid(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.uid?.let { return it }
        val authResult = auth.signInAnonymously().await()
        return authResult.user?.uid ?: throw IllegalStateException("Firebase sign-in failed: missing user.")
    }

    private fun generateId(): Long {
        return System.currentTimeMillis() + (0..9999).random()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toExam(): Exam? {
        val examName = getString("examName") ?: return null
        val subject = getString("subject") ?: return null
        val setA = extractQuestionIds(this)
        val setB = (get("setBQuestionIds") as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
        val fallbackId = id.hashCode().toLong().let { if (it < 0) -it else it }
        val normalizedId = getLong("id") ?: fallbackId
        return Exam(
            id = normalizedId,
            examName = examName,
            subject = subject,
            setAQuestionIds = setA,
            setBQuestionIds = setB,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun extractQuestionIds(document: com.google.firebase.firestore.DocumentSnapshot): List<Long> {
        val setA = (document.get("setAQuestionIds") as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }.orEmpty()
        if (setA.isNotEmpty()) return setA
        return (document.get("questionIds") as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }.orEmpty()
    }
}