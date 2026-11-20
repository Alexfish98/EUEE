package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.AccentPalette
import com.example.core.model.AppLanguage
import com.example.core.model.ThemeMode
import com.example.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "euee_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ALARM_DEFAULT_SNOOZE = intPreferencesKey("alarm_default_snooze")
        val DND_AUTO_ENABLE = booleanPreferencesKey("dnd_auto_enable")
        val POMODORO_FOCUS = intPreferencesKey("pomodoro_focus")
        val POMODORO_SHORT_BREAK = intPreferencesKey("pomodoro_short_break")
        val POMODORO_LONG_BREAK = intPreferencesKey("pomodoro_long_break")
        val DAILY_FOCUS_GOAL = intPreferencesKey("daily_focus_goal")
        val DAILY_STUDY_GOAL = intPreferencesKey("daily_study_goal")
        val DEFAULT_EXAM_MINUTES = intPreferencesKey("default_exam_minutes")
        val KEEP_AWAKE_STUDY = booleanPreferencesKey("keep_awake_study")
        val KEEP_AWAKE_EXAM = booleanPreferencesKey("keep_awake_exam")
        val SRS_DAILY_NEW_LIMIT = intPreferencesKey("srs_daily_new_limit")
        val SRS_DAILY_REVIEW_LIMIT = intPreferencesKey("srs_daily_review_limit")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_STUDY_EPOCH_DAY = longPreferencesKey("last_study_epoch_day")
        val FOCUS_ACTIVE = booleanPreferencesKey("focus_active")
        val FOCUS_END_EPOCH_MS = longPreferencesKey("focus_end_epoch_ms")
        val FOCUS_PREV_DND_FILTER = intPreferencesKey("focus_prev_dnd_filter")
        val EXAM_TARGET_EPOCH_DAY = longPreferencesKey("exam_target_epoch_day")
        val TARGET_TOTAL_SCORE = intPreferencesKey("target_total_score")
        val COMPLETED_SYLLABUS_TOPICS = stringSetPreferencesKey("completed_syllabus_topics")
        val COMPLETED_CHECKLIST_ITEMS = stringSetPreferencesKey("completed_checklist_items")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            accentPalette = runCatching { AccentPalette.valueOf(p[Keys.ACCENT_PALETTE] ?: "") }.getOrDefault(AccentPalette.ABYSSINIAN_COBALT),
            fontScale = (p[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
            language = runCatching { AppLanguage.valueOf(p[Keys.LANGUAGE] ?: "") }.getOrDefault(AppLanguage.ENGLISH),
            soundEnabled = p[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = p[Keys.VIBRATION_ENABLED] ?: true,
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: true,
            alarmDefaultSnoozeMinutes = p[Keys.ALARM_DEFAULT_SNOOZE] ?: 10,
            dndAutoEnableForFocus = p[Keys.DND_AUTO_ENABLE] ?: false,
            pomodoroFocusMinutes = p[Keys.POMODORO_FOCUS] ?: 25,
            pomodoroShortBreakMinutes = p[Keys.POMODORO_SHORT_BREAK] ?: 5,
            pomodoroLongBreakMinutes = p[Keys.POMODORO_LONG_BREAK] ?: 15,
            dailyFocusGoalMinutes = p[Keys.DAILY_FOCUS_GOAL] ?: 90,
            dailyStudyGoalMinutes = p[Keys.DAILY_STUDY_GOAL] ?: 120,
            defaultExamMinutes = p[Keys.DEFAULT_EXAM_MINUTES] ?: 60,
            keepScreenAwakeInStudy = p[Keys.KEEP_AWAKE_STUDY] ?: true,
            keepScreenAwakeInExam = p[Keys.KEEP_AWAKE_EXAM] ?: true,
            srsDailyNewLimit = p[Keys.SRS_DAILY_NEW_LIMIT] ?: 20,
            srsDailyReviewLimit = p[Keys.SRS_DAILY_REVIEW_LIMIT] ?: 60,
            speechRate = (p[Keys.SPEECH_RATE] ?: 1.0f).coerceIn(0.6f, 1.8f),
            currentStreakDays = p[Keys.STREAK_DAYS] ?: 1,
            lastStudyEpochDay = p[Keys.LAST_STUDY_EPOCH_DAY] ?: 0L,
            examTargetEpochDay = p[Keys.EXAM_TARGET_EPOCH_DAY] ?: (LocalDate.now().toEpochDay() + 90L),
            targetTotalScore = (p[Keys.TARGET_TOTAL_SCORE] ?: 520).coerceIn(200, 600),
            completedSyllabusTopicIds = p[Keys.COMPLETED_SYLLABUS_TOPICS] ?: emptySet(),
            completedChecklistIds = p[Keys.COMPLETED_CHECKLIST_ITEMS] ?: emptySet()
        )
    }

    val focusActiveFlow: Flow<Pair<Boolean, Long>> = context.dataStore.data.map { p ->
        (p[Keys.FOCUS_ACTIVE] ?: false) to (p[Keys.FOCUS_END_EPOCH_MS] ?: 0L)
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccentPalette(palette: AccentPalette) = update { it[Keys.ACCENT_PALETTE] = palette.name }
    suspend fun setFontScale(scale: Float) = update { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    suspend fun setLanguage(language: AppLanguage) = update { it[Keys.LANGUAGE] = language.name }
    suspend fun setSoundEnabled(enabled: Boolean) = update { it[Keys.SOUND_ENABLED] = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean) = update { it[Keys.VIBRATION_ENABLED] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = update { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setAlarmDefaultSnooze(minutes: Int) = update { it[Keys.ALARM_DEFAULT_SNOOZE] = minutes.coerceIn(1, 30) }
    suspend fun setDndAutoEnable(enabled: Boolean) = update { it[Keys.DND_AUTO_ENABLE] = enabled }
    suspend fun setPomodoroFocusMinutes(minutes: Int) = update { it[Keys.POMODORO_FOCUS] = minutes.coerceIn(5, 120) }
    suspend fun setPomodoroShortBreakMinutes(minutes: Int) = update { it[Keys.POMODORO_SHORT_BREAK] = minutes.coerceIn(1, 30) }
    suspend fun setPomodoroLongBreakMinutes(minutes: Int) = update { it[Keys.POMODORO_LONG_BREAK] = minutes.coerceIn(5, 60) }
    suspend fun setDailyFocusGoalMinutes(minutes: Int) = update { it[Keys.DAILY_FOCUS_GOAL] = minutes.coerceIn(10, 480) }
    suspend fun setDailyStudyGoalMinutes(minutes: Int) = update { it[Keys.DAILY_STUDY_GOAL] = minutes.coerceIn(15, 600) }
    suspend fun setDefaultExamMinutes(minutes: Int) = update { it[Keys.DEFAULT_EXAM_MINUTES] = minutes.coerceIn(5, 240) }
    suspend fun setKeepAwakeStudy(enabled: Boolean) = update { it[Keys.KEEP_AWAKE_STUDY] = enabled }
    suspend fun setKeepAwakeExam(enabled: Boolean) = update { it[Keys.KEEP_AWAKE_EXAM] = enabled }
    suspend fun setSrsDailyNewLimit(limit: Int) = update { it[Keys.SRS_DAILY_NEW_LIMIT] = limit.coerceIn(1, 200) }
    suspend fun setSrsDailyReviewLimit(limit: Int) = update { it[Keys.SRS_DAILY_REVIEW_LIMIT] = limit.coerceIn(5, 500) }
    suspend fun setSpeechRate(rate: Float) = update { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.8f) }

    suspend fun setExamTargetEpochDay(epochDay: Long) = update { it[Keys.EXAM_TARGET_EPOCH_DAY] = epochDay }
    suspend fun setTargetTotalScore(score: Int) = update { it[Keys.TARGET_TOTAL_SCORE] = score.coerceIn(200, 600) }

    suspend fun toggleSyllabusTopic(topicId: String) = update { p ->
        val current = p[Keys.COMPLETED_SYLLABUS_TOPICS]?.toMutableSet() ?: mutableSetOf()
        if (!current.add(topicId)) current.remove(topicId)
        p[Keys.COMPLETED_SYLLABUS_TOPICS] = current
    }

    suspend fun toggleChecklistItem(itemId: String) = update { p ->
        val current = p[Keys.COMPLETED_CHECKLIST_ITEMS]?.toMutableSet() ?: mutableSetOf()
        if (!current.add(itemId)) current.remove(itemId)
        p[Keys.COMPLETED_CHECKLIST_ITEMS] = current
    }

    suspend fun clearChecklistItems(itemIds: Collection<String>) = update { p ->
        val current = p[Keys.COMPLETED_CHECKLIST_ITEMS]?.toMutableSet() ?: mutableSetOf()
        current.removeAll(itemIds.toSet())
        p[Keys.COMPLETED_CHECKLIST_ITEMS] = current
    }

    suspend fun recordDailyActivityForStreak(todayEpochDay: Long = LocalDate.now().toEpochDay()) {
        context.dataStore.edit { p ->
            val lastDay = p[Keys.LAST_STUDY_EPOCH_DAY] ?: 0L
            val currentStreak = p[Keys.STREAK_DAYS] ?: 0
            when {
                lastDay == todayEpochDay -> Unit
                lastDay == todayEpochDay - 1 -> {
                    p[Keys.STREAK_DAYS] = currentStreak + 1
                    p[Keys.LAST_STUDY_EPOCH_DAY] = todayEpochDay
                }
                else -> {
                    p[Keys.STREAK_DAYS] = 1
                    p[Keys.LAST_STUDY_EPOCH_DAY] = todayEpochDay
                }
            }
        }
    }

    suspend fun saveFocusRuntimeState(active: Boolean, endEpochMs: Long, prevDndFilter: Int? = null) {
        context.dataStore.edit { p ->
            p[Keys.FOCUS_ACTIVE] = active
            p[Keys.FOCUS_END_EPOCH_MS] = endEpochMs
            if (prevDndFilter != null) {
                p[Keys.FOCUS_PREV_DND_FILTER] = prevDndFilter
            }
        }
    }

    suspend fun getSavedPrevDndFilter(): Int? {
        var result: Int? = null
        context.dataStore.edit { p ->
            result = p[Keys.FOCUS_PREV_DND_FILTER]
        }
        return result
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
