package com.pbec.preboardexamchecker.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Student(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val studentId: String = "",
    val program: String = "",
    val yearLevel: String = "",
    val section: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
