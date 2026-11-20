package com.example.alarms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AlarmCategory
import com.example.data.EueeRepository
import com.example.database.AlarmEntity
import com.example.database.AlarmHistoryEntity
import com.example.database.SubjectEntity
import com.example.widgets.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AlarmsUiState(
    val alarms: List<AlarmEntity> = emptyList(),
    val history: List<AlarmHistoryEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val canScheduleExactAlarms: Boolean = true,
    val lastScheduleMessage: String? = null
)

class AlarmsViewModel(
    private val appContext: Context,
    private val repository: EueeRepository
) : ViewModel() {

    private val exactPermissionFlow = MutableStateFlow(AlarmScheduler.canScheduleExactAlarms(appContext))
    private val lastMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlarmsUiState> = combine(
        repository.observeAlarms(),
        repository.observeAlarmHistory(),
        repository.observeSubjects(),
        exactPermissionFlow,
        lastMessageFlow
    ) { alarms, history, subjects, exact, msg ->
        AlarmsUiState(
            alarms = alarms,
            history = history,
            subjects = subjects,
            canScheduleExactAlarms = exact,
            lastScheduleMessage = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmsUiState())

    fun refreshExactAlarmPermission() {
        exactPermissionFlow.value = AlarmScheduler.canScheduleExactAlarms(appContext)
    }

    fun buildExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null
    }

    fun saveAlarm(
        existing: AlarmEntity?,
        label: String,
        hour: Int,
        minute: Int,
        daysMask: Int,
        isRepeating: Boolean,
        category: AlarmCategory,
        subjectId: Long?,
        vibrate: Boolean,
        snoozeMinutes: Int
    ) {
        viewModelScope.launch {
            val entity = AlarmEntity(
                id = existing?.id ?: 0L,
                label = label.ifBlank { category.label },
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                daysOfWeekMask = daysMask,
                isRepeating = isRepeating && daysMask != 0,
                isEnabled = true,
                category = category.name,
                subjectId = subjectId,
                vibrate = vibrate,
                snoozeMinutes = snoozeMinutes.coerceIn(1, 30),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val savedId = repository.upsertAlarm(entity)
            val savedEntity = entity.copy(id = savedId)
            val res = AlarmScheduler.scheduleAlarm(appContext, savedEntity)
            lastMessageFlow.value = res.message
            refreshExactAlarmPermission()
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.upsertAlarm(updated)
            if (updated.isEnabled) {
                val res = AlarmScheduler.scheduleAlarm(appContext, updated)
                lastMessageFlow.value = res.message
            } else {
                AlarmScheduler.cancelAlarm(appContext, updated.id)
                lastMessageFlow.value = "Alarm '${updated.label}' turned off."
            }
            refreshExactAlarmPermission()
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(appContext, alarm.id)
            repository.deleteAlarm(alarm)
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }
}
