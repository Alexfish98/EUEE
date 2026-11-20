package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SubjectProgressRow(
    val subjectId: Long,
    val subjectName: String,
    val colorHex: Long,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val correctTotal: Int,
    val attemptsTotal: Int,
    val masteredCount: Int
)

data class TopicPerformanceRow(
    val topicId: Long,
    val topicTitle: String,
    val subjectName: String,
    val totalQuestions: Int,
    val attemptsTotal: Int,
    val correctTotal: Int
)

data class DailyActivityRow(
    val epochDay: Long,
    val attemptsCount: Int,
    val correctCount: Int,
    val totalTimeMs: Long
)

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM subjects ORDER BY isCoreEuee DESC, id ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY isCoreEuee DESC, id ASC")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("SELECT * FROM grades ORDER BY level ASC")
    fun observeGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades ORDER BY level ASC")
    suspend fun getAllGrades(): List<GradeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Query("SELECT * FROM units WHERE (:subjectId IS NULL OR subjectId = :subjectId) AND (:gradeId IS NULL OR gradeId = :gradeId) ORDER BY unitNumber ASC")
    fun observeUnits(subjectId: Long?, gradeId: Long?): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units ORDER BY subjectId, gradeId, unitNumber")
    suspend fun getAllUnits(): List<UnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Query("SELECT * FROM topics WHERE (:unitId IS NULL OR unitId = :unitId) ORDER BY id ASC")
    fun observeTopics(unitId: Long?): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    suspend fun getAllTopics(): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Query("SELECT * FROM question_banks ORDER BY id ASC")
    fun observeQuestionBanks(): Flow<List<QuestionBankEntity>>

    @Query("SELECT * FROM question_banks ORDER BY id ASC")
    suspend fun getAllQuestionBanks(): List<QuestionBankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionBank(bank: QuestionBankEntity): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)
}

@Dao
interface QuestionDao {
    @Query("SELECT COUNT(*) FROM questions")
    fun observeTotalQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Query("""
        SELECT * FROM questions
        WHERE (:subjectId IS NULL OR subjectId = :subjectId)
          AND (:gradeId IS NULL OR gradeId = :gradeId)
          AND (:unitId IS NULL OR unitId = :unitId)
          AND (:topicId IS NULL OR topicId = :topicId)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
          AND (:bookmarkedOnly = 0 OR isBookmarked = 1)
          AND (:query = '' OR questionText LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%' OR uniqueCode LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryQuestionsPaged(
        subjectId: Long?,
        gradeId: Long?,
        unitId: Long?,
        topicId: Long?,
        difficulty: String?,
        bookmarkedOnly: Boolean,
        query: String,
        limit: Int,
        offset: Int
    ): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE (:subjectId IS NULL OR subjectId = :subjectId)
          AND (:gradeId IS NULL OR gradeId = :gradeId)
          AND (:unitId IS NULL OR unitId = :unitId)
          AND (:topicId IS NULL OR topicId = :topicId)
          AND (:difficulty IS NULL OR difficulty = :difficulty)
        ORDER BY id ASC
        LIMIT :limit
    """)
    suspend fun getQuestionsFiltered(
        subjectId: Long?,
        gradeId: Long?,
        unitId: Long?,
        topicId: Long?,
        difficulty: String?,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT * FROM questions ORDER BY id ASC")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT uniqueCode FROM questions")
    suspend fun getAllUniqueCodes(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long>

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("UPDATE questions SET isBookmarked = :bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setBookmarked(id: Long, bookmarked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM questions WHERE id IN (:ids)")
    suspend fun deleteQuestionsByIds(ids: List<Long>)

    @Query("DELETE FROM questions WHERE bankId = :bankId")
    suspend fun deleteQuestionsByBank(bankId: Long)

    @Query("UPDATE questions SET difficulty = :difficulty, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun bulkUpdateDifficulty(ids: List<Long>, difficulty: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getDistractorPool(subjectId: Long, limit: Int = 20): List<QuestionEntity>
}

@Dao
interface LearningProgressDao {
    @Query("SELECT * FROM question_progress")
    fun observeAllProgress(): Flow<List<QuestionProgressEntity>>

    @Query("SELECT * FROM question_progress WHERE questionId = :questionId")
    suspend fun getProgressForQuestion(questionId: Long): QuestionProgressEntity?

    @Query("SELECT * FROM question_progress")
    suspend fun getAllProgress(): List<QuestionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: QuestionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressBatch(items: List<QuestionProgressEntity>)

    @Query("SELECT COUNT(*) FROM question_progress WHERE attempts > 0")
    fun observeAttemptedQuestionCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(correctCount), 0) FROM question_progress")
    fun observeTotalCorrectAnswers(): Flow<Int>

    @Query("SELECT COALESCE(SUM(incorrectCount), 0) FROM question_progress")
    fun observeTotalIncorrectAnswers(): Flow<Int>

    @Query("SELECT COUNT(*) FROM question_progress WHERE masteryStatus = 'MASTERED'")
    fun observeMasteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM question_progress WHERE masteryStatus IN ('LEARNING', 'REVIEWING')")
    fun observeLearningCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM question_progress WHERE nextReviewAt > 0 AND nextReviewAt <= :now AND masteryStatus != 'MASTERED'")
    fun observeDueReviewCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM question_progress WHERE incorrectCount > correctCount")
    fun observeIncorrectAttentionCount(): Flow<Int>

    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN question_progress p ON q.id = p.questionId
        WHERE p.id IS NULL OR (p.nextReviewAt <= :now AND p.masteryStatus != 'MASTERED')
        ORDER BY
          CASE WHEN p.incorrectCount > p.correctCount THEN 0 ELSE 1 END,
          COALESCE(p.confidence, 0) ASC,
          COALESCE(p.nextReviewAt, 0) ASC
        LIMIT :limit
    """)
    suspend fun getSmartQueue(now: Long, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN question_progress p ON q.id = p.questionId
        WHERE p.attempts > 0
        ORDER BY p.lastReviewedAt DESC
        LIMIT :limit
    """)
    suspend fun getReviewableQuestions(limit: Int): List<QuestionEntity>

