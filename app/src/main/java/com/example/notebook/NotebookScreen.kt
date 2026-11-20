package com.example.notebook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.StatusPill
import com.example.database.FlashcardEntity
import com.example.database.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Notes, 1 = Flashcards
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var flippedCardIds by remember { mutableStateOf(setOf<Long>()) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Notes (${state.notes.size})") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Flashcards (${state.flashcards.size})") })
        }

        state.statusMessage?.let { msg ->
            AssistChip(
                onClick = { viewModel.clearMessage() },
                label = { Text(msg) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Dismiss") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Subject filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedSubjectId == null,
                    onClick = { viewModel.selectSubject(null) },
                    label = { Text("All") }
                )
            }
            items(state.subjects, key = { it.id }) { s ->
                FilterChip(
                    selected = state.selectedSubjectId == s.id,
                    onClick = { viewModel.selectSubject(s.id) },
                    label = { Text(s.name) }
                )
            }
        }

        if (activeTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.showMistakesOnly,
                    onClick = { viewModel.setShowMistakesOnly(!state.showMistakesOnly) },
                    label = { Text("Mistakes") }
                )
                FilledTonalIconButton(
                    onClick = {
                        editingNote = null
                        showNoteDialog = true
                    },
                    modifier = Modifier.testTag("add_note_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create note")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.notes.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.EditNote,
                            title = "No notes yet",
                            message = "Create study notes or answer questions to log mistakes automatically.",
                            actionLabel = "Create Note",
                            onAction = { editingNote = null; showNoteDialog = true }
                        )
                    }
                } else {
                    items(state.notes, key = { it.id }) { note ->
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingNote = note
                                    showNoteDialog = true
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (note.isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                        }
                                        Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    if (note.isMistakeEntry) {
                                        StatusPill("Mistake Log", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                                Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        dateFormatter.format(Date(note.updatedAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row {
                                        TextButton(onClick = { viewModel.convertNoteToFlashcard(note) }) {
                                            Text("To Card")
                                        }
                                        IconButton(onClick = { viewModel.togglePin(note) }) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pin note")
                                        }
                                        IconButton(onClick = { viewModel.deleteNote(note) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete note")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Flashcards Deck Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tap any card to flip and rate recall", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { showCardDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Card")
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.flashcards.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Style,
                            title = "No flashcards in this filter",
                            message = "Create a flashcard or convert any note into a flashcard with one tap.",
                            actionLabel = "Add Flashcard",
                            onAction = { showCardDialog = true }
                        )
                    }
                } else {
                    items(state.flashcards, key = { it.id }) { card ->
                        val isFlipped = card.id in flippedCardIds
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    flippedCardIds = if (isFlipped) flippedCardIds - card.id else flippedCardIds + card.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatusPill(
                                        text = if (isFlipped) "BACK (Tap to flip)" else "FRONT (Tap to reveal)",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    IconButton(onClick = { viewModel.deleteFlashcard(card) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete flashcard")
                                    }
                                }
                                Text(
                                    text = if (isFlipped) card.back else card.front,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!isFlipped && card.hint.isNotBlank()) {
                                    Text("Hint: ${card.hint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isFlipped) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(1 to "Hard", 3 to "Good", 5 to "Easy").forEach { (conf, lbl) ->
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.rateFlashcard(card, conf)
                                                    flippedCardIds = flippedCardIds - card.id
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) { Text(lbl) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        var title by remember(editingNote) { mutableStateOf(editingNote?.title ?: "") }
        var content by remember(editingNote) { mutableStateOf(editingNote?.content ?: "") }
        var pinned by remember(editingNote) { mutableStateOf(editingNote?.isPinned ?: false) }
        var subId by remember(editingNote) { mutableStateOf(editingNote?.subjectId ?: state.selectedSubjectId) }

        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(if (editingNote == null) "New Study Note" else "Edit Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Notes / Formulas / Key Points") },
                        minLines = 4,
                        maxLines = 8
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.subjects, key = { it.id }) { s ->
                            FilterChip(selected = subId == s.id, onClick = { subId = s.id }, label = { Text(s.name) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                        Text("Pin note to top")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveNote(editingNote, title, content, subId, pinned)
                    showNoteDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCardDialog) {
        var front by remember { mutableStateOf("") }
        var back by remember { mutableStateOf("") }
        var hint by remember { mutableStateOf("") }
        var subId by remember { mutableStateOf(state.selectedSubjectId) }
        AlertDialog(
            onDismissRequest = { showCardDialog = false },
            title = { Text("Create Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Front (Prompt / Term)") })
                    OutlinedTextField(value = back, onValueChange = { back = it }, label = { Text("Back (Answer / Definition)") })
                    OutlinedTextField(value = hint, onValueChange = { hint = it }, label = { Text("Optional Hint") })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.subjects, key = { it.id }) { s ->
                            FilterChip(selected = subId == s.id, onClick = { subId = s.id }, label = { Text(s.name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveFlashcard(null, front, back, hint, subId)
                    showCardDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCardDialog = false }) { Text("Cancel") } }
        )
    }
}
