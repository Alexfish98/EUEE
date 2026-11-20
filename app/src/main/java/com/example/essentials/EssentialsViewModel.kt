package com.example.essentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

data class SyllabusUnitItem(
    val unit: UnitEntity,
    val subjectName: String,
    val subjectColorHex: Long,
    val gradeLevel: Int,
    val topics: List<TopicEntity>
)

data class EssentialsChecklistItem(
    val id: String,
    val group: String,
    val title: String,
    val detail: String
)

data class EssentialsUiState(
    val daysUntilExam: Long = 90L,
    val examTargetDateFormatted: String = "",
    val targetTotalScore: Int = 520,
    val totalQuestions: Int = 0,
    val masteredQuestions: Int = 0,
    val recommendedQuestionsPerDay: Int = 10,
    val recommendedStudyMinutesPerDay: Int = 30,
    val subjects: List<SubjectEntity> = emptyList(),
    val subjectProgress: List<SubjectProgressRow> = emptyList(),
    val syllabusUnits: List<SyllabusUnitItem> = emptyList(),
    val completedSyllabusTopicIds: Set<String> = emptySet(),
    val completedChecklistIds: Set<String> = emptySet(),
    val subjectScoreEstimates: Map<String, Int> = mapOf(
        "Mathematics" to 78,
        "Physics" to 74,
        "Chemistry" to 76,
        "Biology" to 80,
        "English" to 82,
        "Aptitude" to 80
    )
) {
    val estimatedTotalScore: Int get() = subjectScoreEstimates.values.sum()
    val estimatedPercent: Int get() = (estimatedTotalScore * 100) / 600
}

