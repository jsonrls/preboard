package com.pbec.preboardexamchecker.ui.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pbec.preboardexamchecker.ui.Screen

@Composable
fun SubjectsScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate(Screen.Exams.createRoute("Mathematics")) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Mathematics")
        }
        Button(
            onClick = { navController.navigate(Screen.Exams.createRoute("ESAS")) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("ESAS")
        }
        Button(
            onClick = { navController.navigate(Screen.Exams.createRoute("Professional EE")) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Professional EE")
        }
    }
}
