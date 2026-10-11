package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CallScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AuraViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf("home") }

                    when (currentScreen) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onStartCall = {
                                viewModel.startLiveCall()
                                currentScreen = "call"
                            },
                            onNavigateSettings = { currentScreen = "settings" }
                        )
                        "call" -> CallScreen(
                            viewModel = viewModel,
                            onEndCall = { currentScreen = "home" }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}
