package com.example.imports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportTargetMode(val label: String) {
    MERGE("Merge into Selected Bank"),
    CREATE_NEW("Create New Question Bank"),
    REPLACE("Replace Selected Bank (Requires Confirmation)")
}

data class ImportUiState(
    val rawText: String = "",
    val maxRecordsLimit: Int = 10_000,
    val preview: ImportPreviewResult? = null,
    val banks: List<QuestionBankEntity> = emptyList(),
    val selectedBankId: Long = 1L,
    val newBankName: String = "Imported EUEE Bank",
    val targetMode: ImportTargetMode = ImportTargetMode.MERGE,
    val isImporting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val statusMessage: String? = null
)

class ImportViewModel(
    private val appContext: Context,
    private val repository: EueeRepository
) : ViewModel() {

    private val rawText = MutableStateFlow("")
    private val maxLimit = MutableStateFlow(10_000)
    private val preview = MutableStateFlow<ImportPreviewResult?>(null)
    private val selectedBankId = MutableStateFlow(1L)
    private val newBankName = MutableStateFlow("Imported EUEE Bank")
    private val targetMode = MutableStateFlow(ImportTargetMode.MERGE)
    private val isImporting = MutableStateFlow(false)
    private val progressPair = MutableStateFlow(0 to 0)
    private val statusMsg = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ImportUiState> = combine(
        combine(rawText, maxLimit, preview, repository.observeQuestionBanks(), selectedBankId) { raw, lim, prev, banks, bankId ->
            listOf(raw, lim, prev, banks, bankId)
        },
        combine(newBankName, targetMode, isImporting, progressPair, statusMsg) { name, mode, busy, prog, msg ->
            listOf(name, mode, busy, prog, msg)
        }
    ) { a, b ->
        @Suppress("UNCHECKED_CAST")
        ImportUiState(
            rawText = a[0] as String,
            maxRecordsLimit = a[1] as Int,
            preview = a[2] as ImportPreviewResult?,
            banks = a[3] as List<QuestionBankEntity>,
            selectedBankId = a[4] as Long,
            newBankName = b[0] as String,
            targetMode = b[1] as ImportTargetMode,
            isImporting = b[2] as Boolean,
            progressCurrent = (b[3] as Pair<Int, Int>).first,
            progressTotal = (b[3] as Pair<Int, Int>).second,
            statusMessage = b[4] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImportUiState())

    fun setRawText(text: String) { rawText.value = text }
    fun setMaxLimit(limit: Int) { maxLimit.value = limit.coerceIn(100, 20_000) }
    fun setSelectedBankId(id: Long) { selectedBankId.value = id }
    fun setNewBankName(name: String) { newBankName.value = name }
    fun setTargetMode(mode: ImportTargetMode) { targetMode.value = mode }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            rawText.value = content
            validateAndPreview()
        }
    }

    fun validateAndPreview() {
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) {
                repository.db.questionDao().getAllUniqueCodes().toSet()
            }
            val parsed = withContext(Dispatchers.Default) {
                ImportParser.parse(rawText.value, existing, maxLimit.value)
            }
            preview.value = parsed
            statusMsg.value = "Validated ${parsed.validItems.size} valid, ${parsed.duplicateCodes.size} duplicates, ${parsed.rejectedRecords.size} rejected."
        }
    }

    fun executeImport() {
        val currentPreview = preview.value ?: return
        if (currentPreview.validItems.isEmpty()) return

        viewModelScope.launch {
            isImporting.value = true
            progressPair.value = 0 to currentPreview.validItems.size
            try {
                val count = withContext(Dispatchers.IO) {
                    val db = repository.db
                    val bankId = when (targetMode.value) {
                        ImportTargetMode.CREATE_NEW -> repository.createQuestionBank(newBankName.value.ifBlank { "Imported Bank" }, "Offline Import", "IMPORTED")
                        else -> selectedBankId.value
                    }

                    val subjectsByName = db.curriculumDao().getAllSubjects().associateBy { it.name.lowercase() }.toMutableMap()
                    val gradesByLevel = db.curriculumDao().getAllGrades().associateBy { it.level }.toMutableMap()
                    val units = db.curriculumDao().getAllUnits().toMutableList()
                    val topics = db.curriculumDao().getAllTopics().toMutableList()

                    val entities = currentPreview.validItems.map { item ->
                        val sub = subjectsByName[item.subjectName.lowercase()] ?: run {
                            val sid = db.curriculumDao().insertSubject(
                                SubjectEntity(name = item.subjectName, category = "Imported", colorHex = 0xFF0284C7, iconName = "School", isCoreEuee = false)
                            )
                            SubjectEntity(id = sid, name = item.subjectName, category = "Imported", colorHex = 0xFF0284C7, iconName = "School", isCoreEuee = false)
                                .also { subjectsByName[item.subjectName.lowercase()] = it }
                        }
                        val grd = gradesByLevel[item.gradeLevel] ?: gradesByLevel.values.first()
                        val unit = units.firstOrNull { it.subjectId == sub.id && it.gradeId == grd.id && it.title.equals(item.unitTitle, true) }
                            ?: run {
                                val uid = db.curriculumDao().insertUnit(UnitEntity(subjectId = sub.id, gradeId = grd.id, unitNumber = 1, title = item.unitTitle))
                                UnitEntity(id = uid, subjectId = sub.id, gradeId = grd.id, unitNumber = 1, title = item.unitTitle).also { units.add(it) }
                            }
                        val topic = topics.firstOrNull { it.unitId == unit.id && it.title.equals(item.topicTitle, true) }
                            ?: run {
                                val tid = db.curriculumDao().insertTopic(TopicEntity(unitId = unit.id, title = item.topicTitle))
                                TopicEntity(id = tid, unitId = unit.id, title = item.topicTitle).also { topics.add(it) }
                            }
                        ImportParser.toQuestionEntity(item, bankId, sub.id, grd.id, unit.id, topic.id)
                    }

                    repository.batchImportQuestions(
                        questionsToInsert = entities,
                        replaceBankId = if (targetMode.value == ImportTargetMode.REPLACE) bankId else null
                    ) { cur, tot ->
                        progressPair.value = cur to tot
                    }
                }
                statusMsg.value = "Successfully imported $count question(s) offline!"
                preview.value = null
                rawText.value = ""
            } catch (ex: Exception) {
                statusMsg.value = "Import failed: ${ex.message}"
            } finally {
                isImporting.value = false
            }
        }
    }
}
