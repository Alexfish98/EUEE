package com.example.questions

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Difficulty
import com.example.core.ui.EmptyStateView
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.exports.ExportManager
import java.io.OutputStream

@Composable
fun QuestionStudioScreen(
    viewModel: QuestionStudioViewModel,
    onCreateQuestion: () -> Unit,
    onEditQuestion: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val subjectsById = remember(state.subjects) { state.subjects.associateBy { it.id } }

    Column(modifier = modifier.fillMaxSize().testTag("question_studio_screen")) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SectionHeader(
                    title = "Question Studio",
                    subtitle = "${state.totalCount} total questions • Page ${state.currentPage + 1}",
                    actionText = "+ New Question",
                    onActionClick = onCreateQuestion
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search code, prompt, or explanation...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = state.bookmarkedOnly,
                            onClick = { viewModel.setBookmarkedOnly(!state.bookmarkedOnly) },
                            label = { Text("Bookmarked") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedSubjectId == null,
                            onClick = { viewModel.setSubjectFilter(null) },
                            label = { Text("All Subjects") }
                        )
                    }
                    items(state.subjects, key = { it.id }) { s ->
                        FilterChip(
                            selected = state.selectedSubjectId == s.id,
                            onClick = { viewModel.setSubjectFilter(s.id) },
                            label = { Text(s.name) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedDifficulty == null,
                            onClick = { viewModel.setDifficultyFilter(null) },
                            label = { Text("Any Difficulty") }
                        )
                    }
                    items(Difficulty.entries) { d ->
                        FilterChip(
                            selected = state.selectedDifficulty == d.name,
                            onClick = { viewModel.setDifficultyFilter(d.name) },
                            label = { Text(d.label) }
                        )
                    }
                }
            }

            if (state.selectedIds.isNotEmpty()) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${state.selectedIds.size} selected", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.bulkSetDifficulty(Difficulty.HARD.name) }) { Text("Set Hard") }
                                TextButton(onClick = { viewModel.bulkDeleteSelected() }) { Text("Delete") }
                                TextButton(onClick = { viewModel.clearSelection() }) { Text("Clear") }
                            }
                        }
                    }
                }
            }

            if (state.questions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Build,
                        title = "No questions found",
                        message = "Adjust filters or create a new EUEE question manually.",
                        actionLabel = "Create Question",
                        onAction = onCreateQuestion
                    )
                }
            } else {
                items(state.questions, key = { it.id }) { q ->
                    val subName = subjectsById[q.subjectId]?.name ?: "Subject"
                    val isSel = q.id in state.selectedIds
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onEditQuestion(q.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSel, onCheckedChange = { viewModel.toggleSelection(q.id) })
                                    StatusPill(q.uniqueCode, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Row {
                                    IconButton(onClick = {
                                        val bmp = ExportManager.renderQuestionCardPng(q, subName)
                                        ExportManager.shareFile(context, "${q.uniqueCode}.png", "image/png") { out: OutputStream ->
                                            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                    }) {
                                        Icon(Icons.Default.Image, contentDescription = "Share PNG question card")
                                    }
                                    IconButton(onClick = { viewModel.duplicateQuestion(q) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate question")
                                    }
                                }
                            }
                            Text(q.questionText, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                            Text(
                                "$subName • ${q.difficulty} • Ans: ${q.correctAnswer}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { viewModel.prevPage() }, enabled = state.currentPage > 0) { Text("Previous 50") }
                    OutlinedButton(onClick = { viewModel.nextPage() }, enabled = state.questions.size >= state.pageSize) { Text("Next 50") }
                }
            }
        }
    }
}
