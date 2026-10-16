package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pskprop.ui.screens.AzimuthCalcScreen
import com.example.pskprop.ui.screens.MapScreen
import com.example.pskprop.ui.screens.SettingsScreen
import com.example.pskprop.ui.screens.SpotsListScreen
import com.example.pskprop.ui.screens.StatsScreen
import com.example.pskprop.viewmodel.PskPropViewModel
import com.example.ui.theme.MyApplicationTheme

enum class PskNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MAP("Map", Icons.Default.Public),
    SPOTS("Spots", Icons.Default.List),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    CALCULATOR("Azimuth", Icons.Default.Explore),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: PskPropViewModel = viewModel()
                var currentTab by remember { mutableStateOf(PskNavTab.MAP) }

                BackHandler(enabled = currentTab != PskNavTab.MAP) {
                    currentTab = PskNavTab.MAP
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .testTag("bottom_nav_bar")
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            PskNavTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        PskNavTab.MAP -> {
                            MapScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        PskNavTab.SPOTS -> {
                            SpotsListScreen(
                                viewModel = viewModel,
                                onNavigateToMap = { currentTab = PskNavTab.MAP },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        PskNavTab.ANALYTICS -> {
                            StatsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        PskNavTab.CALCULATOR -> {
                            AzimuthCalcScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        PskNavTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
