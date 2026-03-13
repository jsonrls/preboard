package com.pbec.preboardexamchecker.data.repository

import com.pbec.preboardexamchecker.data.dao.ExamDao
import com.pbec.preboardexamchecker.data.models.Exam
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val examDao: ExamDao
) {
    suspend fun insertExam(exam: Exam): Long {
        return examDao.insertExam(exam)
    }

    fun getAllExams(): Flow<List<Exam>> {
        return examDao.getAllExams()
    }

    fun getExamsBySubject(subject: String): Flow<List<Exam>> {
        return examDao.getExamsBySubject(subject)
    }

    suspend fun getExamsBySubjectOnce(subject: String): List<Exam> {
        return examDao.getExamsBySubjectOnce(subject)
    }

    suspend fun getExamById(examId: Long): Exam? {
        return examDao.getExamById(examId)
    }

    suspend fun deleteExam(exam: Exam): Int {
        return examDao.deleteExam(exam)
    }

    suspend fun deleteExams(exams: List<Exam>): Int {
        if (exams.isEmpty()) return 0
        return examDao.deleteExams(exams)
    }

    suspend fun updateExam(exam: Exam): Int {
        return examDao.updateExam(exam)
    }
}