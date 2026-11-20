package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PrimarySection(
    val route: String,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", "Home", Icons.Default.Dashboard),
    STUDY_LIBRARY("study_library", "Study Library", "Library", Icons.AutoMirrored.Filled.MenuBook),
    NOTEBOOK("notebook", "Notebook & Cards", "Notes", Icons.Default.EditNote),
    MOCK_EXAMS("exams", "Mock Exams", "Exams", Icons.Default.Assignment),
    DAILY_PLANNER("planner", "Daily Planner", "Planner", Icons.Default.CalendarMonth),
    INSIGHTS("insights", "Insights", "Insights", Icons.Default.Insights),
    STUDENT_ESSENTIALS("essentials", "Student Essentials", "Essentials", Icons.Default.School),
    SCIENTIFIC_TOOLS("tools", "Scientific Tools", "Tools", Icons.Default.Calculate),
    QUESTION_STUDIO("question_studio", "Question Studio", "Studio", Icons.Default.Build),
    IMPORT_CENTER("imports", "Import Center", "Import", Icons.Default.FileUpload),
    EXPORT_CENTER("exports", "Export Center", "Export", Icons.Default.FileDownload),
    SETTINGS("settings", "Settings", "Settings", Icons.Default.Settings)
}

object Routes {
    const val STUDY_SESSION = "study_session"
    const val ACTIVE_EXAM = "active_exam"
    const val EXAM_RESULT = "exam_result/{resultId}"
    const val QUESTION_EDITOR = "question_editor/{questionId}"
    const val FOCUS_MODE = "focus"
    const val ALARMS = "alarms"
    const val FLASHCARDS = "flashcards"

    fun examResult(resultId: Long) = "exam_result/$resultId"
    fun questionEditor(questionId: Long = -1L) = "question_editor/$questionId"
}
