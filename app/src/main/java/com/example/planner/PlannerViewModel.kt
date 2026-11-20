package com.example.planner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.TaskPriority
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.SubjectEntity
import com.example.database.StudyTaskEntity
import com.example.database.TopicEntity
import com.example.database.UnitEntity
import com.example.widgets.WidgetUpdateHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isWeeklyView: Boolean = false,
    val dayTasks: List<StudyTaskEntity> = emptyList(),
    val weekTasks: List<StudyTaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val dailyStudyGoalMinutes: Int = 120
) {
    val completedCount: Int get() = dayTasks.count { it.isCompleted }
    val plannedMinutes: Int get() = dayTasks.sumOf { it.durationMinutes }
    val completedMinutes: Int get() = dayTasks.filter { it.isCompleted }.sumOf { it.durationMinutes }
}

class PlannerViewModel(
    private val appContext: Context,
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val isWeeklyView = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayTasksFlow = selectedDate.flatMapLatest { d ->
        repository.observeTasksForDate(d.toEpochDay())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val weekTasksFlow = selectedDate.flatMapLatest { d ->
        val start = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        repository.observeTasksInRange(start.toEpochDay(), end.toEpochDay())
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        selectedDate,
        isWeeklyView,
        dayTasksFlow,
        weekTasksFlow,
        combine(
            repository.observeSubjects(),
            repository.observeUnits(),
            repository.observeTopics(),
            preferencesRepository.preferencesFlow
        ) { s, u, t, p -> listOf(s, u, t, p) }
    ) { date, weekly, dTasks, wTasks, meta ->
        @Suppress("UNCHECKED_CAST")
        PlannerUiState(
            selectedDate = date,
            isWeeklyView = weekly,
            dayTasks = dTasks,
            weekTasks = wTasks,
            subjects = meta[0] as List<SubjectEntity>,
            units = meta[1] as List<UnitEntity>,
            topics = meta[2] as List<TopicEntity>,
            dailyStudyGoalMinutes = (meta[3] as com.example.core.model.UserPreferences).dailyStudyGoalMinutes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlannerUiState())

    fun selectDate(date: LocalDate) { selectedDate.value = date }
    fun setWeeklyView(weekly: Boolean) { isWeeklyView.value = weekly }

    fun saveTask(
        existing: StudyTaskEntity?,
        title: String,
        notes: String,
        subjectId: Long?,
        unitId: Long?,
        topicId: Long?,
        date: LocalDate,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int,
        priority: TaskPriority
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.upsertTask(
                StudyTaskEntity(
                    id = existing?.id ?: 0L,
                    title = title.trim(),
                    notes = notes.trim(),
                    subjectId = subjectId,
                    unitId = unitId,
                    topicId = topicId,
                    scheduledDateEpochDay = date.toEpochDay(),
                    startTimeMinutes = (startHour * 60 + startMinute).coerceIn(0, 1439),
                    durationMinutes = durationMinutes.coerceIn(5, 480),
                    priority = priority.name,
                    isCompleted = existing?.isCompleted ?: false,
                    completedAt = existing?.completedAt
                )
            )
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }

    fun toggleCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.upsertTask(
                task.copy(
                    isCompleted = !task.isCompleted,
                    completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
                )
            )
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }
}
