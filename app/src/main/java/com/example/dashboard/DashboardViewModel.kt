package com.example.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.UserPreferences
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyActivityPoint(
    val label: String,
    val attempts: Int,
    val correct: Int
)

data class DashboardUiState(
    val totalQuestions: Int = 0,
    val attemptedQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val accuracyPercent: Int = 0,
    val masteredQuestions: Int = 0,
    val learningQuestions: Int = 0,
    val dueReviews: Int = 0,
    val incorrectAttention: Int = 0,
    val streakDays: Int = 1,
    val todayStudyMinutes: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val weeklyActivity: List<DailyActivityPoint> = emptyList(),
    val subjectProgress: List<SubjectProgressRow> = emptyList(),
    val weakTopics: List<TopicPerformanceRow> = emptyList(),
    val recentSessions: List<StudySessionEntity> = emptyList(),
    val activeSession: StudySessionEntity? = null,
    val inProgressExam: InProgressExamEntity? = null,
    val upcomingAlarms: List<AlarmEntity> = emptyList(),
    val todayTasks: List<StudyTaskEntity> = emptyList(),
    val focusActive: Boolean = false,
    val daysUntilExam: Long = 90L
)

class DashboardViewModel(
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            repository.observeTotalQuestionCount(),
            repository.observeAttemptedQuestionCount(),
            repository.observeTotalCorrect(),
            repository.observeTotalIncorrect(),
            repository.observeMasteredCount()
        ) { total, attempted, correct, incorrect, mastered ->
            listOf(total, attempted, correct, incorrect, mastered)
        },
        combine(
            repository.observeLearningCount(),
            repository.observeDueReviewCount(),
            repository.observeIncorrectAttentionCount(),
            repository.observeTodayStudyTimeMs(),
            preferencesRepository.preferencesFlow
        ) { learning, due, attention, todayMs, prefs ->
            Triple(Triple(learning, due, attention), todayMs, prefs)
        },
        combine(
            repository.observeSubjectProgress(),
            repository.observeWeakTopics(5),
            repository.observeRecentSessions(5),
            repository.observeActiveStudySession(),
            repository.observeInProgressExam()
        ) { subs, weak, sessions, active, exam ->
            listOf(subs, weak, sessions, active, exam)
        },
        combine(
            repository.observeAlarms(),
            repository.observeTasksForDate(today.toEpochDay()),
            repository.observeAttemptsLast30Days(),
            preferencesRepository.focusActiveFlow
        ) { alarms, tasks, attempts, focus ->
            listOf(alarms, tasks, attempts, focus)
        }
    ) { counts, mid, lists, native ->
        val total = counts[0]
        val attempted = counts[1]
        val correct = counts[2]
        val incorrect = counts[3]
        val mastered = counts[4]

        val (learningTriple, todayMs, prefs) = mid
        val (learning, due, attention) = learningTriple

        @Suppress("UNCHECKED_CAST")
        val subs = lists[0] as List<SubjectProgressRow>
        @Suppress("UNCHECKED_CAST")
        val weak = lists[1] as List<TopicPerformanceRow>
        @Suppress("UNCHECKED_CAST")
        val sessions = lists[2] as List<StudySessionEntity>
        val activeSession = lists[3] as StudySessionEntity?
        val inProgressExam = lists[4] as InProgressExamEntity?

        @Suppress("UNCHECKED_CAST")
        val alarms = (native[0] as List<AlarmEntity>).filter { it.isEnabled }
        @Suppress("UNCHECKED_CAST")
        val tasks = native[1] as List<StudyTaskEntity>
        @Suppress("UNCHECKED_CAST")
        val attempts = native[2] as List<AttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val focusPair = native[3] as Pair<Boolean, Long>

        val totalAns = correct + incorrect
        val acc = if (totalAns == 0) 0 else (correct * 100) / totalAns
        val weekly = buildWeeklyActivity(attempts)

        DashboardUiState(
            totalQuestions = total,
            attemptedQuestions = attempted,
            correctAnswers = correct,
            incorrectAnswers = incorrect,
            accuracyPercent = acc,
            masteredQuestions = mastered,
            learningQuestions = learning,
            dueReviews = due,
            incorrectAttention = attention,
            streakDays = prefs.currentStreakDays,
            todayStudyMinutes = (todayMs / 60_000L).toInt(),
            dailyGoalMinutes = prefs.dailyStudyGoalMinutes,
            weeklyActivity = weekly,
            subjectProgress = subs,
            weakTopics = weak,
            recentSessions = sessions,
            activeSession = activeSession,
            inProgressExam = inProgressExam,
            upcomingAlarms = alarms.take(3),
            todayTasks = tasks,
            focusActive = focusPair.first && focusPair.second > System.currentTimeMillis(),
            daysUntilExam = (prefs.examTargetEpochDay - today.toEpochDay()).coerceAtLeast(1L)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun toggleTaskCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.upsertTask(
                task.copy(
                    isCompleted = !task.isCompleted,
                    completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
                )
            )
        }
    }

    private fun buildWeeklyActivity(attempts: List<AttemptEntity>): List<DailyActivityPoint> {
        val zone = ZoneId.systemDefault()
        val byDay = attempts.groupBy {
            Instant.ofEpochMilli(it.attemptedAt).atZone(zone).toLocalDate()
        }
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val list = byDay[date].orEmpty()
            DailyActivityPoint(
                label = date.dayOfWeek.name.take(3),
                attempts = list.size,
                correct = list.count { it.isCorrect }
            )
        }
    }
}
