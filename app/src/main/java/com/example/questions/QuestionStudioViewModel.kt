package com.example.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuestionStudioState(
    val subjects: List<SubjectEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val totalCount: Int = 0,
    val selectedSubjectId: Long? = null,
    val selectedGradeId: Long? = null,
    val selectedDifficulty: String? = null,
    val bookmarkedOnly: Boolean = false,
    val searchQuery: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val currentPage: Int = 0,
    val pageSize: Int = 50
)

class QuestionStudioViewModel(private val repository: EueeRepository) : ViewModel() {

    private val subjectFilter = MutableStateFlow<Long?>(null)
    private val gradeFilter = MutableStateFlow<Long?>(null)
    private val diffFilter = MutableStateFlow<String?>(null)
    private val bookmarkFilter = MutableStateFlow(false)
    private val queryFilter = MutableStateFlow("")
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val pagedQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val pageNumber = MutableStateFlow(0)

    val uiState: StateFlow<QuestionStudioState> = combine(
        repository.observeSubjects(),
        repository.observeGrades(),
        repository.observeUnits(),
        repository.observeTopics(),
        combine(
            repository.observeTotalQuestionCount(),
            subjectFilter,
            gradeFilter,
            diffFilter,
            bookmarkFilter
        ) { total, s, g, d, b -> listOf(total, s, g, d, b) },
        combine(queryFilter, selectedIds, pagedQuestions, pageNumber) { q, sel, list, pg ->
            listOf(q, sel, list, pg)
        }
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val subs = arr[0] as List<SubjectEntity>
        @Suppress("UNCHECKED_CAST")
        val grades = arr[1] as List<GradeEntity>
        @Suppress("UNCHECKED_CAST")
        val units = arr[2] as List<UnitEntity>
        @Suppress("UNCHECKED_CAST")
        val topics = arr[3] as List<TopicEntity>
        @Suppress("UNCHECKED_CAST")
        val f1 = arr[4] as List<Any?>
        @Suppress("UNCHECKED_CAST")
        val f2 = arr[5] as List<Any?>

        @Suppress("UNCHECKED_CAST")
        QuestionStudioState(
            subjects = subs,
            grades = grades,
            units = units,
            topics = topics,
            questions = f2[2] as List<QuestionEntity>,
            totalCount = f1[0] as Int,
            selectedSubjectId = f1[1] as Long?,
            selectedGradeId = f1[2] as Long?,
            selectedDifficulty = f1[3] as String?,
            bookmarkedOnly = f1[4] as Boolean,
            searchQuery = f2[0] as String,
            selectedIds = f2[1] as Set<Long>,
            currentPage = f2[3] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuestionStudioState())

    init {
        refreshPage()
    }

    fun setSubjectFilter(id: Long?) { subjectFilter.value = id; pageNumber.value = 0; refreshPage() }
    fun setGradeFilter(id: Long?) { gradeFilter.value = id; pageNumber.value = 0; refreshPage() }
    fun setDifficultyFilter(d: String?) { diffFilter.value = d; pageNumber.value = 0; refreshPage() }
    fun setBookmarkedOnly(only: Boolean) { bookmarkFilter.value = only; pageNumber.value = 0; refreshPage() }
    fun setSearchQuery(q: String) { queryFilter.value = q; pageNumber.value = 0; refreshPage() }

    fun nextPage() { pageNumber.value += 1; refreshPage() }
    fun prevPage() { if (pageNumber.value > 0) { pageNumber.value -= 1; refreshPage() } }

    fun refreshPage() {
        viewModelScope.launch {
            pagedQuestions.value = repository.queryQuestionsPaged(
                subjectId = subjectFilter.value,
                gradeId = gradeFilter.value,
                difficulty = diffFilter.value,
                bookmarkedOnly = bookmarkFilter.value,
                query = queryFilter.value,
                limit = 50,
                offset = pageNumber.value * 50
            )
        }
    }

    fun toggleSelection(id: Long) {
        selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun bulkDeleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteQuestions(ids)
            selectedIds.value = emptySet()
            refreshPage()
        }
    }

    fun bulkSetDifficulty(difficulty: String) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkUpdateDifficulty(ids, difficulty)
            selectedIds.value = emptySet()
            refreshPage()
        }
    }

    fun duplicateQuestion(q: QuestionEntity) {
        viewModelScope.launch {
            val copy = q.copy(
                id = 0L,
                uniqueCode = "${q.uniqueCode}-COPY-${System.currentTimeMillis() % 10000}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertQuestion(copy)
            refreshPage()
        }
    }

    suspend fun getQuestion(id: Long): QuestionEntity? = repository.getQuestionById(id)

    fun saveQuestion(question: QuestionEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.upsertQuestion(question.copy(updatedAt = System.currentTimeMillis()))
            refreshPage()
            onSaved()
        }
    }
}
