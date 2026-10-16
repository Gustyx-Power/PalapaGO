package id.gustyxganteng.agent

import android.app.Application
import id.gustyxganteng.agent.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class GGAgentApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@GGAgentApp)
            modules(appModule)
        }
    }
}
