package com.example.iwt.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.iwt.ui.screens.CalibrationScreen
import com.example.iwt.ui.screens.HomeScreen
import com.example.iwt.ui.screens.SessionScreen
import com.example.iwt.ui.screens.SummaryScreen
import com.example.iwt.ui.theme.IwtTheme

enum class Routes { Home, Session, Summary, Calibration }

@Composable
fun IwtApp() {
    val navController = rememberNavController()

    IwtTheme {
        NavHost(navController = navController, startDestination = Routes.Home.name) {
            composable(Routes.Home.name) {
                HomeScreen(
                    onStart = { fast, slow ->
                        navController.navigate("${Routes.Session.name}/$fast/$slow")
                    },
                    onStartCalibration = { navController.navigate(Routes.Calibration.name) }
                )
            }
            composable(Routes.Calibration.name) {
                CalibrationScreen(
                    onComplete = {
                        navController.navigate(Routes.Home.name) {
                            popUpTo(Routes.Home.name) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.Session.name}/{fastSpm}/{slowSpm}",
                arguments = listOf(
                    navArgument("fastSpm") { type = NavType.IntType },
                    navArgument("slowSpm") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val fastSpm = backStackEntry.arguments?.getInt("fastSpm") ?: 120
                val slowSpm = backStackEntry.arguments?.getInt("slowSpm") ?: 80
                SessionScreen(
                    fastSpm = fastSpm,
                    slowSpm = slowSpm,
                    onFinish = { _ ->
                        navController.navigate(Routes.Summary.name) {
                            popUpTo(Routes.Home.name)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.Summary.name) {
                SummaryScreen(onDone = {
                    navController.popBackStack(Routes.Home.name, inclusive = false)
                })
            }
        }
    }
}
