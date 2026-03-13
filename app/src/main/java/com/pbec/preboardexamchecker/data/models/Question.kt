package com.pbec.preboardexamchecker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "questions", indices = [Index(value = ["subject", "fileName"]), Index(value = ["importSessionId"])])
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String, // "Mathematics", "ESAS", "Professional EE"
    val fileName: String, // e.g., "math001", "math001_1" for duplicates
    val category: String? = null, // e.g., "Computation", "Objective"
    val topic: String? = null, // e.g., "Algebra", "Calculus 1"
    val questionNumber: Int, // Original number in source file
    val questionText: String, 
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String?, // "A", "B", "C", "D", or null
    val importSessionId: Long = 0L,
    val customSessionName: String? = null // For renaming imported sessions
)
