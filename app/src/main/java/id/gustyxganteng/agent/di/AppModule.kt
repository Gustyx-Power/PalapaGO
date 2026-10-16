package id.gustyxganteng.agent.di

import androidx.room.Room
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.database.ChatRepository
import id.gustyxganteng.agent.core.database.GGAgentDatabase
import id.gustyxganteng.agent.core.network.AgnesApiService
import id.gustyxganteng.agent.core.network.AgnesApiServiceImpl
import id.gustyxganteng.agent.core.network.SmartRetryInterceptor
import id.gustyxganteng.agent.core.network.SseStreamParser
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
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

    single { AppSettingsRepository(androidContext()) }
    single { ChatRepository(get(), get(), get()) }

    // Network & SSE
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
}
