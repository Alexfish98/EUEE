package com.example.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EueeRepository
import com.example.database.FlashcardEntity
import com.example.database.NoteEntity
import com.example.database.SubjectEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotebookUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val searchQuery: String = "",
    val showMistakesOnly: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val statusMessage: String? = null
)

class NotebookViewModel(private val repository: EueeRepository) : ViewModel() {

    private val selectedSubjectId = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")
    private val showMistakesOnly = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val notesFlow = combine(selectedSubjectId, searchQuery) { s, q -> s to q }
        .flatMapLatest { (s, q) -> repository.observeNotes(s, q) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val flashcardsFlow = selectedSubjectId.flatMapLatest { s -> repository.observeFlashcards(s) }

    val uiState: StateFlow<NotebookUiState> = combine(
        repository.observeSubjects(),
        selectedSubjectId,
        searchQuery,
        showMistakesOnly,
        combine(notesFlow, flashcardsFlow, statusMessage) { n, f, msg -> Triple(n, f, msg) }
    ) { subjects, selSub, query, mistakesOnly, triple ->
        val (notes, cards, msg) = triple
        NotebookUiState(
            subjects = subjects,
            selectedSubjectId = selSub,
            searchQuery = query,
            showMistakesOnly = mistakesOnly,
            notes = if (mistakesOnly) notes.filter { it.isMistakeEntry } else notes,
            flashcards = cards,
            statusMessage = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotebookUiState())

    fun selectSubject(id: Long?) { selectedSubjectId.value = id }
    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun setShowMistakesOnly(only: Boolean) { showMistakesOnly.value = only }
    fun clearMessage() { statusMessage.value = null }

    fun saveNote(existing: NoteEntity?, title: String, content: String, subjectId: Long?, pinned: Boolean) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.upsertNote(
                NoteEntity(
                    id = existing?.id ?: 0L,
                    subjectId = subjectId,
                    topicId = existing?.topicId,
                    title = title.ifBlank { "Untitled Note" },
                    content = content,
                    isPinned = pinned,
                    isMistakeEntry = existing?.isMistakeEntry ?: false,
                    linkedQuestionId = existing?.linkedQuestionId,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
            statusMessage.value = "Note saved."
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.upsertNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            statusMessage.value = "Note deleted."
        }
    }

    fun convertNoteToFlashcard(note: NoteEntity) {
        viewModelScope.launch {
            repository.upsertFlashcard(
                FlashcardEntity(
                    subjectId = note.subjectId,
                    topicId = note.topicId,
                    front = note.title,
                    back = note.content.take(500),
                    hint = "Converted from Notebook",
                    sourceNoteId = note.id
                )
            )
            statusMessage.value = "Converted '${note.title}' into a Flashcard."
        }
    }

    fun saveFlashcard(existing: FlashcardEntity?, front: String, back: String, hint: String, subjectId: Long?) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.upsertFlashcard(
                FlashcardEntity(
                    id = existing?.id ?: 0L,
                    subjectId = subjectId,
                    front = front.trim(),
                    back = back.trim(),
                    hint = hint.trim(),
                    confidence = existing?.confidence ?: 0,
                    intervalDays = existing?.intervalDays ?: 0.0,
                    nextReviewAt = existing?.nextReviewAt ?: 0L
                )
            )
            statusMessage.value = "Flashcard saved."
        }
    }

    fun rateFlashcard(card: FlashcardEntity, confidence: Int) {
        viewModelScope.launch {
            val intervalDays = when {
                confidence <= 2 -> 0.5
                confidence == 3 -> 2.0
                confidence == 4 -> 5.0
                else -> 12.0
            }
            val nextReview = System.currentTimeMillis() + (intervalDays * 86_400_000L).toLong()
            repository.upsertFlashcard(
                card.copy(confidence = confidence, intervalDays = intervalDays, nextReviewAt = nextReview)
            )
        }
    }

    fun deleteFlashcard(card: FlashcardEntity) {
        viewModelScope.launch { repository.deleteFlashcard(card) }
    }
}
