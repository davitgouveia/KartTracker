package com.example.karttracker


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.karttracker.pages.RecordPage
import androidx.navigation.compose.composable


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("recordPage") { RecordPage(navController) }
    }
}