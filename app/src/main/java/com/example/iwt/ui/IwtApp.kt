package com.example.iwt.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.iwt.ui.screens.HomeScreen
import com.example.iwt.ui.screens.SessionScreen
import com.example.iwt.ui.screens.SummaryScreen

enum class Routes { Home, Session, Summary }

@Composable
fun IwtApp() {
    val navController = rememberNavController()
    MaterialTheme {
        NavHost(navController = navController, startDestination = Routes.Home.name) {
            composable(Routes.Home.name) {
                HomeScreen(onStart = { navController.navigate(Routes.Session.name) })
            }
            composable(Routes.Session.name) {
                SessionScreen(
                    onFinish = { _ -> navController.navigate(Routes.Summary.name) },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.Summary.name) {
                SummaryScreen(onDone = { navController.navigate(Routes.Home.name) })
            }
        }
    }
}