package com.example.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.*
import com.example.domain.AnswerValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class MockExamLobbyState(
    val subjects: List<SubjectEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val inProgressExam: InProgressExamEntity? = null,
    val history: List<ExamResultEntity> = emptyList(),
    val selectedSubjectIds: Set<Long> = emptySet(),
    val selectedGradeId: Long? = null,
    val selectedDifficulty: String? = null,
    val questionCount: Int = 20,
    val durationMinutes: Int = 45,
    val examTitle: String = "EUEE Mock Examination"
)

data class ActiveExamUiState(
    val title: String = "",
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Long, String> = emptyMap(),
    val flaggedIds: Set<Long> = emptySet(),
    val durationMinutes: Int = 45,
    val remainingSeconds: Long = 0L,
    val startedAtMs: Long = System.currentTimeMillis(),
    val keepScreenAwake: Boolean = true,
    val completedResultId: Long? = null
) {
    val currentQuestion: QuestionEntity? get() = questions.getOrNull(currentIndex)
    val unansweredCount: Int get() = questions.count { selectedAnswers[it.id].isNullOrBlank() }
}

class MockExamViewModel(
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedSubjectIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedGradeId = MutableStateFlow<Long?>(null)
    private val selectedDifficulty = MutableStateFlow<String?>(null)
    private val questionCount = MutableStateFlow(20)
    private val durationMinutes = MutableStateFlow(45)
    private val examTitle = MutableStateFlow("EUEE Mock Examination")

    val lobbyState: StateFlow<MockExamLobbyState> = combine(
        repository.observeSubjects(),
        repository.observeGrades(),
        repository.observeInProgressExam(),
        repository.observeExamResults(),
        combine(selectedSubjectIds, selectedGradeId, selectedDifficulty, questionCount, durationMinutes) { s, g, d, c, dur ->
            listOf(s, g, d, c, dur)
        },
        examTitle
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val subs = arr[0] as List<SubjectEntity>
        @Suppress("UNCHECKED_CAST")
        val grades = arr[1] as List<GradeEntity>
        val inProg = arr[2] as InProgressExamEntity?
        @Suppress("UNCHECKED_CAST")
        val hist = arr[3] as List<ExamResultEntity>
        @Suppress("UNCHECKED_CAST")
        val cfg = arr[4] as List<Any?>
        val title = arr[5] as String

        @Suppress("UNCHECKED_CAST")
        MockExamLobbyState(
            subjects = subs,
            grades = grades,
            inProgressExam = inProg,
            history = hist,
            selectedSubjectIds = cfg[0] as Set<Long>,
            selectedGradeId = cfg[1] as Long?,
            selectedDifficulty = cfg[2] as String?,
            questionCount = cfg[3] as Int,
            durationMinutes = cfg[4] as Int,
            examTitle = title
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MockExamLobbyState())

    private val _activeExam = MutableStateFlow(ActiveExamUiState())
    val activeExam: StateFlow<ActiveExamUiState> = _activeExam.asStateFlow()

    private var timerJob: Job? = null

    fun toggleSubject(id: Long) {
        selectedSubjectIds.update { if (id in it) it - id else it + id }
    }
    fun selectGrade(id: Long?) { selectedGradeId.value = id }
    fun selectDifficulty(d: String?) { selectedDifficulty.value = d }
    fun setQuestionCount(c: Int) { questionCount.value = c.coerceIn(5, 120) }
    fun setDurationMinutes(m: Int) { durationMinutes.value = m.coerceIn(5, 240) }
    fun setExamTitle(t: String) { examTitle.value = t }

    fun startNewExam(onStarted: () -> Unit) {
        viewModelScope.launch {
            val cfg = lobbyState.value
            val prefs = preferencesRepository.preferencesFlow.first()
            val all = if (cfg.selectedSubjectIds.isEmpty()) {
                repository.getQuestionsFiltered(null, cfg.selectedGradeId, null, null, cfg.selectedDifficulty, 400)
            } else {
                cfg.selectedSubjectIds.flatMap { subId ->
                    repository.getQuestionsFiltered(subId, cfg.selectedGradeId, null, null, cfg.selectedDifficulty, 200)
                }
            }
            val chosen = all.shuffled().take(cfg.questionCount)
            if (chosen.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val durationSec = cfg.durationMinutes * 60L
            val title = cfg.examTitle.ifBlank { "EUEE Mock Examination" }

            val inProg = InProgressExamEntity(
                id = 1L,
                title = title,
                subjectIdsJson = JSONArray(cfg.selectedSubjectIds.toList()).toString(),
                questionIdsJson = JSONArray(chosen.map { it.id }).toString(),
                selectedAnswersJson = "{}",
                flaggedIdsJson = "[]",
                durationMinutes = cfg.durationMinutes,
                remainingSeconds = durationSec,
                currentIndex = 0,
                startedAt = now,
                updatedAt = now
            )
            repository.saveInProgressExam(inProg)

            _activeExam.value = ActiveExamUiState(
                title = title,
                questions = chosen,
                currentIndex = 0,
                selectedAnswers = emptyMap(),
                flaggedIds = emptySet(),
                durationMinutes = cfg.durationMinutes,
                remainingSeconds = durationSec,
                startedAtMs = now,
                keepScreenAwake = prefs.keepScreenAwakeInExam
            )
            startCountdown()
            onStarted()
        }
    }

    fun resumeSavedExam(onResumed: () -> Unit) {
        viewModelScope.launch {
            val saved = repository.getInProgressExam() ?: return@launch
            val prefs = preferencesRepository.preferencesFlow.first()
            val arr = runCatching { JSONArray(saved.questionIdsJson) }.getOrDefault(JSONArray())
            val ids = List(arr.length()) { arr.optLong(it) }
            val byId = repository.getQuestionsByIds(ids).associateBy { it.id }
            val ordered = ids.mapNotNull { byId[it] }

            val ansObj = runCatching { JSONObject(saved.selectedAnswersJson) }.getOrDefault(JSONObject())
            val ansMap = mutableMapOf<Long, String>()
            ansObj.keys().forEach { k -> k.toLongOrNull()?.let { ansMap[it] = ansObj.optString(k) } }

            val flagArr = runCatching { JSONArray(saved.flaggedIdsJson) }.getOrDefault(JSONArray())
            val flagged = List(flagArr.length()) { flagArr.optLong(it) }.toSet()

            _activeExam.value = ActiveExamUiState(
                title = saved.title,
                questions = ordered,
                currentIndex = saved.currentIndex.coerceIn(0, (ordered.size - 1).coerceAtLeast(0)),
                selectedAnswers = ansMap,
                flaggedIds = flagged,
                durationMinutes = saved.durationMinutes,
                remainingSeconds = saved.remainingSeconds.coerceAtLeast(1L),
                startedAtMs = saved.startedAt,
                keepScreenAwake = prefs.keepScreenAwakeInExam
            )
            startCountdown()
            onResumed()
        }
    }

    fun selectAnswer(questionId: Long, answer: String) {
        _activeExam.update { it.copy(selectedAnswers = it.selectedAnswers + (questionId to answer)) }
        persistInProgress()
    }

    fun toggleFlag(questionId: Long) {
        _activeExam.update { state ->
            val updated = if (questionId in state.flaggedIds) state.flaggedIds - questionId else state.flaggedIds + questionId
            state.copy(flaggedIds = updated)
        }
        persistInProgress()
    }

    fun jumpToQuestion(index: Int) {
        _activeExam.update { state ->
            state.copy(currentIndex = index.coerceIn(0, (state.questions.size - 1).coerceAtLeast(0)))
        }
        persistInProgress()
    }

    fun submitExam(onResultReady: (Long) -> Unit = {}) {
        timerJob?.cancel()
        val state = _activeExam.value
        if (state.questions.isEmpty()) return

        viewModelScope.launch {
            val subjectsById = repository.observeSubjects().first().associateBy { it.id }
            var correctCount = 0
            var answeredCount = 0
            val incorrectIds = mutableListOf<Long>()
            val topicTallies = mutableMapOf<String, Pair<Int, Int>>() // topic -> (correct, total)

            state.questions.forEach { q ->
                val userAns = state.selectedAnswers[q.id].orEmpty()
                if (userAns.isNotBlank()) answeredCount++
                val eval = AnswerValidator.evaluate(q, userAns)
                if (eval.isCorrect) {
                    correctCount++
                } else {
                    incorrectIds.add(q.id)
                }

                // Record attempt for SRS & Mistake Notebook
                if (userAns.isNotBlank()) {
                    repository.recordAttempt(
                        question = q,
                        userAnswer = userAns,
                        isCorrect = eval.isCorrect,
                        responseTimeMs = 10_000L,
                        hintsUsed = 0,
                        confidenceRating = if (eval.isCorrect) 4 else 2,
                        errorCategory = if (eval.isCorrect) "" else "EXAM_SIM"
                    )
                }

                val subLabel = subjectsById[q.subjectId]?.name ?: "General"
                val prev = topicTallies[subLabel] ?: (0 to 0)
                topicTallies[subLabel] = (prev.first + if (eval.isCorrect) 1 else 0) to (prev.second + 1)
            }

            val breakdownArr = JSONArray()
            topicTallies.forEach { (subject, pair) ->
                breakdownArr.put(JSONObject().apply {
                    put("subject", subject)
                    put("correct", pair.first)
                    put("total", pair.second)
                })
            }

            val ansObj = JSONObject()
            state.selectedAnswers.forEach { (k, v) -> ansObj.put(k.toString(), v) }

            val timeTaken = ((state.durationMinutes * 60L) - state.remainingSeconds).coerceAtLeast(1L)
            val scorePct = (correctCount * 100.0) / state.questions.size

            val resultId = repository.saveExamResult(
                ExamResultEntity(
                    title = state.title,
                    subjectIdsJson = "[]",
                    totalQuestions = state.questions.size,
                    answeredCount = answeredCount,
                    correctCount = correctCount,
                    durationMinutes = state.durationMinutes,
                    timeTakenSeconds = timeTaken,
                    scorePercent = scorePct,
                    topicBreakdownJson = breakdownArr.toString(),
                    incorrectQuestionIdsJson = JSONArray(incorrectIds).toString(),
                    answerMapJson = ansObj.toString()
                )
            )

            repository.clearInProgressExam()
            _activeExam.update { it.copy(completedResultId = resultId) }
            onResultReady(resultId)
        }
    }

    suspend fun loadExamResult(resultId: Long): ExamResultEntity? = repository.getExamResultById(resultId)
    suspend fun loadQuestions(ids: List<Long>): List<QuestionEntity> = repository.getQuestionsByIds(ids)

    private fun persistInProgress() {
        val state = _activeExam.value
        if (state.questions.isEmpty()) return
        viewModelScope.launch {
            val ansObj = JSONObject()
            state.selectedAnswers.forEach { (k, v) -> ansObj.put(k.toString(), v) }
            repository.saveInProgressExam(
                InProgressExamEntity(
                    id = 1L,
                    title = state.title,
                    subjectIdsJson = "[]",
                    questionIdsJson = JSONArray(state.questions.map { it.id }).toString(),
                    selectedAnswersJson = ansObj.toString(),
                    flaggedIdsJson = JSONArray(state.flaggedIds.toList()).toString(),
                    durationMinutes = state.durationMinutes,
                    remainingSeconds = state.remainingSeconds,
                    currentIndex = state.currentIndex,
                    startedAt = state.startedAtMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_activeExam.value.remainingSeconds > 0 && _activeExam.value.completedResultId == null) {
                delay(1000L)
                _activeExam.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0L)) }
                if (_activeExam.value.remainingSeconds % 5L == 0L) persistInProgress()
                if (_activeExam.value.remainingSeconds == 0L) {
                    submitExam()
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
