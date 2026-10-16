package id.gustyxganteng.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.designsystem.GGAgentTheme
import id.gustyxganteng.agent.feature.chat.ChatScreen
import id.gustyxganteng.agent.feature.chat.ChatViewModel
import id.gustyxganteng.agent.feature.settings.SettingsScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModel()
    private val settingsRepository: AppSettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GGAgentTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "chat"
                ) {
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
                            }
                        )
                    }
                }
            }
        }
    }
}
