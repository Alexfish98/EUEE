package com.example.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatCard
import com.example.core.ui.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("insights_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader("Academic Insights", "30-day offline learning telemetry & mastery trends")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "30d Accuracy",
                    value = "${state.overallAccuracy}%",
                    subtitle = "${state.totalAttempts} attempts",
                    icon = Icons.Default.Analytics,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Review Retention",
                    value = "${state.reviewEffectivenessPercent}%",
                    subtitle = "${state.totalStudyMinutes}m active",
                    icon = Icons.Default.Psychology,
                    tint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.totalAttempts == 0 && state.examScores.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Insights,
                    title = "No analytics data yet",
                    message = "Answer questions in Study Library or take a Mock Exam to unlock accuracy trends, error breakdowns, and topic mastery charts."
                )
            }
        }

        // 14-Day Accuracy Trend Line Chart
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("14-Day Accuracy Trend", "Daily percentage of correct responses")
                    AccuracyLineChart(values = state.accuracyTrend14d)
                }
            }
        }

        // Subject Accuracy Bars
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Subject-Wise Accuracy & Mastery")
                    state.subjectProgress.forEach { s ->
                        val acc = if (s.attemptsTotal == 0) 0 else (s.correctTotal * 100) / s.attemptsTotal
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.subjectName, style = MaterialTheme.typography.titleSmall)
                                Text("$acc% (${s.masteredCount}/${s.totalQuestions} mastered)", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = { acc / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(s.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Mastery & Confidence Breakdown
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Spaced Repetition Status", "Distribution across learning stages")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("LEARNING" to AmberWarning, "REVIEWING" to MaterialTheme.colorScheme.primary, "MASTERED" to EmeraldSuccess).forEach { (key, col) ->
                            val count = state.masteryBreakdown[key] ?: 0
                            StatusPill("$key: $count", col.copy(alpha = 0.14f), col)
                        }
                    }
                    HorizontalDivider()
                    Text("Confidence Ratings (1–5)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..5).forEach { rating ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${state.confidenceDistribution[rating] ?: 0}", fontWeight = FontWeight.Bold)
                                Text("★$rating", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Error Categories & Exam Score History
        if (state.errorCategories.isNotEmpty() || state.examScores.isNotEmpty()) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.errorCategories.isNotEmpty()) {
                            SectionHeader("Incorrect Answer Categories")
                            state.errorCategories.forEach { (cat, count) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat.replace('_', ' '), style = MaterialTheme.typography.bodyMedium)
                                    StatusPill("$count", CoralError.copy(alpha = 0.14f), CoralError)
                                }
                            }
                        }
                        if (state.examScores.isNotEmpty()) {
                            HorizontalDivider()
                            SectionHeader("Mock Exam Scores")
                            state.examScores.take(5).forEach { exam ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(exam.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text("${exam.scorePercent.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccuracyLineChart(values: List<Float>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        // Horizontal reference lines (0%, 50%, 100%)
        listOf(0f, 0.5f, 1f).forEach { frac ->
            val y = size.height * (1f - frac)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }
        if (values.size < 2) return@Canvas
        val dx = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { idx, v ->
            val x = idx * dx
            val y = size.height * (1f - (v / 100f).coerceIn(0f, 1f))
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(lineColor, radius = 5f, center = Offset(x, y))
        }
        drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}
