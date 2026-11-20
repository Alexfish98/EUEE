package com.example.core.model

enum class QuestionType(val label: String) {
    MULTIPLE_CHOICE("Multiple Choice"),
    SHORT_ANSWER("Short Answer"),
    NUMERICAL("Numerical"),
    TRUE_FALSE("True / False")
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    CHALLENGE("EUEE Challenge")
}

enum class MasteryStatus(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    REVIEWING("Reviewing"),
    MASTERED("Mastered")
}

enum class StudyMode(val title: String, val subtitle: String) {
    PRACTICE("Practice", "Answer with instant feedback & explanation"),
    ACTIVE_RECALL("Active Recall", "Think first, reveal answer on demand"),
    FLASHCARDS("Flashcards", "Flip cards and rate recall confidence"),
    MULTIPLE_CHOICE("Multiple Choice", "Randomized options from the bank"),
    TIMED_EXAM("Timed Examination", "Timed drill with configurable duration"),
    REVIEW("Review", "Revisit previously attempted questions"),
    SMART_STUDY("Smart Study", "Prioritizes mistakes, weak topics & due items"),
    RANDOM_PRACTICE("Random Practice", "Shuffled cross-topic challenge"),
    WEAK_TOPICS("Weak Topics", "Focus on lowest-accuracy topics"),
    RAPID_REVISION("Rapid Revision", "Quick 10-question sprint session")
}

enum class AlarmCategory(val label: String) {
    STUDY_REMINDER("Study Session Reminder"),
    EXAM_REMINDER("Examination Reminder"),
    BREAK_REMINDER("Break Reminder"),
    WAKE_UP("Wake-Up Alarm"),
    SUBJECT_ALARM("Subject-Specific Alarm")
}

enum class TaskPriority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Exam Critical", 4)
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AccentPalette(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long
) {
    ABYSSINIAN_COBALT("Abyssinian Cobalt", 0xFF1A3B70, 0xFF0D9488, 0xFFD97706),
    EMERALD_SCHOLAR("Emerald Scholar", 0xFF0F766E, 0xFF1D4ED8, 0xFFF59E0B),
    ROYAL_CRIMSON("Royal Crimson", 0xFF9F1239, 0xFF0369A1, 0xFFD97706),
    AMETHYST_STUDIO("Amethyst Studio", 0xFF5B21B6, 0xFF0D9488, 0xFFEA580C),
    WARM_OCHRE("Warm Ochre", 0xFFB45309, 0xFF1E3A8A, 0xFF059669)
}

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    AMHARIC_LOCALE("am", "Amharic (Curriculum Terms Ready)"),
    OROMO_LOCALE("om", "Afaan Oromoo (Locale Ready)")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.ABYSSINIAN_COBALT,
    val fontScale: Float = 1.0f,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val alarmDefaultSnoozeMinutes: Int = 10,
    val dndAutoEnableForFocus: Boolean = false,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val dailyFocusGoalMinutes: Int = 90,
    val dailyStudyGoalMinutes: Int = 120,
    val defaultExamMinutes: Int = 60,
    val keepScreenAwakeInStudy: Boolean = true,
    val keepScreenAwakeInExam: Boolean = true,
    val srsDailyNewLimit: Int = 20,
    val srsDailyReviewLimit: Int = 60,
    val speechRate: Float = 1.0f,
    val currentStreakDays: Int = 1,
    val lastStudyEpochDay: Long = 0L,
    val examTargetEpochDay: Long = 0L,
    val targetTotalScore: Int = 520,
    val completedSyllabusTopicIds: Set<String> = emptySet(),
    val completedChecklistIds: Set<String> = emptySet()
)

data class AnswerEvaluation(
    val isCorrect: Boolean,
    val normalizedUserAnswer: String,
    val matchedAcceptedAnswer: String,
    val feedbackNote: String
)

data class SrsUpdateResult(
    val newMasteryStatus: MasteryStatus,
    val newIntervalDays: Double,
    val newEaseFactor: Double,
    val nextReviewAt: Long,
    val newStreak: Int
)
