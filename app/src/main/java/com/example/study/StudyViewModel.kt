package com.example.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AnswerEvaluation
import com.example.core.model.StudyMode
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

data class StudyLibraryState(
    val subjects: List<SubjectEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val selectedGradeId: Long? = null,
    val selectedUnitId: Long? = null,
    val selectedTopicId: Long? = null,
    val previewQuestions: List<QuestionEntity> = emptyList()
)

data class StudySessionUiState(
    val sessionId: Long = 0L,
    val mode: StudyMode = StudyMode.PRACTICE,
    val title: String = "Practice Session",
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val currentOptions: List<String> = emptyList(),
    val userInput: String = "",
    val isAnswerRevealed: Boolean = false,
    val evaluation: AnswerEvaluation? = null,
    val hintsRevealed: Int = 0,
    val confidenceRating: Int = 3,
    val selectedErrorCategory: String = "CONCEPT_GAP",
    val isCardFlipped: Boolean = false,
    val correctCount: Int = 0,
    val attemptedCount: Int = 0,
    val remainingSeconds: Long = 0L,
    val isTimedMode: Boolean = false,
    val isCompleted: Boolean = false,
    val questionStartedAtMs: Long = System.currentTimeMillis(),
    val keepScreenAwake: Boolean = true,
    val speechRate: Float = 1.0f,
    val vibrationEnabled: Boolean = true
) {
    val currentQuestion: QuestionEntity? get() = questions.getOrNull(currentIndex)
}

