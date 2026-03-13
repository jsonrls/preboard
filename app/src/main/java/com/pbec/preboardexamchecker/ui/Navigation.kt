package com.pbec.preboardexamchecker.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List 
import androidx.compose.material.icons.filled.AccountCircle 
import androidx.compose.material.icons.filled.Home 
import androidx.compose.material.icons.filled.Search 

 
import androidx.compose.material.icons.filled.Download 
import androidx.compose.material.icons.filled.Description 
import androidx.compose.material.icons.filled.UploadFile

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface Screen {
    val route: String
    val title: String
    val icon: @Composable () -> Unit

    data object Login : Screen {
        override val route = "login"
        override val title = "Login"
        override val icon: @Composable () -> Unit = {}  
    }

    data object Programs : Screen {
        override val route = "programs"
        override val title = "Home"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.Home, contentDescription = "Home", modifier = Modifier.size(24.dp))
        }
    }

    data object Subjects : Screen {
        override val route = "subjects"
        override val title = "Subjects"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Subjects", modifier = Modifier.size(24.dp))
        }
    }

    data object Exams : Screen {
        override val route = "exams/{subject}"
        override val title = "Exams"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.Description, contentDescription = "Exams", modifier = Modifier.size(24.dp))
        }
        fun createRoute(subject: String) = "exams/$subject"
    }

    data object ExamDetails : Screen {
        override val route = "examDetails/{examId}"
        override val title = "Exam Details"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.Description, contentDescription = "Exam Details", modifier = Modifier.size(24.dp))
        }
        fun createRoute(examId: Long) = "examDetails/$examId"
    }

    data object ExamContent : Screen {
        override val route = "examContent/{examId}"
        override val title = "Exam Content"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.Description, contentDescription = "Exam Content", modifier = Modifier.size(24.dp))
        }
        fun createRoute(examId: Long) = "examContent/$examId"
    }

    data object ExamBank : Screen {
        override val route = "examBank/{subject}"
        override val title = "Exam Bank"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.UploadFile, contentDescription = "Exam Bank", modifier = Modifier.size(24.dp))
        }
        fun createRoute(subject: String) = "examBank/$subject"
    }

    data object ImportSessionDetails : Screen {
        override val route = "importSessionDetails/{subject}/{questionBankId}"
        override val title = "Import Session"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.UploadFile, contentDescription = "Import Session", modifier = Modifier.size(24.dp))
        }
        fun createRoute(subject: String, questionBankId: String) = "importSessionDetails/$subject/$questionBankId"
    }

    data object Students : Screen {
        override val route = "students"
        override val title = "Students"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Students", modifier = Modifier.size(24.dp))
        }
    }

    data object Capture : Screen {  
        override val route = "capture"
        override val title = "Capture"
        override val icon: @Composable () -> Unit = {
            Icon(
                Icons.Filled.Search,  
                contentDescription = "Capture",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    data object Summary : Screen {
        override val route = "summary"
        override val title = "Summary"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Summary", modifier = Modifier.size(24.dp))
        }
    }

    data object Account : Screen {
        override val route = "account"
        override val title = "Account"
        override val icon: @Composable () -> Unit = {
            Icon(Icons.Filled.AccountCircle, contentDescription = "Account", modifier = Modifier.size(24.dp))
        }
    }

    data object Security : Screen {
        override val route = "security"
        override val title = "Security"
        override val icon: @Composable () -> Unit = {}
    }
}