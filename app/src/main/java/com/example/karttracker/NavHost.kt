package com.example.karttracker


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import com.example.karttracker.pages.RecordPage
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.karttracker.pages.HistoryPage
import com.example.karttracker.pages.LapTrackerMapScreen
import com.example.karttracker.pages.RADIUS_ARG
import com.example.karttracker.pages.START_FINISH_LAT_ARG
import com.example.karttracker.pages.START_FINISH_LNG_ARG
import com.example.karttracker.pages.SessionSummary.SessionSummaryScreen


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("map") { LapTrackerMapScreen(navController) }
        composable("history") { HistoryPage(navController) }
        composable(
            "record/{$START_FINISH_LAT_ARG}/{$START_FINISH_LNG_ARG}/{$RADIUS_ARG}",
            arguments = listOf(
                navArgument(START_FINISH_LAT_ARG) { type = androidx.navigation.NavType.StringType },
                navArgument(START_FINISH_LNG_ARG) { type = androidx.navigation.NavType.StringType },
                navArgument(RADIUS_ARG) { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            RecordPage(navController = navController)
        }
        composable(
            route = "summary_route/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
            if (sessionId != null) {
                SessionSummaryScreen(sessionId = sessionId, navController = navController)
            } else {
                // Handle error: sessionId not found, maybe show a message or navigate back
                Text("Error: Session ID not found", color = Color.Red)
            }
        }
    }
}