class StudyViewModel(
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedSubjectId = MutableStateFlow<Long?>(null)
    private val selectedGradeId = MutableStateFlow<Long?>(null)
    private val selectedUnitId = MutableStateFlow<Long?>(null)
    private val selectedTopicId = MutableStateFlow<Long?>(null)
    private val previewQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())

    val libraryState: StateFlow<StudyLibraryState> = combine(
        repository.observeSubjects(),
        repository.observeGrades(),
        repository.observeUnits(),
        repository.observeTopics(),
        combine(selectedSubjectId, selectedGradeId, selectedUnitId, selectedTopicId, previewQuestions) { s, g, u, t, q ->
            listOf(s, g, u, t, q)
        }
    ) { subjects, grades, units, topics, sel ->
        val sId = sel[0] as Long?
        val gId = sel[1] as Long?
        val uId = sel[2] as Long?
        val tId = sel[3] as Long?
        @Suppress("UNCHECKED_CAST")
        val qList = sel[4] as List<QuestionEntity>
        StudyLibraryState(
            subjects = subjects,
            grades = grades,
            units = units.filter { (sId == null || it.subjectId == sId) && (gId == null || it.gradeId == gId) },
            topics = topics.filter { uId == null || it.unitId == uId },
            selectedSubjectId = sId,
            selectedGradeId = gId,
            selectedUnitId = uId,
            selectedTopicId = tId,
            previewQuestions = qList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyLibraryState())

    private val _sessionState = MutableStateFlow(StudySessionUiState())
    val sessionState: StateFlow<StudySessionUiState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null
    private val answerLog = mutableMapOf<Long, String>()

    init {
        refreshPreviewQuestions()
    }

    fun selectSubject(id: Long?) {
        selectedSubjectId.value = id
        selectedUnitId.value = null
        selectedTopicId.value = null
        refreshPreviewQuestions()
    }

    fun selectGrade(id: Long?) {
        selectedGradeId.value = id
        selectedUnitId.value = null
        selectedTopicId.value = null
        refreshPreviewQuestions()
    }

    fun selectUnit(id: Long?) {
        selectedUnitId.value = id
        selectedTopicId.value = null
        refreshPreviewQuestions()
    }

    fun selectTopic(id: Long?) {
        selectedTopicId.value = id
        refreshPreviewQuestions()
    }

    fun refreshPreviewQuestions() {
        viewModelScope.launch {
            previewQuestions.value = repository.queryQuestionsPaged(
                subjectId = selectedSubjectId.value,
                gradeId = selectedGradeId.value,
                unitId = selectedUnitId.value,
                topicId = selectedTopicId.value,
                limit = 40,
                offset = 0
            )
        }
    }

    fun addCustomSubject(name: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addSubject(name, category)
            selectSubject(id)
        }
    }

    fun addCustomUnit(subjectId: Long, gradeId: Long, unitNumber: Int, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.addUnit(subjectId, gradeId, unitNumber, title)
            selectUnit(id)
        }
    }

    fun addCustomTopic(unitId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.addTopic(unitId, title)
            selectTopic(id)
        }
    }

    fun startSession(
        mode: StudyMode,
        subjectId: Long? = selectedSubjectId.value,
        gradeId: Long? = selectedGradeId.value,
        unitId: Long? = selectedUnitId.value,
        topicId: Long? = selectedTopicId.value,
        timedDurationMinutes: Int = 15,
        questionLimit: Int = 20
    ) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val pool = when (mode) {
                StudyMode.SMART_STUDY -> repository.getSmartStudyQueue(
                    (prefs.srsDailyNewLimit + prefs.srsDailyReviewLimit).coerceAtMost(60)
                ).ifEmpty { repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, questionLimit) }
                StudyMode.REVIEW -> repository.getReviewableQuestions(questionLimit)
                    .ifEmpty { repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, questionLimit) }
                StudyMode.WEAK_TOPICS -> repository.getSmartStudyQueue(questionLimit)
                    .ifEmpty { repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, questionLimit) }
                StudyMode.RANDOM_PRACTICE -> repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, 200).shuffled().take(questionLimit)
                StudyMode.RAPID_REVISION -> repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, 200).shuffled().take(10)
                else -> repository.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, null, questionLimit)
            }

            answerLog.clear()
            val isTimed = mode == StudyMode.TIMED_EXAM
            val idsJson = JSONArray(pool.map { it.id }).toString()
            val sessionId = if (pool.isNotEmpty()) {
                repository.saveStudySession(
                    StudySessionEntity(
                        mode = mode.name,
                        title = "${mode.title} Drill",
                        subjectId = subjectId,
                        gradeId = gradeId,
                        unitId = unitId,
                        topicId = topicId,
                        questionIdsJson = idsJson
                    )
                )
            } else 0L

            val firstOptions = pool.firstOrNull()?.let { buildOptionsFor(it, mode) }.orEmpty()
            _sessionState.value = StudySessionUiState(
                sessionId = sessionId,
                mode = mode,
                title = "${mode.title} Session",
                questions = pool,
                currentIndex = 0,
                currentOptions = firstOptions,
                remainingSeconds = if (isTimed) timedDurationMinutes * 60L else 0L,
                isTimedMode = isTimed,
                keepScreenAwake = prefs.keepScreenAwakeInStudy,
                speechRate = prefs.speechRate,
                vibrationEnabled = prefs.vibrationEnabled,
                questionStartedAtMs = System.currentTimeMillis()
            )
            if (isTimed && pool.isNotEmpty()) startSessionTimer() else timerJob?.cancel()
        }
    }

    fun resumeSession(sessionId: Long) {
        viewModelScope.launch {
            val entity = repository.getStudySessionById(sessionId) ?: return@launch
            val prefs = preferencesRepository.preferencesFlow.first()
            val arr = runCatching { JSONArray(entity.questionIdsJson) }.getOrDefault(JSONArray())
            val ids = List(arr.length()) { arr.optLong(it) }
            val loadedMap = repository.getQuestionsByIds(ids).associateBy { it.id }
            val ordered = ids.mapNotNull { loadedMap[it] }
            val mode = runCatching { StudyMode.valueOf(entity.mode) }.getOrDefault(StudyMode.PRACTICE)
            val safeIndex = entity.currentIndex.coerceIn(0, (ordered.size - 1).coerceAtLeast(0))
            val opts = ordered.getOrNull(safeIndex)?.let { buildOptionsFor(it, mode) }.orEmpty()

            _sessionState.value = StudySessionUiState(
                sessionId = entity.id,
                mode = mode,
                title = entity.title,
                questions = ordered,
                currentIndex = safeIndex,
                currentOptions = opts,
                correctCount = entity.correctCount,
                attemptedCount = entity.attemptedCount,
                keepScreenAwake = prefs.keepScreenAwakeInStudy,
                speechRate = prefs.speechRate,
                vibrationEnabled = prefs.vibrationEnabled,
                questionStartedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun updateUserInput(text: String) {
        _sessionState.update { it.copy(userInput = text) }
    }

    fun revealHint() {
        _sessionState.update { state ->
            val maxHints = state.currentQuestion?.let { AnswerValidator.parseStringArrayJson(it.hintsJson).size } ?: 0
            state.copy(hintsRevealed = (state.hintsRevealed + 1).coerceAtMost(maxHints))
        }
    }

    fun flipFlashcard() {
        _sessionState.update { it.copy(isCardFlipped = !it.isCardFlipped, isAnswerRevealed = true) }
    }

    fun setConfidence(rating: Int) {
        _sessionState.update { it.copy(confidenceRating = rating.coerceIn(1, 5)) }
    }

    fun setErrorCategory(category: String) {
        _sessionState.update { it.copy(selectedErrorCategory = category) }
    }

    fun submitAnswer(selectedOrTyped: String) {
        val state = _sessionState.value
        val q = state.currentQuestion ?: return
        if (state.isAnswerRevealed) return

        val eval = AnswerValidator.evaluate(q, selectedOrTyped)
        val elapsedMs = (System.currentTimeMillis() - state.questionStartedAtMs).coerceAtLeast(500L)
        answerLog[q.id] = selectedOrTyped

        val newCorrect = state.correctCount + if (eval.isCorrect) 1 else 0
        val newAttempted = state.attemptedCount + 1

        _sessionState.update {
            it.copy(
                userInput = selectedOrTyped,
                isAnswerRevealed = true,
                evaluation = eval,
                correctCount = newCorrect,
                attemptedCount = newAttempted
            )
        }

        viewModelScope.launch {
            repository.recordAttempt(
                question = q,
                userAnswer = selectedOrTyped,
                isCorrect = eval.isCorrect,
                responseTimeMs = elapsedMs,
                hintsUsed = state.hintsRevealed,
                confidenceRating = state.confidenceRating,
                sessionId = state.sessionId,
                errorCategory = state.selectedErrorCategory
            )
            persistSessionProgress()
        }
    }

    fun revealActiveRecallAnswer() {
        _sessionState.update { it.copy(isAnswerRevealed = true) }
    }

    fun recordSelfGradedRecall(knewIt: Boolean) {
        val state = _sessionState.value
        val q = state.currentQuestion ?: return
        val elapsedMs = (System.currentTimeMillis() - state.questionStartedAtMs).coerceAtLeast(500L)
        val answerStr = if (knewIt) q.correctAnswer else "(Self-marked incorrect)"
        answerLog[q.id] = answerStr

        val newCorrect = state.correctCount + if (knewIt) 1 else 0
        val newAttempted = state.attemptedCount + 1

        _sessionState.update {
            it.copy(
                isAnswerRevealed = true,
                evaluation = AnswerEvaluation(
                    isCorrect = knewIt,
                    normalizedUserAnswer = answerStr,
                    matchedAcceptedAnswer = q.correctAnswer,
                    feedbackNote = if (knewIt) "Recorded as recalled." else "Logged to notebook & spaced repetition."
                ),
                correctCount = newCorrect,
                attemptedCount = newAttempted
            )
        }

        viewModelScope.launch {
            repository.recordAttempt(
                question = q,
                userAnswer = answerStr,
                isCorrect = knewIt,
                responseTimeMs = elapsedMs,
                hintsUsed = state.hintsRevealed,
                confidenceRating = state.confidenceRating,
                sessionId = state.sessionId,
                errorCategory = state.selectedErrorCategory
            )
            persistSessionProgress()
            nextQuestion()
        }
    }

    fun toggleBookmarkCurrent() {
        val q = _sessionState.value.currentQuestion ?: return
        val newVal = !q.isBookmarked
        viewModelScope.launch {
            repository.setBookmarked(q.id, newVal)
            _sessionState.update { state ->
                val updatedList = state.questions.map { if (it.id == q.id) it.copy(isBookmarked = newVal) else it }
                state.copy(questions = updatedList)
            }
            refreshPreviewQuestions()
        }
    }

    fun nextQuestion() {
        val state = _sessionState.value
        if (state.currentIndex + 1 >= state.questions.size) {
            completeSession()
            return
        }
        val nextIdx = state.currentIndex + 1
        val nextQ = state.questions[nextIdx]
        viewModelScope.launch {
            val opts = buildOptionsFor(nextQ, state.mode)
            _sessionState.update {
                it.copy(
                    currentIndex = nextIdx,
                    currentOptions = opts,
                    userInput = "",
                    isAnswerRevealed = false,
                    evaluation = null,
                    hintsRevealed = 0,
                    isCardFlipped = false,
                    questionStartedAtMs = System.currentTimeMillis()
                )
            }
            persistSessionProgress()
        }
    }

    fun completeSession() {
        timerJob?.cancel()
        _sessionState.update { it.copy(isCompleted = true) }
        viewModelScope.launch {
            persistSessionProgress(status = "COMPLETED")
        }
    }

    private suspend fun persistSessionProgress(status: String = "ACTIVE") {
        val state = _sessionState.value
        if (state.sessionId <= 0L) return
        val existing = repository.getStudySessionById(state.sessionId) ?: return
        val ansObj = JSONObject()
        answerLog.forEach { (k, v) -> ansObj.put(k.toString(), v) }
        repository.saveStudySession(
            existing.copy(
                currentIndex = state.currentIndex,
                correctCount = state.correctCount,
                attemptedCount = state.attemptedCount,
                answersJson = ansObj.toString(),
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun buildOptionsFor(q: QuestionEntity, mode: StudyMode): List<String> {
        val base = AnswerValidator.parseStringArrayJson(q.optionsJson)
        if (base.isNotEmpty()) {
            return if (mode == StudyMode.MULTIPLE_CHOICE || mode == StudyMode.RANDOM_PRACTICE) base.shuffled() else base
        }
        if (mode == StudyMode.MULTIPLE_CHOICE) {
            val distractors = repository.getDistractorPool(q.subjectId)
                .map { it.correctAnswer }
                .filter { it.isNotBlank() && it != q.correctAnswer }
                .distinct()
                .take(3)
            return (distractors + q.correctAnswer).shuffled()
        }
        return emptyList()
    }

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_sessionState.value.remainingSeconds > 0 && !_sessionState.value.isCompleted) {
                delay(1000L)
                _sessionState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0L)) }
                if (_sessionState.value.remainingSeconds == 0L) {
                    completeSession()
                }
            }
        }
    }
}
