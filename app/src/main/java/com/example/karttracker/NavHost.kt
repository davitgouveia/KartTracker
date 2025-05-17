package com.example.karttracker


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.karttracker.pages.RecordPage
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.karttracker.pages.LapTrackerMapScreen
import com.example.karttracker.pages.RADIUS_ARG
import com.example.karttracker.pages.START_FINISH_LAT_ARG
import com.example.karttracker.pages.START_FINISH_LNG_ARG


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("map") { LapTrackerMapScreen(navController) } // Added LapTrackerMapScreen route
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
    }
}