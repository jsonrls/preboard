package com.pbec.preboardexamchecker.data.repository

import com.pbec.preboardexamchecker.data.dao.TransactionLogDao
import com.pbec.preboardexamchecker.data.models.TransactionLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionLogRepository @Inject constructor(
    private val transactionLogDao: TransactionLogDao
) {
    suspend fun insertTransaction(action: String, subject: String?, details: String) {
        transactionLogDao.insertTransaction(
            TransactionLog(
                action = action,
                subject = subject,
                details = details
            )
        )
    }

    fun getAllTransactions(): Flow<List<TransactionLog>> {
        return transactionLogDao.getAllTransactions()
    }
}
