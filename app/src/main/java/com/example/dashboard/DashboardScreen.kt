package com.example.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.model.StudyMode
import com.example.core.ui.GradientActionCard
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatCard
import com.example.core.ui.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartStudyMode: (StudyMode) -> Unit,
    onResumeSession: (Long) -> Unit,
    onResumeExam: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenEssentials: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Academic Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(176.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dashboard_banner),
                        contentDescription = "EUEE Mastery Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xE60B172B),
                                        Color(0xAA102447),
                                        Color(0x551A3B70)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = "Streak: ${state.streakDays} day${if (state.streakDays == 1) "" else "s"}",
                                    containerColor = Color(0x33F59E0B),
                                    contentColor = Color(0xFFFCD34D)
                                )
                                StatusPill(
                                    text = if (state.focusActive) "Focus Active" else "100% Offline Ready",
                                    containerColor = Color(0x330D9488),
                                    contentColor = Color(0xFF5EEAD4)
                                )
                            }

                            Column {
                                Text(
                                    text = "EUEE Mastery Studio",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${state.dueReviews} due for spaced review • ${state.todayStudyMinutes}/${state.dailyGoalMinutes} min today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }

                            val goalProgress = if (state.dailyGoalMinutes > 0) {
                                (state.todayStudyMinutes.toFloat() / state.dailyGoalMinutes).coerceIn(0f, 1f)
                            } else 0f
                            LinearProgressIndicator(
                                progress = { goalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFF59E0B),
                                trackColor = Color.White.copy(alpha = 0.22f)
                            )
                        }
                    }
                }
            }
        }

        // Resume Unfinished Session or Mock Exam
        if (state.activeSession != null || state.inProgressExam != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.activeSession?.let { session ->
                        GradientActionCard(
                            title = "Resume ${session.title}",
                            subtitle = "Question ${session.currentIndex + 1} • ${session.attemptedCount} answered • Tap to continue",
                            icon = Icons.Default.PlayArrow,
                            startColor = MaterialTheme.colorScheme.primary,
                            endColor = MaterialTheme.colorScheme.secondary,
                            onClick = { onResumeSession(session.id) },
                            modifier = Modifier.testTag("resume_session_card")
                        )
                    }
                    state.inProgressExam?.let { exam ->
                        GradientActionCard(
                            title = "Resume Mock Exam: ${exam.title}",
                            subtitle = "${exam.remainingSeconds / 60} min remaining • Answers safely preserved",
                            icon = Icons.Default.Timer,
                            startColor = Color(0xFFB45309),
                            endColor = Color(0xFF9F1239),
                            onClick = onResumeExam,
                            modifier = Modifier.testTag("resume_exam_card")
                        )
                    }
                }
            }
        }

        // Quick-Start Actions
        item {
            SectionHeader(title = "Quick Start", subtitle = "Jump directly into targeted EUEE preparation")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    QuickActionTile(
                        title = "Smart Study",
                        subtitle = "${state.dueReviews} due",
                        icon = Icons.Default.AutoAwesome,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onStartStudyMode(StudyMode.SMART_STUDY) },
                        modifier = Modifier.testTag("quick_smart_study")
                    )
                }
                item {
                    QuickActionTile(
                        title = "Mock Exam",
                        subtitle = "Timed sim",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        color = Color(0xFFD97706),
                        onClick = onOpenExams,
                        modifier = Modifier.testTag("quick_mock_exam")
                    )
                }
                item {
                    QuickActionTile(
                        title = "Focus & DND",
                        subtitle = if (state.focusActive) "Running" else "Pomodoro",
                        icon = Icons.Default.SelfImprovement,
                        color = Color(0xFF0D9488),
                        onClick = onOpenFocus,
                        modifier = Modifier.testTag("quick_focus")
                    )
                }
                item {
                    QuickActionTile(
                        title = "Alarms",
                        subtitle = "${state.upcomingAlarms.size} active",
                        icon = Icons.Default.Alarm,
                        color = Color(0xFF7C3AED),
                        onClick = onOpenAlarms,
                        modifier = Modifier.testTag("quick_alarms")
                    )
                }
                item {
                    QuickActionTile(
                        title = "Essentials",
                        subtitle = "${state.daysUntilExam}d left",
                        icon = Icons.Default.School,
                        color = Color(0xFF059669),
                        onClick = onOpenEssentials,
                        modifier = Modifier.testTag("quick_essentials")
                    )
                }
                item {
                    QuickActionTile(
                        title = "Sci Tools",
                        subtitle = "21 offline tools",
                        icon = Icons.Default.Calculate,
                        color = Color(0xFF0284C7),
                        onClick = onOpenTools,
                        modifier = Modifier.testTag("quick_tools")
                    )
                }
            }
        }

        // Comprehensive KPI Grid
        item {
            SectionHeader(title = "Performance Overview", subtitle = "Live Room database telemetry")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Question Bank",
                        value = "${state.totalQuestions}",
                        subtitle = "${state.attemptedQuestions} attempted",
                        icon = Icons.Default.LibraryBooks,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenLibrary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Accuracy",
                        value = "${state.accuracyPercent}%",
                        subtitle = "${state.correctAnswers}✓ / ${state.incorrectAnswers}✗",
                        icon = Icons.Default.Verified,
                        tint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Mastered",
                        value = "${state.masteredQuestions}",
                        subtitle = "${state.learningQuestions} learning",
                        icon = Icons.Default.EmojiEvents,
                        tint = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Due & Mistakes",
                        value = "${state.dueReviews}",
                        subtitle = "${state.incorrectAttention} need review",
                        icon = Icons.Default.NotificationImportant,
                        tint = CoralError,
                        onClick = { onStartStudyMode(StudyMode.WEAK_TOPICS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weekly Study Activity Chart (Custom Compose Canvas)
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = "7-Day Study Activity", subtitle = "Questions attempted vs. answered correctly")
                    WeeklyActivityBarChart(points = state.weeklyActivity)
                }
            }
        }

        // Subject-Wise Progress
        item {
            SectionHeader(
                title = "Subject Progress",
                subtitle = "Natural Science & Additional EUEE subjects",
                actionText = "Open Library",
                onActionClick = onOpenLibrary
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.subjectProgress.forEach { sub ->
                    val pct = if (sub.attemptsTotal > 0) (sub.correctTotal * 100) / sub.attemptsTotal else 0
                    val coverage = if (sub.totalQuestions > 0) sub.attemptedQuestions.toFloat() / sub.totalQuestions else 0f
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenLibrary)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(sub.colorHex))
                                    )
                                    Text(sub.subjectName, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "${sub.attemptedQuestions}/${sub.totalQuestions} Q • $pct% acc • ${sub.masteredCount} mastered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { coverage.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(sub.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Weakest Topics & Today's Tasks + Upcoming Alarms
        if (state.weakTopics.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Weakest Topics",
                    subtitle = "Lowest accuracy topics needing targeted practice",
                    actionText = "Drill Weak",
                    onActionClick = { onStartStudyMode(StudyMode.WEAK_TOPICS) }
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.weakTopics.forEach { topic ->
                        val acc = if (topic.attemptsTotal > 0) (topic.correctTotal * 100) / topic.attemptsTotal else 0
                        OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(topic.topicTitle, style = MaterialTheme.typography.titleSmall)
                                    Text(topic.subjectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(
                                    text = "$acc% ($topic.correctTotal/${topic.attemptsTotal})".replace("$topic.correctTotal", "${topic.correctTotal}"),
                                    containerColor = CoralError.copy(alpha = 0.12f),
                                    contentColor = CoralError
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Tasks
        item {
            SectionHeader(
                title = "Today's Study Tasks",
                subtitle = "${state.todayTasks.count { it.isCompleted }} of ${state.todayTasks.size} completed",
                actionText = "Planner",
                onActionClick = onOpenPlanner
            )
            Spacer(Modifier.height(8.dp))
            if (state.todayTasks.isEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No study tasks scheduled for today. Open Daily Planner to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.todayTasks.forEach { task ->
                        ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTaskCompleted(task) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleTaskCompleted(task) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "%02d:%02d • %d min • %s".format(
                                            task.startTimeMinutes / 60,
                                            task.startTimeMinutes % 60,
                                            task.durationMinutes,
                                            task.priority
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Alarms & Recent Sessions
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenAlarms)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Upcoming Alarms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (state.upcomingAlarms.isEmpty()) {
                            Text("None active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.upcomingAlarms.forEach { alarm ->
                                Text(
                                    text = "%02d:%02d • %s".format(alarm.hour, alarm.minute, alarm.label),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Recent Sessions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (state.recentSessions.isEmpty()) {
                            Text("No sessions yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.recentSessions.take(3).forEach { s ->
                                Text(
                                    text = "${s.mode} (${s.correctCount}/${s.attemptedCount})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .width(126.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyActivityBarChart(points: List<DailyActivityPoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = EmeraldSuccess
    val track = MaterialTheme.colorScheme.surfaceVariant
    val maxAttempts = (points.maxOfOrNull { it.attempts } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val barSlot = size.width / points.size
            val barWidth = (barSlot * 0.48f).coerceAtMost(34f)
            points.forEachIndexed { idx, pt ->
                val x = idx * barSlot + (barSlot - barWidth) / 2f
                drawRoundRect(
                    color = track,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                val totalH = (pt.attempts.toFloat() / maxAttempts) * size.height
                if (totalH > 0f) {
                    drawRoundRect(
                        color = primary.copy(alpha = 0.45f),
                        topLeft = Offset(x, size.height - totalH),
                        size = Size(barWidth, totalH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    val correctH = (pt.correct.toFloat() / maxAttempts) * size.height
                    if (correctH > 0f) {
                        drawRoundRect(
                            color = secondary,
                            topLeft = Offset(x, size.height - correctH),
                            size = Size(barWidth, correctH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            points.forEach { pt ->
                Text(pt.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
