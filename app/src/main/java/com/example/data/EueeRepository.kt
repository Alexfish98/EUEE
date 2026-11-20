package com.example.data

import androidx.room.withTransaction
import com.example.database.*
import com.example.domain.SpacedRepetitionEngine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class EueeRepository(
    val db: EueeDatabase,
    private val preferencesRepository: PreferencesRepository
) {
    private val curriculum = db.curriculumDao()
    private val questions = db.questionDao()
    private val progress = db.learningProgressDao()
    private val sessions = db.studySessionDao()
    private val notebook = db.notebookDao()
    private val exams = db.examDao()
    private val plannerAndAlarm = db.plannerAndAlarmDao()

    suspend fun ensureSeeded() {
        SeedDataProvider.seedIfEmpty(db)
    }

    // Curriculum Flows
    fun observeSubjects(): Flow<List<SubjectEntity>> = curriculum.observeSubjects()
    fun observeGrades(): Flow<List<GradeEntity>> = curriculum.observeGrades()
    fun observeUnits(subjectId: Long? = null, gradeId: Long? = null): Flow<List<UnitEntity>> = curriculum.observeUnits(subjectId, gradeId)
    fun observeTopics(unitId: Long? = null): Flow<List<TopicEntity>> = curriculum.observeTopics(unitId)
    fun observeQuestionBanks(): Flow<List<QuestionBankEntity>> = curriculum.observeQuestionBanks()

    suspend fun addSubject(name: String, category: String = "Custom", colorHex: Long = 0xFF0284C7): Long =
        curriculum.insertSubject(SubjectEntity(name = name.trim(), category = category, colorHex = colorHex, iconName = "School", isCoreEuee = false))

    suspend fun addUnit(subjectId: Long, gradeId: Long, unitNumber: Int, title: String): Long =
        curriculum.insertUnit(UnitEntity(subjectId = subjectId, gradeId = gradeId, unitNumber = unitNumber, title = title.trim()))

    suspend fun addTopic(unitId: Long, title: String, summary: String = ""): Long =
        curriculum.insertTopic(TopicEntity(unitId = unitId, title = title.trim(), summary = summary.trim()))

    suspend fun createQuestionBank(name: String, description: String = "", sourceType: String = "USER_CREATED"): Long =
        curriculum.insertQuestionBank(QuestionBankEntity(name = name.trim(), description = description.trim(), sourceType = sourceType))

    // Dashboard & Analytics Flows
    fun observeTotalQuestionCount(): Flow<Int> = questions.observeTotalQuestionCount()
    fun observeAttemptedQuestionCount(): Flow<Int> = progress.observeAttemptedQuestionCount()
    fun observeTotalCorrect(): Flow<Int> = progress.observeTotalCorrectAnswers()
    fun observeTotalIncorrect(): Flow<Int> = progress.observeTotalIncorrectAnswers()
    fun observeMasteredCount(): Flow<Int> = progress.observeMasteredCount()
    fun observeLearningCount(): Flow<Int> = progress.observeLearningCount()
    fun observeDueReviewCount(now: Long = System.currentTimeMillis()): Flow<Int> = progress.observeDueReviewCount(now)
    fun observeIncorrectAttentionCount(): Flow<Int> = progress.observeIncorrectAttentionCount()
    fun observeSubjectProgress(): Flow<List<SubjectProgressRow>> = progress.observeSubjectProgress()
    fun observeWeakTopics(limit: Int = 8): Flow<List<TopicPerformanceRow>> = progress.observeWeakTopics(limit)
    fun observeAllProgress(): Flow<List<QuestionProgressEntity>> = progress.observeAllProgress()

    fun observeTodayStudyTimeMs(): Flow<Long> {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return progress.observeStudyTimeSince(startOfDay)
    }

    fun observeAttemptsLast30Days(): Flow<List<AttemptEntity>> {
        val thirtyDaysAgo = LocalDate.now().minusDays(29).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return progress.observeAttemptsSince(thirtyDaysAgo)
    }

    fun observeActiveStudySession(): Flow<StudySessionEntity?> = sessions.observeActiveSession()
    fun observeRecentSessions(limit: Int = 10): Flow<List<StudySessionEntity>> = sessions.observeRecentSessions(limit)

    // Question Queries & CRUD
    suspend fun queryQuestionsPaged(
        subjectId: Long? = null,
        gradeId: Long? = null,
        unitId: Long? = null,
        topicId: Long? = null,
        difficulty: String? = null,
        bookmarkedOnly: Boolean = false,
        query: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): List<QuestionEntity> = questions.queryQuestionsPaged(
        subjectId, gradeId, unitId, topicId, difficulty, bookmarkedOnly, query.trim(), limit, offset
    )

    suspend fun getQuestionById(id: Long): QuestionEntity? = questions.getQuestionById(id)
    suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity> =
        if (ids.isEmpty()) emptyList() else questions.getQuestionsByIds(ids)

    suspend fun getQuestionsFiltered(
        subjectId: Long? = null,
        gradeId: Long? = null,
        unitId: Long? = null,
        topicId: Long? = null,
        difficulty: String? = null,
        limit: Int = 500
    ): List<QuestionEntity> = questions.getQuestionsFiltered(subjectId, gradeId, unitId, topicId, difficulty, limit)

    suspend fun getSmartStudyQueue(limit: Int = 40): List<QuestionEntity> =
        progress.getSmartQueue(System.currentTimeMillis(), limit)

    suspend fun getReviewableQuestions(limit: Int = 40): List<QuestionEntity> =
        progress.getReviewableQuestions(limit)

    suspend fun getDistractorPool(subjectId: Long): List<QuestionEntity> =
        questions.getDistractorPool(subjectId, 20)

    suspend fun upsertQuestion(question: QuestionEntity): Long = questions.insertQuestion(question)

    suspend fun setBookmarked(questionId: Long, bookmarked: Boolean) =
        questions.setBookmarked(questionId, bookmarked)

    suspend fun deleteQuestions(ids: List<Long>) {
        if (ids.isNotEmpty()) questions.deleteQuestionsByIds(ids)
    }

    suspend fun bulkUpdateDifficulty(ids: List<Long>, difficulty: String) {
        if (ids.isNotEmpty()) questions.bulkUpdateDifficulty(ids, difficulty)
    }

    /**
     * Transaction-safe submission of a study question attempt:
     * - Records AttemptEntity
     * - Updates QuestionProgressEntity via SpacedRepetitionEngine
     * - Updates daily streak in DataStore
     * - Automatically adds/updates an entry in the Notebook if answered incorrectly
     */
    suspend fun recordAttempt(
        question: QuestionEntity,
        userAnswer: String,
        isCorrect: Boolean,
        responseTimeMs: Long,
        hintsUsed: Int,
        confidenceRating: Int,
        sessionId: Long = 0L,
        errorCategory: String = ""
    ): QuestionProgressEntity {
        val now = System.currentTimeMillis()
        val updatedProgress = db.withTransaction {
            val current = progress.getProgressForQuestion(question.id)
            val srs = SpacedRepetitionEngine.computeNextSchedule(
                current = current,
                isCorrect = isCorrect,
                confidenceRating = confidenceRating,
                hintsUsed = hintsUsed,
                nowMillis = now
            )

            val prevAttempts = current?.attempts ?: 0
            val newAttempts = prevAttempts + 1
            val prevAvg = current?.avgResponseTimeMs ?: 0L
            val newAvg = ((prevAvg * prevAttempts) + responseTimeMs) / newAttempts

            val entity = QuestionProgressEntity(
                id = current?.id ?: 0L,
                questionId = question.id,
                attempts = newAttempts,
                correctCount = (current?.correctCount ?: 0) + if (isCorrect) 1 else 0,
                incorrectCount = (current?.incorrectCount ?: 0) + if (!isCorrect) 1 else 0,
                confidence = confidenceRating.coerceIn(1, 5),
                nextReviewAt = srs.nextReviewAt,
                lastReviewedAt = now,
                intervalDays = srs.newIntervalDays,
                easeFactor = srs.newEaseFactor,
                masteryStatus = srs.newMasteryStatus.name,
                avgResponseTimeMs = newAvg,
                hintsUsedTotal = (current?.hintsUsedTotal ?: 0) + hintsUsed,
                consecutiveCorrect = srs.newStreak
            )
            progress.upsertProgress(entity)

            progress.insertAttempt(
                AttemptEntity(
                    questionId = question.id,
                    sessionId = sessionId,
                    isCorrect = isCorrect,
                    userAnswer = userAnswer,
                    responseTimeMs = responseTimeMs,
                    hintsUsed = hintsUsed,
                    confidenceRating = confidenceRating,
                    errorCategory = if (isCorrect) "" else errorCategory.ifBlank { "CONCEPT_GAP" },
                    attemptedAt = now
                )
            )

            // Automatically record incorrect questions into the Notebook for active review
            if (!isCorrect) {
                val existingNote = notebook.getNoteForQuestion(question.id)
                val noteContent = buildString {
                    appendLine("Question (${question.uniqueCode}):")
                    appendLine(question.questionText)
                    appendLine()
                    appendLine("Your answer: ${userAnswer.ifBlank { "(blank)" }}")
                    appendLine("Correct answer: ${question.correctAnswer}")
                    if (question.explanation.isNotBlank()) {
                        appendLine()
                        appendLine("Explanation: ${question.explanation}")
                    }
                    if (question.examTrap.isNotBlank()) {
                        appendLine("Exam Trap: ${question.examTrap}")
                    }
                }
                notebook.upsertNote(
                    NoteEntity(
                        id = existingNote?.id ?: 0L,
                        subjectId = question.subjectId,
                        topicId = question.topicId,
                        title = "Mistake Review: ${question.uniqueCode}",
                        content = noteContent,
                        isPinned = existingNote?.isPinned ?: false,
                        isMistakeEntry = true,
                        linkedQuestionId = question.id,
                        createdAt = existingNote?.createdAt ?: now,
                        updatedAt = now
                    )
                )
            }

            entity
        }

        preferencesRepository.recordDailyActivityForStreak()
        return updatedProgress
    }

    suspend fun saveStudySession(session: StudySessionEntity): Long = sessions.upsertSession(session)
    suspend fun getStudySessionById(id: Long): StudySessionEntity? = sessions.getSessionById(id)

    // Notebook & Flashcards
    fun observeNotes(subjectId: Long? = null, query: String = ""): Flow<List<NoteEntity>> =
        notebook.observeNotes(subjectId, query.trim())

    suspend fun upsertNote(note: NoteEntity): Long = notebook.upsertNote(note)
    suspend fun deleteNote(note: NoteEntity) = notebook.deleteNote(note)
    suspend fun getNoteById(id: Long): NoteEntity? = notebook.getNoteById(id)

    fun observeFlashcards(subjectId: Long? = null): Flow<List<FlashcardEntity>> = notebook.observeFlashcards(subjectId)
    suspend fun upsertFlashcard(card: FlashcardEntity): Long = notebook.upsertFlashcard(card)
    suspend fun deleteFlashcard(card: FlashcardEntity) = notebook.deleteFlashcard(card)

    // Exams
    fun observeExamResults(): Flow<List<ExamResultEntity>> = exams.observeExamResults()
    fun observeInProgressExam(): Flow<InProgressExamEntity?> = exams.observeInProgressExam()
    suspend fun getInProgressExam(): InProgressExamEntity? = exams.getInProgressExam()
    suspend fun saveInProgressExam(exam: InProgressExamEntity) = exams.saveInProgressExam(exam)
    suspend fun clearInProgressExam() = exams.clearInProgressExam()
    suspend fun saveExamResult(result: ExamResultEntity): Long = exams.insertExamResult(result)
    suspend fun getExamResultById(id: Long): ExamResultEntity? = exams.getExamResultById(id)

    // Planner
    fun observeTasksForDate(epochDay: Long): Flow<List<StudyTaskEntity>> = plannerAndAlarm.observeTasksForDate(epochDay)
    fun observeTasksInRange(startDay: Long, endDay: Long): Flow<List<StudyTaskEntity>> =
        plannerAndAlarm.observeTasksInRange(startDay, endDay)
    suspend fun upsertTask(task: StudyTaskEntity): Long = plannerAndAlarm.upsertTask(task)
    suspend fun deleteTask(task: StudyTaskEntity) = plannerAndAlarm.deleteTask(task)

    // Alarms & Focus
    fun observeAlarms(): Flow<List<AlarmEntity>> = plannerAndAlarm.observeAlarms()
    fun observeAlarmHistory(): Flow<List<AlarmHistoryEntity>> = plannerAndAlarm.observeAlarmHistory()
    suspend fun getEnabledAlarms(): List<AlarmEntity> = plannerAndAlarm.getEnabledAlarms()
    suspend fun getAlarmById(id: Long): AlarmEntity? = plannerAndAlarm.getAlarmById(id)
    suspend fun upsertAlarm(alarm: AlarmEntity): Long = plannerAndAlarm.upsertAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = plannerAndAlarm.deleteAlarm(alarm)
    suspend fun recordAlarmHistory(alarmId: Long, label: String, actionType: String) =
        plannerAndAlarm.insertAlarmHistory(AlarmHistoryEntity(alarmId = alarmId, label = label, actionType = actionType))

    fun observeFocusSessions(): Flow<List<FocusSessionEntity>> = plannerAndAlarm.observeFocusSessions()
    fun observeTodayFocusMinutes(): Flow<Int> {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return plannerAndAlarm.observeFocusMinutesSince(startOfDay)
    }
    suspend fun recordFocusSession(session: FocusSessionEntity): Long = plannerAndAlarm.insertFocusSession(session)

    /**
     * Batch import questions in chunks inside a transaction (handles 5,000+ records safely).
     */
    suspend fun batchImportQuestions(
        questionsToInsert: List<QuestionEntity>,
        replaceBankId: Long? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int {
        var inserted = 0
        db.withTransaction {
            if (replaceBankId != null) {
                questions.deleteQuestionsByBank(replaceBankId)
            }
            val chunkSize = 250
            val total = questionsToInsert.size
            for (i in questionsToInsert.indices step chunkSize) {
                val end = minOf(i + chunkSize, total)
                questions.insertQuestions(questionsToInsert.subList(i, end))
                inserted += (end - i)
                onProgress(inserted, total)
            }
        }
        return inserted
    }
}
