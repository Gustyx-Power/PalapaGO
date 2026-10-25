package id.gustyxganteng.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.designsystem.GGAgentTheme
import id.gustyxganteng.agent.core.designsystem.component.NetworkOfflineOverlay
import id.gustyxganteng.agent.core.network.NetworkMonitor
import id.gustyxganteng.agent.feature.chat.ChatScreen
import id.gustyxganteng.agent.feature.chat.ChatViewModel
import id.gustyxganteng.agent.feature.onboarding.OnboardingScreen
import id.gustyxganteng.agent.feature.settings.SettingsScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModel()
    private val settingsRepository: AppSettingsRepository by inject()
    private val networkMonitor: NetworkMonitor by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GGAgentTheme {
                val hasCompletedOnboarding by settingsRepository.hasCompletedOnboarding.collectAsState(initial = null)
                val isOnline by networkMonitor.isOnline.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCompletedOnboarding != null) {
                        val navController = rememberNavController()
                        val startDest = if (hasCompletedOnboarding == true) "chat" else "onboarding"

                        NavHost(
                            navController = navController,
                            startDestination = startDest
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    settingsRepository = settingsRepository,
                                    onFinished = {
                                        navController.navigate("chat") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("chat") {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    settingsRepository = settingsRepository,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToOnboarding = {
                                        navController.navigate("onboarding")
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    NetworkOfflineOverlay(
                        isOnline = isOnline,
                        onRetry = { networkMonitor.checkConnectivity() }
                    )
                }
            }
        }
    }
}
