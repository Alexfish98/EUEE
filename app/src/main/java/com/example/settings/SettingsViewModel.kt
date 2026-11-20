package com.example.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarms.AlarmScheduler
import com.example.core.model.*
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.EueeDatabase
import com.example.focus.DndManager
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StorageDiagnostics(
    val dbFileBytes: Long = 0L,
    val questionCount: Int = 0,
    val noteCount: Int = 0,
    val examCount: Int = 0,
    val notificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
    val dndPolicyGranted: Boolean = false
)

class SettingsViewModel(
    private val appContext: Context,
    private val repository: EueeRepository,
    val preferences: PreferencesRepository
) : ViewModel() {

    val prefsState: StateFlow<UserPreferences> = preferences.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _diagnostics = MutableStateFlow(StorageDiagnostics())
    val diagnostics: StateFlow<StorageDiagnostics> = _diagnostics.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val dbFile = appContext.getDatabasePath(EueeDatabase.DATABASE_NAME)
            val qCount = repository.db.questionDao().getTotalQuestionCount()
            val nCount = repository.db.notebookDao().getAllNotes().size
            val eCount = repository.db.examDao().getAllExamResults().size
            _diagnostics.value = StorageDiagnostics(
                dbFileBytes = if (dbFile.exists()) dbFile.length() else 0L,
                questionCount = qCount,
                noteCount = nCount,
                examCount = eCount,
                notificationsGranted = NotificationHelper.hasNotificationPermission(appContext),
                exactAlarmsGranted = AlarmScheduler.canScheduleExactAlarms(appContext),
                dndPolicyGranted = DndManager.getStatus(appContext).hasPolicyAccess
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { preferences.setThemeMode(mode) } }
    fun setAccentPalette(palette: AccentPalette) { viewModelScope.launch { preferences.setAccentPalette(palette) } }
    fun setFontScale(scale: Float) { viewModelScope.launch { preferences.setFontScale(scale) } }
    fun setLanguage(lang: AppLanguage) { viewModelScope.launch { preferences.setLanguage(lang) } }
    fun setSoundEnabled(v: Boolean) { viewModelScope.launch { preferences.setSoundEnabled(v) } }
    fun setVibrationEnabled(v: Boolean) { viewModelScope.launch { preferences.setVibrationEnabled(v) } }
    fun setNotificationsEnabled(v: Boolean) { viewModelScope.launch { preferences.setNotificationsEnabled(v) } }
    fun setDndAutoEnable(v: Boolean) { viewModelScope.launch { preferences.setDndAutoEnable(v) } }
    fun setKeepAwakeStudy(v: Boolean) { viewModelScope.launch { preferences.setKeepAwakeStudy(v) } }
    fun setKeepAwakeExam(v: Boolean) { viewModelScope.launch { preferences.setKeepAwakeExam(v) } }
    fun setDailyStudyGoal(min: Int) { viewModelScope.launch { preferences.setDailyStudyGoalMinutes(min) } }
    fun setDailyFocusGoal(min: Int) { viewModelScope.launch { preferences.setDailyFocusGoalMinutes(min) } }
    fun setPomodoroFocus(min: Int) { viewModelScope.launch { preferences.setPomodoroFocusMinutes(min) } }
    fun setSrsNewLimit(limit: Int) { viewModelScope.launch { preferences.setSrsDailyNewLimit(limit) } }
    fun setSrsReviewLimit(limit: Int) { viewModelScope.launch { preferences.setSrsDailyReviewLimit(limit) } }
    fun setSpeechRate(rate: Float) { viewModelScope.launch { preferences.setSpeechRate(rate) } }
}