    @Query("""
        SELECT s.id AS subjectId, s.name AS subjectName, s.colorHex AS colorHex,
               COUNT(q.id) AS totalQuestions,
               COUNT(CASE WHEN p.attempts > 0 THEN 1 END) AS attemptedQuestions,
               COALESCE(SUM(p.correctCount), 0) AS correctTotal,
               COALESCE(SUM(p.attempts), 0) AS attemptsTotal,
               COUNT(CASE WHEN p.masteryStatus = 'MASTERED' THEN 1 END) AS masteredCount
        FROM subjects s
        LEFT JOIN questions q ON q.subjectId = s.id
        LEFT JOIN question_progress p ON p.questionId = q.id
        GROUP BY s.id
        ORDER BY s.isCoreEuee DESC, s.id ASC
    """)
    fun observeSubjectProgress(): Flow<List<SubjectProgressRow>>

    @Query("""
        SELECT t.id AS topicId, t.title AS topicTitle, s.name AS subjectName,
               COUNT(q.id) AS totalQuestions,
               COALESCE(SUM(p.attempts), 0) AS attemptsTotal,
               COALESCE(SUM(p.correctCount), 0) AS correctTotal
        FROM topics t
        INNER JOIN units u ON t.unitId = u.id
        INNER JOIN subjects s ON u.subjectId = s.id
        LEFT JOIN questions q ON q.topicId = t.id
        LEFT JOIN question_progress p ON p.questionId = q.id
        GROUP BY t.id
        HAVING attemptsTotal > 0
        ORDER BY (CAST(correctTotal AS REAL) / attemptsTotal) ASC
        LIMIT :limit
    """)
    fun observeWeakTopics(limit: Int = 8): Flow<List<TopicPerformanceRow>>

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts ORDER BY attemptedAt DESC LIMIT :limit")
    fun observeRecentAttempts(limit: Int = 50): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts ORDER BY attemptedAt DESC")
    suspend fun getAllAttempts(): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE attemptedAt >= :sinceMillis ORDER BY attemptedAt ASC")
    fun observeAttemptsSince(sinceMillis: Long): Flow<List<AttemptEntity>>

    @Query("SELECT COALESCE(SUM(responseTimeMs), 0) FROM attempts WHERE attemptedAt >= :sinceMillis")
    fun observeStudyTimeSince(sinceMillis: Long): Flow<Long>
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<StudySessionEntity?>

    @Query("SELECT * FROM study_sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 10): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: StudySessionEntity): Long
}

@Dao
interface NotebookDao {
    @Query("""
        SELECT * FROM notes
        WHERE (:subjectId IS NULL OR subjectId = :subjectId)
          AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun observeNotes(subjectId: Long?, query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE linkedQuestionId = :questionId LIMIT 1")
    suspend fun getNoteForQuestion(questionId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM flashcards WHERE (:subjectId IS NULL OR subjectId = :subjectId) ORDER BY nextReviewAt ASC, createdAt DESC")
    fun observeFlashcards(subjectId: Long?): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    suspend fun getAllFlashcards(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcard(card: FlashcardEntity): Long

    @Delete
    suspend fun deleteFlashcard(card: FlashcardEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_results ORDER BY completedAt DESC")
    fun observeExamResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results ORDER BY completedAt DESC")
    suspend fun getAllExamResults(): List<ExamResultEntity>

    @Query("SELECT * FROM exam_results WHERE id = :id")
    suspend fun getExamResultById(id: Long): ExamResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResult(result: ExamResultEntity): Long

    @Query("SELECT * FROM in_progress_exams WHERE id = 1")
    fun observeInProgressExam(): Flow<InProgressExamEntity?>

    @Query("SELECT * FROM in_progress_exams WHERE id = 1")
    suspend fun getInProgressExam(): InProgressExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveInProgressExam(exam: InProgressExamEntity)

    @Query("DELETE FROM in_progress_exams WHERE id = 1")
    suspend fun clearInProgressExam()
}

@Dao
interface PlannerAndAlarmDao {
    @Query("SELECT * FROM study_tasks WHERE scheduledDateEpochDay = :epochDay ORDER BY isCompleted ASC, startTimeMinutes ASC")
    fun observeTasksForDate(epochDay: Long): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE scheduledDateEpochDay BETWEEN :startDay AND :endDay ORDER BY scheduledDateEpochDay ASC, startTimeMinutes ASC")
    fun observeTasksInRange(startDay: Long, endDay: Long): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY scheduledDateEpochDay DESC, startTimeMinutes ASC")
    suspend fun getAllTasks(): List<StudyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: StudyTaskEntity): Long

    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun observeAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlarm(alarm: AlarmEntity): Long

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("SELECT * FROM alarm_history ORDER BY triggeredAt DESC LIMIT 50")
    fun observeAlarmHistory(): Flow<List<AlarmHistoryEntity>>

    @Insert
    suspend fun insertAlarmHistory(entry: AlarmHistoryEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC LIMIT 40")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun getAllFocusSessions(): List<FocusSessionEntity>

    @Query("SELECT COALESCE(SUM(actualMinutes), 0) FROM focus_sessions WHERE startedAt >= :sinceMillis")
    fun observeFocusMinutesSince(sinceMillis: Long): Flow<Int>

    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity): Long
}
