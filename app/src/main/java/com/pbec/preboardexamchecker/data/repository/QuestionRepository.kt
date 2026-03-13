package com.pbec.preboardexamchecker.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pbec.preboardexamchecker.data.models.Question
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val QUESTION_BANKS_COLLECTION = "question_banks"
        private const val QUESTIONS_SUBCOLLECTION = "questions"
    }

    suspend fun insertQuestions(questions: List<Question>) {
        if (questions.isEmpty()) return
        val uid = ensureFirebaseUserUid()
        val teacherId = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("teacher_id", null)
            ?: uid

        questions.groupBy { it.questionBankId }.forEach { (bankId, groupedQuestions) ->
            groupedQuestions.forEach { question ->
                val normalizedQuestion = if (question.id == 0L) {
                    question.copy(id = generateId())
                } else {
                    question
                }

                firestore.collection(QUESTION_BANKS_COLLECTION)
                    .document(bankId)
                    .collection(QUESTIONS_SUBCOLLECTION)
                    .document(normalizedQuestion.id.toString())
                    .set(
                        mapOf(
                            "id" to normalizedQuestion.id,
                            "subject" to normalizedQuestion.subject,
                            "fileName" to normalizedQuestion.fileName,
                            "category" to normalizedQuestion.category,
                            "topic" to normalizedQuestion.topic,
                            "questionNumber" to normalizedQuestion.questionNumber,
                            "questionText" to normalizedQuestion.questionText,
                            "optionA" to normalizedQuestion.optionA,
                            "optionB" to normalizedQuestion.optionB,
                            "optionC" to normalizedQuestion.optionC,
                            "optionD" to normalizedQuestion.optionD,
                            "correctAnswer" to normalizedQuestion.correctAnswer,
                            "questionBankId" to normalizedQuestion.questionBankId,
                            "importSessionId" to normalizedQuestion.importSessionId,
                            "customSessionName" to normalizedQuestion.customSessionName,
                            "sourceFileName" to normalizedQuestion.fileName,
                            "uploadedByUid" to uid,
                            "uploadedByTeacherId" to teacherId,
                            "syncedAt" to com.google.firebase.Timestamp.now()
                        )
                )
                    .await()
            }

            val refreshedCount = firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .collection(QUESTIONS_SUBCOLLECTION)
                .get()
                .await()
                .size()

            val representative = groupedQuestions.first()
            firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .set(
                    mapOf(
                        "questionBankId" to bankId,
                        "subject" to representative.subject,
                        "sourceFileName" to representative.fileName,
                        "displayName" to (representative.customSessionName ?: representative.fileName),
                        "questionCount" to refreshedCount,
                        "uploadedByUid" to uid,
                        "uploadedByTeacherId" to teacherId,
                        "legacyImportSessionId" to representative.importSessionId,
                        "updatedAt" to com.google.firebase.Timestamp.now(),
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        }
    }

    fun getQuestionsBySubject(subject: String): Flow<List<Question>> {
        return callbackFlow {
            val uid = runCatching { ensureFirebaseUserUid() }.getOrElse {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }

            val listener = firestore.collection(QUESTION_BANKS_COLLECTION)
                .whereEqualTo("uploadedByUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val bankIds = snapshot?.documents
                        ?.filter { it.getString("subject") == subject }
                        ?.map { it.id }
                        .orEmpty()
                    launch {
                        val questions = loadQuestionsFromBanks(uid, bankIds)
                            .filter { it.subject == subject }
                            .sortedWith(compareBy<Question> { it.questionBankId }.thenBy { it.questionNumber })
                        trySend(questions)
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    suspend fun getAllQuestionsForSubjectOnce(subject: String): List<Question> {
        val uid = ensureFirebaseUserUid()
        val bankSnapshot = firestore.collection(QUESTION_BANKS_COLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .whereEqualTo("subject", subject)
            .get()
            .await()
        val bankIds = bankSnapshot.documents.map { it.id }
        return loadQuestionsFromBanks(uid, bankIds)
            .filter { it.subject == subject }
            .sortedWith(compareBy<Question> { it.questionBankId }.thenBy { it.questionNumber })
    }

    suspend fun deleteQuestion(question: Question): Int {
        val uid = ensureFirebaseUserUid()
        val directDocRef = firestore.collection(QUESTION_BANKS_COLLECTION)
            .document(question.questionBankId)
            .collection(QUESTIONS_SUBCOLLECTION)
            .document(question.id.toString())
        val directDoc = directDocRef.get().await()
        if (directDoc.exists()) {
            directDocRef.delete().await()
            refreshQuestionBankCount(uid, question.questionBankId)
            return 1
        }

        // Fallback for legacy/mismatched bank IDs: scan user's banks directly (no collection-group index required).
        val bankIds = getUserBankIds(uid)
        var deleted = 0
        bankIds.forEach { bankId ->
            val docRef = firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .collection(QUESTIONS_SUBCOLLECTION)
                .document(question.id.toString())
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.delete().await()
                refreshQuestionBankCount(uid, bankId)
                deleted++
            }
        }
        return deleted
    }

    suspend fun deleteQuestionsByImportSessionId(importSessionId: Long): Int {
        val uid = ensureFirebaseUserUid()
        var deletedCount = 0
        getUserBankIds(uid).forEach { bankId ->
            val docs = firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .collection(QUESTIONS_SUBCOLLECTION)
                .whereEqualTo("uploadedByUid", uid)
                .get()
                .await()
                .documents
            val matchingDocs = docs.filter { doc ->
                doc.getLong("importSessionId") == importSessionId ||
                    doc.getString("importSessionId")?.toLongOrNull() == importSessionId
            }
            matchingDocs.forEach { it.reference.delete().await() }
            if (matchingDocs.isNotEmpty()) {
                refreshQuestionBankCount(uid, bankId)
            }
            deletedCount += matchingDocs.size
        }
        return deletedCount
    }

    suspend fun deleteQuestionsByQuestionBankId(questionBankId: String): Int {
        val uid = ensureFirebaseUserUid()
        val bankDocs = firestore.collection(QUESTION_BANKS_COLLECTION)
            .document(questionBankId)
            .collection(QUESTIONS_SUBCOLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
            .documents
        if (bankDocs.isNotEmpty()) {
            bankDocs.forEach { it.reference.delete().await() }
            firestore.collection(QUESTION_BANKS_COLLECTION).document(questionBankId).delete().await()
            return bankDocs.size
        }

        // Legacy fallback: locate matching docs by metadata across user's banks.
        var deletedCount = 0
        getUserBankIds(uid).forEach { bankId ->
            val docs = firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .collection(QUESTIONS_SUBCOLLECTION)
                .whereEqualTo("uploadedByUid", uid)
                .get()
                .await()
                .documents
            val matchingDocs = docs.filter { doc ->
                val docBankId = doc.getString("questionBankId")
                val matchesBankId = docBankId == questionBankId
                val legacyImportSession = doc.getLong("importSessionId")
                    ?: doc.getString("importSessionId")?.toLongOrNull()
                val matchesLegacyBank = questionBankId.startsWith("legacy_") &&
                    legacyImportSession?.let { "legacy_$it" == questionBankId } == true
                matchesBankId || matchesLegacyBank
            }
            matchingDocs.forEach { it.reference.delete().await() }
            if (matchingDocs.isNotEmpty()) {
                refreshQuestionBankCount(uid, bankId)
            }
            deletedCount += matchingDocs.size
        }
        return deletedCount
    }

    /**
     * Fetches all questions for a given subject filtered by a list of import session IDs.
     * @param subject The subject to filter questions by.
     * @param importSessionIds List of import session IDs to filter questions by.
     * @return List of questions matching the subject and import session IDs.
     */
    suspend fun getQuestionsByImportSessionIds(subject: String, importSessionIds: List<Long>): List<Question> {
        if (importSessionIds.isEmpty()) return emptyList()
        return getAllQuestionsForSubjectOnce(subject)
            .filter { importSessionIds.contains(it.importSessionId) }
    }

    suspend fun getQuestionsByImportSessionIdsOnly(importSessionIds: List<Long>): List<Question> {
        if (importSessionIds.isEmpty()) return emptyList()
        val uid = ensureFirebaseUserUid()
        val docs = firestore.collectionGroup(QUESTIONS_SUBCOLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
        return docs.documents
            .mapNotNull { it.toQuestion() }
            .filter { importSessionIds.contains(it.importSessionId) }
            .sortedWith(compareBy<Question> { it.questionBankId }.thenBy { it.questionNumber })
    }

    suspend fun getQuestionsByQuestionBankIdsOnly(questionBankIds: List<String>): List<Question> {
        if (questionBankIds.isEmpty()) return emptyList()
        val uid = ensureFirebaseUserUid()
        return loadQuestionsFromBanks(uid, questionBankIds)
            .filter { questionBankIds.contains(it.questionBankId) }
            .sortedWith(compareBy<Question> { it.questionBankId }.thenBy { it.questionNumber })
    }

    suspend fun updateCustomSessionName(importSessionId: Long, newName: String): Int {
        val uid = ensureFirebaseUserUid()
        val docs = firestore.collectionGroup(QUESTIONS_SUBCOLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
        val matchingDocs = docs.documents.filter { doc ->
            doc.getLong("importSessionId") == importSessionId ||
                doc.getString("importSessionId")?.toLongOrNull() == importSessionId
        }
        matchingDocs.forEach { doc ->
            doc.reference.update("customSessionName", newName).await()
        }
        return matchingDocs.size
    }

    suspend fun updateCustomSessionNameByQuestionBankId(questionBankId: String, newName: String): Int {
        val uid = ensureFirebaseUserUid()
        val docs = firestore.collectionGroup(QUESTIONS_SUBCOLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
            .documents
        val matchingDocs = docs.filter { doc ->
            val docBankId = doc.getString("questionBankId")
            val matchesBankId = docBankId == questionBankId
            val legacyImportSession = doc.getLong("importSessionId")
                ?: doc.getString("importSessionId")?.toLongOrNull()
            val matchesLegacyBank = questionBankId.startsWith("legacy_") &&
                legacyImportSession?.let { "legacy_$it" == questionBankId } == true
            matchesBankId || matchesLegacyBank
        }
        matchingDocs.forEach { doc ->
            doc.reference.update("customSessionName", newName).await()
        }
        firestore.collection(QUESTION_BANKS_COLLECTION)
            .document(questionBankId)
            .set(
                mapOf(
                    "displayName" to newName,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
        return matchingDocs.size
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

    private suspend fun refreshQuestionBankCount(uid: String, questionBankId: String) {
        if (questionBankId.isBlank()) return
        val bankRef = firestore.collection(QUESTION_BANKS_COLLECTION).document(questionBankId)
        val countSnapshot = bankRef.collection(QUESTIONS_SUBCOLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
        val count = countSnapshot.size()
        if (count == 0) {
            bankRef.delete().await()
            return
        }
        bankRef.set(
            mapOf(
                "questionBankId" to questionBankId,
                "questionCount" to count,
                "uploadedByUid" to uid,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun loadQuestionsFromBanks(uid: String, bankIds: List<String>): List<Question> {
        if (bankIds.isEmpty()) return emptyList()
        val result = mutableListOf<Question>()
        bankIds.forEach { bankId ->
            val docs = firestore.collection(QUESTION_BANKS_COLLECTION)
                .document(bankId)
                .collection(QUESTIONS_SUBCOLLECTION)
                .whereEqualTo("uploadedByUid", uid)
                .get()
                .await()
                .documents
            result += docs.mapNotNull { it.toQuestion() }
        }
        return result
    }

    private suspend fun getUserBankIds(uid: String): List<String> {
        return firestore.collection(QUESTION_BANKS_COLLECTION)
            .whereEqualTo("uploadedByUid", uid)
            .get()
            .await()
            .documents
            .map { it.id }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toQuestion(): Question? {
        val subject = getString("subject") ?: return null
        val fileName = getString("fileName")
            ?: getString("sourceFileName")
            ?: "Imported Bank"
        val questionText = getString("questionText") ?: return null
        val optionA = getString("optionA").orEmpty()
        val optionB = getString("optionB").orEmpty()
        val optionC = getString("optionC").orEmpty()
        val optionD = getString("optionD").orEmpty()

        val fallbackId = id.hashCode().toLong().let { if (it < 0) -it else it }
        val normalizedId = getLong("id") ?: fallbackId

        val parsedImportSessionId = getLong("importSessionId")
            ?: getString("importSessionId")?.toLongOrNull()
            ?: 0L
        val sourceFileName = getString("sourceFileName")
        val parsedQuestionBankId = getString("questionBankId")
            ?: when {
                parsedImportSessionId != 0L -> "legacy_$parsedImportSessionId"
                !sourceFileName.isNullOrBlank() -> "legacy_file_${sourceFileName.trim().lowercase()}"
                else -> "manual"
            }

        return Question(
            id = normalizedId,
            subject = subject,
            fileName = fileName,
            category = getString("category"),
            topic = getString("topic"),
            questionNumber = getLong("questionNumber")?.toInt()
                ?: getDouble("questionNumber")?.toInt()
                ?: getString("questionNumber")?.toIntOrNull()
                ?: 0,
            questionText = questionText,
            optionA = optionA,
            optionB = optionB,
            optionC = optionC,
            optionD = optionD,
            correctAnswer = getString("correctAnswer"),
            questionBankId = parsedQuestionBankId,
            importSessionId = parsedImportSessionId,
            customSessionName = getString("customSessionName")
        )
    }
}