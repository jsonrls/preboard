package com.pbec.preboardexamchecker.data.repository

import com.pbec.preboardexamchecker.data.dao.QuestionDao
import com.pbec.preboardexamchecker.data.models.Question
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao
) {
    suspend fun insertQuestions(questions: List<Question>) {
        questionDao.insertQuestions(questions)
    }

    fun getQuestionsBySubject(subject: String): Flow<List<Question>> {
        return questionDao.getQuestionsBySubject(subject)
    }

    suspend fun getAllQuestionsForSubjectOnce(subject: String): List<Question> {
        return questionDao.getAllQuestionsForSubject(subject)
    }

    suspend fun deleteQuestion(question: Question): Int {
        return questionDao.deleteQuestion(question)
    }

    suspend fun deleteQuestionsByImportSessionId(importSessionId: Long): Int {
        return questionDao.deleteQuestionsByImportSessionId(importSessionId)
    }

    /**
     * Fetches all questions for a given subject filtered by a list of import session IDs.
     * @param subject The subject to filter questions by.
     * @param importSessionIds List of import session IDs to filter questions by.
     * @return List of questions matching the subject and import session IDs.
     */
    suspend fun getQuestionsByImportSessionIds(subject: String, importSessionIds: List<Long>): List<Question> {
        return questionDao.getQuestionsByImportSessionIds(subject, importSessionIds)
    }

    suspend fun getQuestionsByImportSessionIdsOnly(importSessionIds: List<Long>): List<Question> {
        return questionDao.getQuestionsByImportSessionIdsOnly(importSessionIds)
    }

    suspend fun updateCustomSessionName(importSessionId: Long, newName: String): Int {
        return questionDao.updateCustomSessionName(importSessionId, newName)
    }
}