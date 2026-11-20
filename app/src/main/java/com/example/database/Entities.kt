package com.example.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.AlarmCategory
import com.example.core.model.Difficulty
import com.example.core.model.MasteryStatus
import com.example.core.model.QuestionType
import com.example.core.model.TaskPriority

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Natural Science" or "Additional" or "Custom"
    val colorHex: Long,
    val iconName: String,
    val isCoreEuee: Boolean = true
)

@Entity(
    tableName = "grades",
    indices = [Index(value = ["level"], unique = true)]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: Int,
    val label: String // e.g., "Grade 9"
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GradeEntity::class,
            parentColumns = ["id"],
            childColumns = ["gradeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("gradeId")]
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val gradeId: Long,
    val unitNumber: Int,
    val title: String
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("unitId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val title: String,
    val summary: String = ""
)

@Entity(tableName = "question_banks")
data class QuestionBankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val sourceType: String = "BUILT_IN", // BUILT_IN, USER_CREATED, IMPORTED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["uniqueCode"], unique = true),
        Index("bankId"),
        Index("subjectId"),
        Index("gradeId"),
        Index("unitId"),
        Index("topicId"),
        Index("difficulty"),
        Index("isBookmarked")
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uniqueCode: String,
    val bankId: Long = 1L,
    val subjectId: Long,
    val gradeId: Long,
    val unitId: Long,
    val topicId: Long,
    val difficulty: String = Difficulty.MEDIUM.name,
    val questionType: String = QuestionType.MULTIPLE_CHOICE.name,
    val questionText: String,
    val correctAnswer: String,
    val acceptedAnswersJson: String = "[]",
    val optionsJson: String = "[]",
    val hintsJson: String = "[]",
    val explanation: String = "",
    val whyCorrect: String = "",
    val commonMistakes: String = "",
    val conceptExplanation: String = "",
    val workedExample: String = "",
    val memoryTip: String = "",
    val examTrap: String = "",
    val imageUri: String? = null,
    val numericalTolerance: Double = 0.01,
    val referencesText: String = "",
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "question_progress",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionId"], unique = true), Index("nextReviewAt"), Index("masteryStatus")]
)
data class QuestionProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val confidence: Int = 0, // 1..5
    val nextReviewAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val intervalDays: Double = 0.0,
    val easeFactor: Double = 2.5,
    val masteryStatus: String = MasteryStatus.NEW.name,
    val avgResponseTimeMs: Long = 0L,
    val hintsUsedTotal: Int = 0,
    val consecutiveCorrect: Int = 0
)

@Entity(
    tableName = "attempts",
    indices = [Index("questionId"), Index("sessionId"), Index("attemptedAt")]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val sessionId: Long = 0L,
    val isCorrect: Boolean,
    val userAnswer: String,
    val responseTimeMs: Long = 0L,
    val hintsUsed: Int = 0,
    val confidenceRating: Int = 3,
    val errorCategory: String = "", // e.g. CONCEPT_GAP, CALCULATION, EXAM_TRAP, TIME_PRESSURE
    val attemptedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions", indices = [Index("status"), Index("startedAt")])
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val title: String,
    val subjectId: Long? = null,
    val gradeId: Long? = null,
    val unitId: Long? = null,
    val topicId: Long? = null,
    val questionIdsJson: String,
    val answersJson: String = "{}",
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val attemptedCount: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val durationSeconds: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [Index("subjectId"), Index("topicId"), Index("isPinned"), Index("updatedAt")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isMistakeEntry: Boolean = false,
    val linkedQuestionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    indices = [Index("subjectId"), Index("topicId"), Index("nextReviewAt")]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val front: String,
    val back: String,
    val hint: String = "",
    val sourceNoteId: Long? = null,
    val confidence: Int = 0, // 0..5
    val intervalDays: Double = 0.0,
    val nextReviewAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_results", indices = [Index("completedAt")])
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectIdsJson: String,
    val totalQuestions: Int,
    val answeredCount: Int,
    val correctCount: Int,
    val durationMinutes: Int,
    val timeTakenSeconds: Long,
    val scorePercent: Double,
    val topicBreakdownJson: String,
    val incorrectQuestionIdsJson: String,
    val answerMapJson: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "in_progress_exams")
data class InProgressExamEntity(
    @PrimaryKey val id: Long = 1L,
    val title: String,
    val subjectIdsJson: String,
    val questionIdsJson: String,
    val selectedAnswersJson: String = "{}",
    val flaggedIdsJson: String = "[]",
    val durationMinutes: Int,
    val remainingSeconds: Long,
    val currentIndex: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_tasks",
    indices = [Index("scheduledDateEpochDay"), Index("subjectId"), Index("isCompleted")]
)
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val subjectId: Long? = null,
    val unitId: Long? = null,
    val topicId: Long? = null,
    val scheduledDateEpochDay: Long,
    val startTimeMinutes: Int, // minutes from midnight (0..1439)
    val durationMinutes: Int = 45,
    val priority: String = TaskPriority.MEDIUM.name,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(tableName = "alarms", indices = [Index("isEnabled")])
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeekMask: Int = 0, // bitmask Sun=1, Mon=2, Tue=4, Wed=8, Thu=16, Fri=32, Sat=64
    val isRepeating: Boolean = false,
    val isEnabled: Boolean = true,
    val category: String = AlarmCategory.STUDY_REMINDER.name,
    val subjectId: Long? = null,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarm_history", indices = [Index("triggeredAt")])
data class AlarmHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val label: String,
    val actionType: String, // TRIGGERED, SNOOZED, DISMISSED, MISSED_EXACT_PERMISSION
    val triggeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions", indices = [Index("startedAt")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val isPomodoro: Boolean,
    val completedCycles: Int,
    val dndEnabled: Boolean,
    val startedAt: Long,
    val endedAt: Long
)
