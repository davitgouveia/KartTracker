package com.example.karttracker.pages.SessionSummary

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.karttracker.components.SessionSummaryViewModel
import com.example.karttracker.utils.TimeUtils

sealed class SessionScreen(val route: String, val label: String, val icon: ImageVector) {
    object Map : SessionScreen("summary", "Summary", Icons.Default.Home)
    object Graph : SessionScreen("map", "Map", Icons.Default.Home)
    object Laps : SessionScreen("laps", "Laps", Icons.Default.Home)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    navController: NavController,
    summaryViewModel: SessionSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) {
        summaryViewModel.setSessionId(sessionId)
        Log.d("SummaryScreen", "Session ID set in LaunchedEffect: $sessionId")
    }

    val runSession by summaryViewModel.getRunSession(sessionId).collectAsState(initial = null)

    var sessionName by remember { mutableStateOf("") }
    var savedSessionName by remember { mutableStateOf("") }
    var sessionNamePlaceholder by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }

    val sessionNavController = rememberNavController()


    LaunchedEffect(runSession) {
        runSession?.let {
            sessionNamePlaceholder = "${TimeUtils.getPeriodOfDay(it.startTimeMillis)} Session"
            savedSessionName = it.name
            sessionName = if (it.name.isNotBlank()) { it.name } else { sessionNamePlaceholder }
            isEditingName = false
        }
    }

    MaterialTheme {
        when (val session = runSession) {
            null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading session data...")
                    }
                }
            }
            else -> {
                Scaffold(
                    topBar = { CustomTopBar(title = sessionName, backAction = { navController.navigate("mainScreen") }) },
                    bottomBar = { BottomNavigationBar(sessionNavController) }
                    ) { paddingValues ->
                        NavHost(
                            navController = sessionNavController,
                            startDestination = SessionScreen.Map.route,
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable(SessionScreen.Map.route) {
                                MapVisualizationFragment(session, summaryViewModel)
                            }
                        }
                    }
            }
        }
    }
}
@Preview
@Composable
fun CustomTopBar (
    title: String = "Unknown",
    backAction: () -> Unit = {},
    menuAction: () -> Unit = {}
){
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(0.dp, 4.dp)) {

        IconButton(
            onClick = backAction,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }


        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium
        )


        IconButton(
            onClick = menuAction,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        SessionScreen.Map,
        SessionScreen.Graph,
        SessionScreen.Laps
    )

    NavigationBar {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.route == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}