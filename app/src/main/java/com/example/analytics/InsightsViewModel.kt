package com.example.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.database.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class InsightsUiState(
    val overallAccuracy: Int = 0,
    val totalAttempts: Int = 0,
    val totalStudyMinutes: Int = 0,
    val subjectProgress: List<SubjectProgressRow> = emptyList(),
    val masteryBreakdown: Map<String, Int> = emptyMap(),
    val dailyActivity14d: List<Pair<String, Int>> = emptyList(),
    val accuracyTrend14d: List<Float> = emptyList(),
    val errorCategories: Map<String, Int> = emptyMap(),
    val confidenceDistribution: Map<Int, Int> = emptyMap(),
    val examScores: List<ExamResultEntity> = emptyList(),
    val reviewEffectivenessPercent: Int = 0,
    val weakTopics: List<TopicPerformanceRow> = emptyList()
)

class InsightsViewModel(repository: EueeRepository) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeSubjectProgress(),
        repository.observeAllProgress(),
        repository.observeAttemptsLast30Days(),
        repository.observeExamResults(),
        repository.observeWeakTopics(10)
    ) { subjects, progressList, attempts, exams, weak ->
        val totalAttempts = attempts.size
        val correct = attempts.count { it.isCorrect }
        val overallAcc = if (totalAttempts == 0) 0 else (correct * 100) / totalAttempts
        val totalMin = (attempts.sumOf { it.responseTimeMs } / 60_000L).toInt()

        val masteryMap = progressList.groupingBy { it.masteryStatus }.eachCount()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val byDay = attempts.groupBy { Instant.ofEpochMilli(it.attemptedAt).atZone(zone).toLocalDate() }

        val daily14 = (13 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            "${d.monthValue}/${d.dayOfMonth}" to (byDay[d]?.size ?: 0)
        }

        val accTrend = (13 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            val list = byDay[d].orEmpty()
            if (list.isEmpty()) 0f else (list.count { it.isCorrect }.toFloat() / list.size) * 100f
        }

        val errorCats = attempts.filter { !it.isCorrect }
            .groupingBy { it.errorCategory.ifBlank { "CONCEPT_GAP" } }
            .eachCount()

        val confDist = (1..5).associateWith { c -> attempts.count { it.confidenceRating == c } }

        val reviewedSuccess = progressList.count { it.attempts >= 2 && it.consecutiveCorrect >= 1 }
        val reviewedTotal = progressList.count { it.attempts >= 2 }
        val reviewEff = if (reviewedTotal == 0) 0 else (reviewedSuccess * 100) / reviewedTotal

        InsightsUiState(
            overallAccuracy = overallAcc,
            totalAttempts = totalAttempts,
            totalStudyMinutes = totalMin,
            subjectProgress = subjects,
            masteryBreakdown = masteryMap,
            dailyActivity14d = daily14,
            accuracyTrend14d = accTrend,
            errorCategories = errorCats,
            confidenceDistribution = confDist,
            examScores = exams,
            reviewEffectivenessPercent = reviewEff,
            weakTopics = weak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())
}
