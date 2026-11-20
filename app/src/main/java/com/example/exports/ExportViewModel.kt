package com.example.exports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportPayloadType(val title: String, val fileName: String, val mimeType: String) {
    FULL_BACKUP("Full Database Backup (JSON)", "euee_mastery_backup.json", "application/json"),
    QUESTIONS_JSON("Question Bank (JSON)", "euee_questions.json", "application/json"),
    QUESTIONS_CSV("Question Bank (CSV)", "euee_questions.csv", "text/csv"),
    NOTES_MD("Personal Notebook (Markdown)", "euee_notebook.md", "text/markdown"),
    FLASHCARDS_JSON("Flashcards Deck (JSON)", "euee_flashcards.json", "application/json"),
    STUDY_PLAN_CSV("Study Plan (CSV)", "euee_study_plan.csv", "text/csv"),
    EXAM_RESULTS_JSON("Examination Results (JSON)", "euee_exam_results.json", "application/json"),
    INCORRECT_QUESTIONS_JSON("Incorrect Questions Review (JSON)", "euee_incorrect_questions.json", "application/json"),
    PRINTABLE_EXAM_PDF("Printable Exam Sheet + Key (PDF)", "euee_printable_exam.pdf", "application/pdf")
}

class ExportViewModel(
    private val appContext: Context,
    private val repository: EueeRepository
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun exportToUri(type: ExportPayloadType, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        writePayload(type, out)
                    } ?: error("Could not open destination file.")
                }
            }.onSuccess {
                _status.value = "Exported ${type.title} successfully."
            }.onFailure {
                _status.value = "Export failed: ${it.message}"
            }
        }
    }

    fun shareExport(context: Context, type: ExportPayloadType) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ExportManager.shareFile(context, type.fileName, type.mimeType) { out ->
                        // Note: writePayload is suspend, runBlocking inside IO or prepare bytes beforehand
                    }
                }
            }
        }
    }

    fun prepareAndShare(context: Context, type: ExportPayloadType) {
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    val baos = java.io.ByteArrayOutputStream()
                    writePayload(type, baos)
                    baos.toByteArray()
                }
                ExportManager.shareFile(context, type.fileName, type.mimeType) { out ->
                    out.write(bytes)
                }
            }.onSuccess {
                _status.value = "Opened Android Share Sheet for ${type.fileName}."
            }.onFailure {
                _status.value = "Share failed: ${it.message}"
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val jsonText = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                withContext(Dispatchers.IO) {
                    ExportManager.restoreFullBackupJson(repository.db, jsonText)
                }
            }.onSuccess { count ->
                _status.value = "Backup restored cleanly ($count questions & user notes/cards restored)."
            }.onFailure {
                _status.value = "Backup restore failed: ${it.message}"
            }
        }
    }

    private suspend fun writePayload(type: ExportPayloadType, out: java.io.OutputStream) {
        val db = repository.db
        when (type) {
            ExportPayloadType.FULL_BACKUP -> out.write(ExportManager.exportFullBackupJson(db).toByteArray())
            ExportPayloadType.QUESTIONS_JSON -> out.write(ExportManager.exportQuestionBankJson(db, db.questionDao().getAllQuestions()).toByteArray())
            ExportPayloadType.QUESTIONS_CSV -> out.write(ExportManager.exportQuestionsCsv(db, db.questionDao().getAllQuestions()).toByteArray())
            ExportPayloadType.NOTES_MD -> out.write(ExportManager.exportNotesMarkdown(db).toByteArray())
            ExportPayloadType.FLASHCARDS_JSON -> out.write(ExportManager.exportFlashcardsJson(db).toByteArray())
            ExportPayloadType.STUDY_PLAN_CSV -> out.write(ExportManager.exportStudyPlanCsv(db).toByteArray())
            ExportPayloadType.EXAM_RESULTS_JSON -> out.write(ExportManager.exportExamResultsJson(db).toByteArray())
            ExportPayloadType.INCORRECT_QUESTIONS_JSON -> {
                val wrongIds = db.learningProgressDao().getAllProgress()
                    .filter { it.incorrectCount > 0 }
                    .map { it.questionId }
                val wrongQs = if (wrongIds.isEmpty()) emptyList() else db.questionDao().getQuestionsByIds(wrongIds)
                out.write(ExportManager.exportQuestionBankJson(db, wrongQs).toByteArray())
            }
            ExportPayloadType.PRINTABLE_EXAM_PDF -> {
                val qs = db.questionDao().getAllQuestions().take(30)
                ExportManager.writePrintableExamPdf(out, "EUEE Mastery Printable Practice Paper", qs)
            }
        }
    }
}