class EssentialsViewModel(
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    private val customEstimates = MutableStateFlow(
        mapOf(
            "Mathematics" to 78,
            "Physics" to 74,
            "Chemistry" to 76,
            "Biology" to 80,
            "English" to 82,
            "Aptitude" to 80
        )
    )

    val checklistItems: List<EssentialsChecklistItem> = listOf(
        // Group 1: Exam-Day Pouch
        EssentialsChecklistItem("kit_id", "Exam-Day Pouch", "EUEE Admission Card & School/National ID", "Keep in a clear waterproof sleeve the night before."),
        EssentialsChecklistItem("kit_pencils", "Exam-Day Pouch", "3 Pre-Sharpened HB Pencils + Sharpener", "Dark, clean shading prevents optical scanner misreads."),
        EssentialsChecklistItem("kit_eraser", "Exam-Day Pouch", "Soft Non-Smudge White Eraser", "Cleanly erase without tearing the OMR answer sheet."),
        EssentialsChecklistItem("kit_watch", "Exam-Day Pouch", "Non-Programmable Analog Watch", "Smartwatches and phones are strictly prohibited in exam halls."),
        EssentialsChecklistItem("kit_water", "Exam-Day Pouch", "Label-Free Clear Water Bottle", "Stay hydrated during 2–3 hour sessions without label issues."),
        // Group 2: Night-Before & Morning Routine
        EssentialsChecklistItem("prep_cutoff", "Night-Before & Morning", "Stop heavy studying by 8:00 PM", "Lightly skim core formulas only; protect working memory."),
        EssentialsChecklistItem("prep_sleep", "Night-Before & Morning", "7.5–8 Hours Sleep Target", "Consolidates recall speed for early morning Aptitude & Math."),
        EssentialsChecklistItem("prep_travel", "Night-Before & Morning", "Arrive at Exam Center 45 Minutes Early", "Allow buffer time for traffic, room verification, and seating."),
        EssentialsChecklistItem("prep_meal", "Night-Before & Morning", "Balanced Low-Sugar Breakfast", "Avoid heavy sugar spikes that cause mid-exam fatigue."),
        // Group 3: OMR & Time-Budget Strategy
        EssentialsChecklistItem("strat_pass1", "OMR & Time Strategy", "Two-Pass Rule: Bank Fast Questions First", "Never spend >2 minutes on one tough question during Pass 1."),
        EssentialsChecklistItem("strat_sync", "OMR & Time Strategy", "Verify Bubble Row Alignment Every 10 Items", "Cross-check question #20, #30, #40 to prevent a cascade shift."),
        EssentialsChecklistItem("strat_elimin", "OMR & Time Strategy", "Eliminate 2 Distractors Before Educated Guessing", "Check units, sign (+/−), and order of magnitude first."),
        EssentialsChecklistItem("strat_final10", "OMR & Time Strategy", "Reserve Final 8 Minutes for Blank Scan", "Ensure every OMR bubble is filled darkly with no stray marks.")
    )

    val uiState: StateFlow<EssentialsUiState> = combine(
        combine(
            repository.observeSubjects(),
            repository.observeGrades(),
            repository.observeUnits(),
            repository.observeTopics()
        ) { subs, grades, units, topics ->
            Quad(subs, grades, units, topics)
        },
        combine(
            repository.observeTotalQuestionCount(),
            repository.observeMasteredCount(),
            repository.observeSubjectProgress(),
            preferencesRepository.preferencesFlow,
            customEstimates
        ) { totalQ, masteredQ, subProg, prefs, est ->
            Quint(totalQ, masteredQ, subProg, prefs, est)
        }
    ) { curr, meta ->
        val (subs, grades, units, topics) = curr
        val (totalQ, masteredQ, subProg, prefs, estimates) = meta

        val todayEpoch = LocalDate.now().toEpochDay()
        val targetEpoch = if (prefs.examTargetEpochDay > 0L) prefs.examTargetEpochDay else todayEpoch + 90L
        val daysLeft = max(1L, targetEpoch - todayEpoch)
        val formattedDate = LocalDate.ofEpochDay(targetEpoch).format(dateFmt)

        val remainingQ = max(0, totalQ - masteredQ)
        val recPerDay = if (remainingQ == 0) 5 else max(5, ceil(remainingQ.toDouble() / daysLeft.toDouble()).toInt())
        val recMins = max(20, recPerDay * 3)

        val subMap = subs.associateBy { it.id }
        val gradeMap = grades.associateBy { it.id }
        val topicsByUnit = topics.groupBy { it.unitId }

        val syllabusUnits = units.mapNotNull { u ->
            val sub = subMap[u.subjectId] ?: return@mapNotNull null
            val gr = gradeMap[u.gradeId] ?: return@mapNotNull null
            val tList = topicsByUnit[u.id].orEmpty()
            SyllabusUnitItem(
                unit = u,
                subjectName = sub.name,
                subjectColorHex = sub.colorHex,
                gradeLevel = gr.level,
                topics = tList
            )
        }.sortedWith(compareBy({ it.subjectName }, { it.gradeLevel }, { it.unit.unitNumber }))

        EssentialsUiState(
            daysUntilExam = daysLeft,
            examTargetDateFormatted = formattedDate,
            targetTotalScore = prefs.targetTotalScore,
            totalQuestions = totalQ,
            masteredQuestions = masteredQ,
            recommendedQuestionsPerDay = recPerDay,
            recommendedStudyMinutesPerDay = recMins,
            subjects = subs,
            subjectProgress = subProg,
            syllabusUnits = syllabusUnits,
            completedSyllabusTopicIds = prefs.completedSyllabusTopicIds,
            completedChecklistIds = prefs.completedChecklistIds,
            subjectScoreEstimates = estimates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EssentialsUiState())

    fun setExamTargetDaysFromToday(days: Long) {
        viewModelScope.launch {
            val target = LocalDate.now().toEpochDay() + days.coerceIn(1L, 730L)
            preferencesRepository.setExamTargetEpochDay(target)
        }
    }

    fun shiftExamTargetDays(deltaDays: Long) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val currentDays = uiState.value.daysUntilExam
            val newDays = (currentDays + deltaDays).coerceIn(1L, 730L)
            preferencesRepository.setExamTargetEpochDay(today + newDays)
        }
    }

    fun setTargetTotalScore(score: Int) {
        viewModelScope.launch {
            preferencesRepository.setTargetTotalScore(score)
        }
    }

    fun toggleSyllabusTopic(topicId: Long) {
        viewModelScope.launch {
            preferencesRepository.toggleSyllabusTopic(topicId.toString())
        }
    }

    fun toggleChecklistItem(itemId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleChecklistItem(itemId)
        }
    }

    fun resetAllChecklists() {
        viewModelScope.launch {
            preferencesRepository.clearChecklistItems(checklistItems.map { it.id })
        }
    }

    fun updateSubjectEstimate(subjectName: String, score: Int) {
        val current = customEstimates.value.toMutableMap()
        current[subjectName] = score.coerceIn(0, 100)
        customEstimates.value = current
    }

    fun syncEstimatesFromPerformance() {
        val updated = customEstimates.value.toMutableMap()
        uiState.value.subjectProgress.forEach { row ->
            if (row.attemptsTotal > 0 && row.subjectName in updated) {
                val acc = ((row.correctTotal * 100.0) / row.attemptsTotal).toInt().coerceIn(0, 100)
                updated[row.subjectName] = acc
            }
        }
        customEstimates.value = updated
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Quint<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
