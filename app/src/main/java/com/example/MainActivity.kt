package com.example

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nova.engine.PermissionManager
import com.example.nova.ui.NovaViewModel
import com.example.nova.ui.screens.*
import com.example.ui.theme.NovaAiTheme
import com.example.ui.theme.NovaDarkBackground

enum class NovaScreen {
    MAIN,
    TOOLS,
    NOTES,
    MEMORY,
    PRIVACY,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NovaAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NovaDarkBackground
                ) {
                    val viewModel: NovaViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf(NovaScreen.MAIN) }

                    // Runtime permission launcher using PermissionManager
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        // Permissions handled gracefully
                    }

                    LaunchedEffect(Unit) {
                        if (!PermissionManager.hasAllPermissions(this@MainActivity)) {
                            PermissionManager.requestAllPermissions(permissionLauncher)
                        }
                    }

                    // Back handler for secondary screens
                    BackHandler(enabled = currentScreen != NovaScreen.MAIN) {
                        currentScreen = NovaScreen.MAIN
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            NovaScreen.MAIN -> NovaMainScreen(
                                viewModel = viewModel,
                                onNavigateToTools = { currentScreen = NovaScreen.TOOLS },
                                onNavigateToNotes = { currentScreen = NovaScreen.NOTES },
                                onNavigateToMemory = { currentScreen = NovaScreen.MEMORY },
                                onNavigateToPrivacy = { currentScreen = NovaScreen.PRIVACY },
                                onNavigateToSettings = { currentScreen = NovaScreen.SETTINGS }
                            )
                            NovaScreen.TOOLS -> ToolsDashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = NovaScreen.MAIN }
                            )
                            NovaScreen.NOTES -> NotesTasksScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = NovaScreen.MAIN }
                            )
                            NovaScreen.MEMORY -> MemoryScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = NovaScreen.MAIN }
                            )
                            NovaScreen.PRIVACY -> PrivacyDashboardScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = NovaScreen.MAIN }
                            )
                            NovaScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = NovaScreen.MAIN }
                            )
                        }
                    }
                }
            }
        }
    }
}
