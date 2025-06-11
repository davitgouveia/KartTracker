package com.example.karttracker
import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.karttracker.pages.HistoryPage
import com.example.karttracker.pages.HomePage
import com.example.karttracker.pages.SettingsPage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun MainScreen(navController: NavHostController) {
    MainScreenWithPermissionHandling(navController = navController)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreenWithPermissionHandling(navController: NavHostController) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // A simple state to control if content should be shown
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.status) {
        showContent = locationPermissionState.status.isGranted
    }

    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("History", Icons.AutoMirrored.Filled.List)
        //NavItem("Settings", Icons.Default.Settings),
    )

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }
    if (showContent) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    navItemList.forEachIndexed { index, navItem ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = { Icon(imageVector = navItem.icon, contentDescription = "Icon") },
                            label = { Text(text = navItem.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->

            ContentScreen(modifier = Modifier.padding(innerPadding), selectedIndex, navController)

        }
    }
    else {
        // Display a message or a UI that prompts the user to grant permission
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    text = "Location permission is required to use this app.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Request permission again if it was denied previously
                    if (!locationPermissionState.status.isGranted) {
                        locationPermissionState.launchPermissionRequest()
                    }
                }) {
                    Text("Grant Permission")
                }
                if (locationPermissionState.status.shouldShowRationale) {
                    // If the user has denied the permission but the rationale can be shown,
                    // then gently explain why the app requires this permission
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enable location permission in your device settings for full functionality.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // If it's the first time the user lands on this feature, or the user
                    // doesn't want to be asked again for this permission, explain that the
                    // permission is required
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Location permission is required in your device settings for full functionality.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

}



@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int, navController: NavController) {
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
        when (selectedIndex) {
            0 -> HomePage(navController)
            1 -> HistoryPage(navController)
            2 -> SettingsPage()
        }
    }
}

