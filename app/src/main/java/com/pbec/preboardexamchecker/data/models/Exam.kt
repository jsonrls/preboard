// app/src/main/java/com/pbec/preboardexamchecker/data/models/Exam.kt
package com.pbec.preboardexamchecker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String, // e.g., "Exam 1", "Exam 2"
    val subject: String,
    val setAQuestionIds: List<Long>, 
    val setBQuestionIds: List<Long>, // Actual questions are same as A but different order
    val createdAt: Long // Timestamp for sorting and naming
) {
    // Single-set view used by current UI; keeps backward compatibility with old data.
    val questionIds: List<Long>
        get() = if (setAQuestionIds.isNotEmpty()) setAQuestionIds else setBQuestionIds
}