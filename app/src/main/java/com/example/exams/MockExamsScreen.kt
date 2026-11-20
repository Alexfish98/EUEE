package com.example.exams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Difficulty
import com.example.core.ui.EmptyStateView
import com.example.core.ui.GradientActionCard
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MockExamsScreen(
    viewModel: MockExamViewModel,
    onStartExam: () -> Unit,
    onResumeExam: () -> Unit,
    onOpenResult: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.lobbyState.collectAsStateWithLifecycle()
    val dateFmt = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("mock_exams_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.inProgressExam != null) {
            val saved = state.inProgressExam!!
            item {
                GradientActionCard(
                    title = "Resume In-Progress Exam: ${saved.title}",
                    subtitle = "${saved.remainingSeconds / 60} min remaining • All answers auto-saved",
                    icon = Icons.Default.PlayArrow,
                    startColor = Color(0xFFB45309),
                    endColor = Color(0xFF9F1239),
                    onClick = { viewModel.resumeSavedExam(onResumeExam) }
                )
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Create Custom Mock Exam", "Full-screen simulator with persistent answer recovery")
                    OutlinedTextField(
                        value = state.examTitle,
                        onValueChange = { viewModel.setExamTitle(it) },
                        label = { Text("Examination Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subjects (leave empty for all)", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.subjects, key = { it.id }) { s ->
                            FilterChip(
                                selected = s.id in state.selectedSubjectIds,
                                onClick = { viewModel.toggleSubject(s.id) },
                                label = { Text(s.name) }
                            )
                        }
                    }

                    Text("Grade & Difficulty", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedGradeId == null,
                                onClick = { viewModel.selectGrade(null) },
                                label = { Text("All Grades") }
                            )
                        }
                        items(state.grades, key = { it.id }) { g ->
                            FilterChip(
                                selected = state.selectedGradeId == g.id,
                                onClick = { viewModel.selectGrade(g.id) },
                                label = { Text(g.label) }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedDifficulty == null,
                                onClick = { viewModel.selectDifficulty(null) },
                                label = { Text("Mixed Difficulty") }
                            )
                        }
                        items(Difficulty.entries) { d ->
                            FilterChip(
                                selected = state.selectedDifficulty == d.name,
                                onClick = { viewModel.selectDifficulty(d.name) },
                                label = { Text(d.label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Questions: ${state.questionCount}", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = state.questionCount.toFloat(),
                                onValueChange = { viewModel.setQuestionCount(it.toInt()) },
                                valueRange = 5f..100f
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration: ${state.durationMinutes} min", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = state.durationMinutes.toFloat(),
                                onValueChange = { viewModel.setDurationMinutes(it.toInt()) },
                                valueRange = 5f..180f
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.startNewExam(onStartExam) },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("start_mock_exam_btn")
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Examination")
                    }
                }
            }
        }

        item {
            SectionHeader("Past Examination Results", "${state.history.size} completed mock exams")
        }

        if (state.history.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.HistoryEdu,
                    title = "No completed mock exams yet",
                    message = "Configure and start a timed mock exam above to generate detailed topic analytics."
                )
            }
        } else {
            items(state.history, key = { it.id }) { res ->
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenResult(res.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(res.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${res.correctCount}/${res.totalQuestions} correct • ${res.timeTakenSeconds / 60}m ${res.timeTakenSeconds % 60}s",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(dateFmt.format(Date(res.completedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(
                            text = "${res.scorePercent.toInt()}%",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
