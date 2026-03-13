package com.pbec.preboardexamchecker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.pbec.preboardexamchecker.data.dao.ExamDao
import com.pbec.preboardexamchecker.data.dao.QuestionDao
import com.pbec.preboardexamchecker.data.dao.TransactionLogDao
import com.pbec.preboardexamchecker.data.models.Exam
import com.pbec.preboardexamchecker.data.models.ListLongConverter
import com.pbec.preboardexamchecker.data.models.Question
import com.pbec.preboardexamchecker.data.models.TransactionLog

@Database(entities = [Question::class, Exam::class, TransactionLog::class], version = 7, exportSchema = true)
@TypeConverters(ListLongConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun examDao(): ExamDao
    abstract fun transactionLogDao(): TransactionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, gson: Gson): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pre_board_exam_checker_db"
                )
                    .addTypeConverter(ListLongConverter(gson))
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
