package id.gustyxganteng.agent.di

import androidx.room.Room
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.database.GGAgentDatabase
import id.gustyxganteng.agent.core.network.AgnesApiService
import id.gustyxganteng.agent.core.network.AgnesApiServiceImpl
import id.gustyxganteng.agent.core.network.GeminiSidecarService
import id.gustyxganteng.agent.core.network.GeminiSidecarServiceImpl
import id.gustyxganteng.agent.core.network.GroqSidecarService
import id.gustyxganteng.agent.core.network.GroqSidecarServiceImpl
import id.gustyxganteng.agent.core.network.NetworkMonitor
import id.gustyxganteng.agent.core.network.SmartRetryInterceptor
import id.gustyxganteng.agent.core.network.SseStreamParser
import id.gustyxganteng.agent.domain.agent.AgentOrchestrator
import id.gustyxganteng.agent.domain.agent.scaffold.AgentsScaffolder
import id.gustyxganteng.agent.domain.context.ContextMeterManager
import id.gustyxganteng.agent.domain.rules.AgentRulesManager
import id.gustyxganteng.agent.domain.skills.AgentSkillsManager
import id.gustyxganteng.agent.domain.skills.SkillMatcher
import id.gustyxganteng.agent.domain.tools.AcademicTool
import id.gustyxganteng.agent.domain.tools.FileSystemTool
import id.gustyxganteng.agent.domain.tools.ImageTool
import id.gustyxganteng.agent.domain.tools.ShellTool
import id.gustyxganteng.agent.domain.tools.WebTool
import id.gustyxganteng.agent.feature.chat.ChatViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
        }
    }

    // Database & DataStore
    single {
        Room.databaseBuilder(
            androidContext(),
            GGAgentDatabase::class.java,
            "gg_agent_database"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<GGAgentDatabase>().chatSessionDao() }
    single { get<GGAgentDatabase>().messageDao() }
    single { get<GGAgentDatabase>().workspaceDao() }
    single { get<GGAgentDatabase>().userRuleDao() }

    single { AppSettingsRepository(androidContext()) }
    single { ChatRepository(get(), get(), get()) }

    // Network & SSE
    single { NetworkMonitor(androidContext()) }
    single {
        SmartRetryInterceptor(
            maxRetriesProvider = { 5 }
        )
    }
    single { SseStreamParser(get()) }
    single<AgnesApiService> {
        AgnesApiServiceImpl(
            retryInterceptor = get(),
            sseStreamParser = get(),
            json = get()
        )
    }

    single<GroqSidecarService> { GroqSidecarServiceImpl() }
    single<GeminiSidecarService> { GeminiSidecarServiceImpl() }

    // Tools & Context
    single { FileSystemTool() }
    single { ShellTool() }
    single { WebTool() }
    single { AcademicTool() }
    single { ImageTool(get(), get(), androidContext()) }
    single { ContextMeterManager(get(), get()) }
    single { SkillMatcher() }
    single { AgentSkillsManager(get()) }
    single { AgentRulesManager(get()) }
    single { AgentsScaffolder() }
    single {
        AgentOrchestrator(
            agnesApiService = get(),
            chatRepository = get(),
            fileSystemTool = get(),
            shellTool = get(),
            webTool = get(),
            academicTool = get(),
            imageTool = get(),
            settingsRepository = get(),
            groqSidecarService = get(),
            geminiSidecarService = get(),
            agentRulesManager = get(),
            agentSkillsManager = get(),
            agentsScaffolder = get(),
            json = get()
        )
    }

    // ViewModel
    viewModel {
        ChatViewModel(
            application = get(),
            chatRepository = get(),
            settingsRepository = get(),
            workspaceDao = get(),
            orchestrator = get(),
            contextMeterManager = get(),
            retryInterceptor = get(),
            groqSidecarService = getOrNull(),
            agnesApiService = getOrNull()
        )
    }
}
