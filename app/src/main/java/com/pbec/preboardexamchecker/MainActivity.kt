package com.pbec.preboardexamchecker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pbec.preboardexamchecker.data.repository.ExamRepository
import com.pbec.preboardexamchecker.ui.Screen
import com.pbec.preboardexamchecker.ui.account.AccountScreen
import com.pbec.preboardexamchecker.ui.account.SecurityScreen
import com.pbec.preboardexamchecker.ui.auth.LoginScreen
import com.pbec.preboardexamchecker.ui.exams.ExamContentScreen
import com.pbec.preboardexamchecker.ui.exams.ExamDetailsScreen
import com.pbec.preboardexamchecker.ui.exams.ExamScreen
import com.pbec.preboardexamchecker.ui.exambank.ExamBankScreen
import com.pbec.preboardexamchecker.ui.exambank.ImportSessionDetailsScreen
import com.pbec.preboardexamchecker.ui.onboarding.OnboardingScreen
import com.pbec.preboardexamchecker.ui.scan.ScanScreen
import com.pbec.preboardexamchecker.ui.programs.ProgramsScreen
import com.pbec.preboardexamchecker.ui.students.StudentsScreen
import com.pbec.preboardexamchecker.ui.subjects.SubjectsScreen
import com.pbec.preboardexamchecker.ui.summary.SummaryScreen
import com.pbec.preboardexamchecker.ui.theme.PreBoardExamCheckerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var examRepository: ExamRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreBoardExamCheckerTheme {
                App(this, examRepository)
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun App(context: Context, examRepository: ExamRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val onboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)

    var isLoggedIn by remember {
        mutableStateOf(sharedPreferences.getString("teacher_id", null)?.isNotBlank() == true)
    }

    val startDestination = if (isLoggedIn && !onboardingCompleted) "onboarding_pager" 
                          else if (isLoggedIn) Screen.Programs.route 
                          else Screen.Login.route

    // Show navbar on all screens except Login and Onboarding
    val showMainNavigationBar = currentRoute != null && 
                                currentRoute != Screen.Login.route && 
                                currentRoute != "onboarding_pager"

    Scaffold(
        bottomBar = {
            if (showMainNavigationBar) {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                ) {
                    val items = listOf(
                        Screen.Programs,
                        Screen.Students,
                        Screen.Capture,
                        Screen.Summary,
                        Screen.Account
                    )
                    
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        
                        if (screen == Screen.Capture) {
                            NavigationBarItem(
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Capture",
                                            modifier = Modifier.size(24.dp),
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                },
                                label = { Text("Capture", fontSize = 10.sp) },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        } else {
                            NavigationBarItem(
                                icon = { screen.icon() },
                                label = { Text(screen.title, fontSize = 10.sp) },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding_pager") {
                OnboardingScreen(navController = navController)
            }
            composable(Screen.Login.route) {
                isLoggedIn = sharedPreferences.getString("teacher_id", null)?.isNotBlank() == true
                LoginScreen(navController = navController, onLogin = { 
                    isLoggedIn = true
                    if (!onboardingCompleted) {
                        navController.navigate("onboarding_pager") { 
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Programs.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                })
            }
            composable(Screen.Programs.route) {
                ProgramsScreen(navController = navController)
            }
            composable(Screen.Students.route) {
                StudentsScreen()
            }
            composable(Screen.Capture.route) {
                ScanScreen(navController = navController)
            }
            composable(Screen.Summary.route) {
                SummaryScreen()
            }
            composable(Screen.Account.route) {
                AccountScreen(navController = navController)
            }
            composable(Screen.Security.route) {
                SecurityScreen(navController = navController)
            }
            composable(Screen.Subjects.route) { SubjectsScreen(navController) }
            composable(Screen.Exams.route) { backStackEntry ->
                val subject = backStackEntry.arguments?.getString("subject") ?: "Unknown"
                ExamScreen(navController, subject)
            }
            composable(Screen.ExamDetails.route) { backStackEntry ->
                val examId = backStackEntry.arguments?.getString("examId")?.toLongOrNull() ?: 0L
                ExamDetailsScreen(navController, examId, examRepository)
            }
            composable(Screen.ExamContent.route) {
                ExamContentScreen(navController)
            }
            composable(Screen.ExamBank.route) { backStackEntry ->
                val subject = backStackEntry.arguments?.getString("subject") ?: "Unknown"
                ExamBankScreen(navController, subject)
            }
            composable(Screen.ImportSessionDetails.route) { backStackEntry ->
                val subject = backStackEntry.arguments?.getString("subject") ?: "Unknown"
                val importSessionId = backStackEntry.arguments?.getString("importSessionId")?.toLongOrNull() ?: 0L
                ImportSessionDetailsScreen(navController, subject, importSessionId)
            }
        }
    }
}