package com.example.karttracker
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.karttracker.pages.HistoryPage
import com.example.karttracker.pages.HomePage
import com.example.karttracker.pages.SettingsPage

@Composable
fun MainScreen(){

    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("History", Icons.Default.List),
        NavItem("Settings", Icons.Default.Settings),)

    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed{index, navItem ->

                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {selectedIndex = index},
                        icon = {Icon(imageVector = navItem.icon, contentDescription = "Icon")},
                        label = { Text(text = navItem.label)})
                }
            }
        }
    ) { innerPadding->
        ContentScreen(modifier = Modifier.padding(innerPadding), selectedIndex)
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int){
    when(selectedIndex){
        0-> HomePage()
        1-> HistoryPage()
        2-> SettingsPage()
    }
}


