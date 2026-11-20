package com.example.app

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.example.core.speech.SpeechHelper
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.EueeDatabase
import com.example.notifications.NotificationHelper
import com.example.notifications.StudyReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: EueeDatabase by lazy { EueeDatabase.getInstance(appContext) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(appContext) }
    val repository: EueeRepository by lazy { EueeRepository(database, preferencesRepository) }
    val speechHelper: SpeechHelper by lazy { SpeechHelper(appContext) }
}

class EueeMasteryApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        runCatching { StudyReminderWorker.schedulePeriodic(this) }
        appScope.launch {
            container.repository.ensureSeeded()
        }
    }
}
