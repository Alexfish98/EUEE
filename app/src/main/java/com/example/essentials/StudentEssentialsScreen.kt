package com.example.essentials

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.tools.ReferenceData
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoFontFamily
import kotlin.math.max
import kotlin.math.roundToInt

enum class EssentialsTab(val title: String) {
    COUNTDOWN("Countdown & Pace"),
    SCORE_PLANNER("Score Planner"),
    SYLLABUS("Syllabus Tracker"),
    CHECKLISTS("Exam Checklists"),
    CHEAT_SHEETS("Pocket Cheat Sheets")
}

@Composable
fun StudentEssentialsScreen(
    viewModel: EssentialsViewModel,
    onStartWeakTopicsDrill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(EssentialsTab.COUNTDOWN) }

    Column(modifier = modifier.fillMaxSize().testTag("essentials_screen")) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EssentialsTab.entries) { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.title) },
                    modifier = Modifier.testTag("essentials_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                EssentialsTab.COUNTDOWN -> {
                    item { CountdownAndPaceSection(state, viewModel, onStartWeakTopicsDrill) }
                    item { ExamTimeBudgetHelperCard() }
                }
                EssentialsTab.SCORE_PLANNER -> {
                    item { ScorePlannerSection(state, viewModel) }
                }
                EssentialsTab.SYLLABUS -> {
                    item { SyllabusTrackerSection(state, viewModel) }
                }
                EssentialsTab.CHECKLISTS -> {
                    item { ExamChecklistsSection(state, viewModel) }
                }
                EssentialsTab.CHEAT_SHEETS -> {
                    item { PocketCheatSheetsSection() }
                }
            }
        }
    }
}

@Composable
private fun CountdownAndPaceSection(
    state: EssentialsUiState,
    viewModel: EssentialsViewModel,
    onStartWeakTopicsDrill: () -> Unit
) {
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = "EUEE COUNTDOWN",
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                    Text(
                        text = state.examTargetDateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = "${state.daysUntilExam} Days Left",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("essentials_days_left")
                )
                Text(
                    text = "Target: ${state.targetTotalScore}/600 • ${state.masteredQuestions}/${state.totalQuestions} bank items mastered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30L, 60L, 90L, 120L).forEach { preset ->
                        FilterChip(
                            selected = state.daysUntilExam == preset,
                            onClick = { viewModel.setExamTargetDaysFromToday(preset) },
                            label = { Text("${preset}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                labelColor = Color.White,
                                selectedContainerColor = Color.White,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.shiftExamTargetDays(-7L) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("-7d") }
                    OutlinedButton(
                        onClick = { viewModel.shiftExamTargetDays(-1L) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("-1d") }
                    OutlinedButton(
                        onClick = { viewModel.shiftExamTargetDays(1L) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("+1d") }
                    OutlinedButton(
                        onClick = { viewModel.shiftExamTargetDays(7L) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("+7d") }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Recommended Daily Study Pace", "Auto-calculated from your Room mastery queue")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${state.recommendedQuestionsPerDay} Q / day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Minimum active recall", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${state.recommendedStudyMinutesPerDay} min / day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Focused revision block", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(
                onClick = onStartWeakTopicsDrill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Weak Topics Drill Now")
            }
        }
    }
}

@Composable
private fun ExamTimeBudgetHelperCard() {
    var totalQuestions by remember { mutableStateOf("80") }
    var totalMinutes by remember { mutableStateOf("120") }
    val qCount = (totalQuestions.toIntOrNull() ?: 0).coerceIn(1, 300)
    val mins = (totalMinutes.toIntOrNull() ?: 0).coerceIn(5, 360)
    val secPerQ = ((mins * 60.0) / qCount).roundToInt()
    val pass1End = (mins * 0.65).roundToInt()
    val pass2End = (mins * 0.90).roundToInt()
    val omrScanMins = max(3, mins - pass2End)

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Exam-Hall Time Budget Helper", "Plan two-pass pacing & OMR verification checkpoints")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = totalQuestions,
                    onValueChange = { totalQuestions = it.filter(Char::isDigit).take(3) },
                    label = { Text("Questions") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = totalMinutes,
                    onValueChange = { totalMinutes = it.filter(Char::isDigit).take(3) },
                    label = { Text("Minutes") },
                    modifier = Modifier.weight(1f)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Average pace: ${secPerQ / 60}m ${secPerQ % 60}s per question",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• Pass 1 (Fast Sweep, ~65% time): Finish easy/medium items by minute $pass1End.", style = MaterialTheme.typography.bodySmall)
                    Text("• Pass 2 (Hard/Flagged Items): Resolve skipped items until minute $pass2End.", style = MaterialTheme.typography.bodySmall)
                    Text("• Final $omrScanMins min: Bubble-alignment & blank scan before hand-in.", style = MaterialTheme.typography.bodySmall)
                    Text("• Checkpoints: Q${qCount / 4} @ ${mins / 4}m  |  Q${qCount / 2} @ ${mins / 2}m  |  Q${(qCount * 3) / 4} @ ${(mins * 3) / 4}m", fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ScorePlannerSection(
    state: EssentialsUiState,
    viewModel: EssentialsViewModel
) {
    val total = state.estimatedTotalScore
    val delta = total - state.targetTotalScore
    val tier = when {
        total >= 540 -> "Top Competitive Tier (Medicine / Engineering / Honor Track)"
        total >= 460 -> "Strong University Placement Tier"
        total >= 360 -> "Solid Developing Tier — Target Weakest 2 Subjects"
        else -> "Intensive Foundation Building Needed"
    }
    val weakestEntry = state.subjectScoreEstimates.minByOrNull { it.value }

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("EUEE /600 Score Estimator", "Adjust subject projections or sync live Room accuracy")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$total / 600 (${state.estimatedPercent}%)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("essentials_score_total")
                    )
                    Text(tier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(
                    text = if (delta >= 0) "+$delta vs goal" else "$delta vs goal",
                    containerColor = (if (delta >= 0) EmeraldSuccess else AmberWarning).copy(alpha = 0.16f),
                    contentColor = if (delta >= 0) EmeraldSuccess else AmberWarning
                )
            }

            Text("Target Score Goal: ${state.targetTotalScore} / 600", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = state.targetTotalScore.toFloat(),
                onValueChange = { viewModel.setTargetTotalScore(it.roundToInt()) },
                valueRange = 250f..600f
            )

            FilledTonalButton(
                onClick = { viewModel.syncEstimatesFromPerformance() },
                modifier = Modifier.fillMaxWidth().testTag("essentials_sync_scores_btn")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Load My Room Subject Accuracies")
            }

            weakestEntry?.let { (name, sc) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoralError.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Highest ROI Focus: Raising $name (currently $sc/100) by +15 pts boosts your total fastest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralError,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Subject Breakdown (0–100 each)")
            state.subjectScoreEstimates.forEach { (subject, score) ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(subject, fontWeight = FontWeight.SemiBold)
                        Text("$score / 100", fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = score.toFloat(),
                        onValueChange = { viewModel.updateSubjectEstimate(subject, it.roundToInt()) },
                        valueRange = 0f..100f
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusTrackerSection(
    state: EssentialsUiState,
    viewModel: EssentialsViewModel
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val allTopicsCount = state.syllabusUnits.sumOf { it.topics.size }
    val completedTopicsCount = state.syllabusUnits.sumOf { unitItem ->
        unitItem.topics.count { it.id.toString() in state.completedSyllabusTopicIds }
    }
    val pct = if (allTopicsCount == 0) 0 else (completedTopicsCount * 100) / allTopicsCount

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Grades 9–12 Syllabus Coverage", "$completedTopicsCount of $allTopicsCount curriculum topics completed ($pct%)")
            LinearProgressIndicator(
                progress = { if (allTopicsCount == 0) 0f else completedTopicsCount.toFloat() / allTopicsCount },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { selectedSubject = null },
                        label = { Text("All Subjects") }
                    )
                }
                items(state.subjects) { sub ->
                    FilterChip(
                        selected = selectedSubject == sub.name,
                        onClick = { selectedSubject = sub.name },
                        label = { Text(sub.name) }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    val filteredUnits = state.syllabusUnits.filter {
        selectedSubject == null || it.subjectName == selectedSubject
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        filteredUnits.forEach { item ->
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.subjectName} • Grade ${item.gradeLevel} (Unit ${item.unit.unitNumber})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(item.subjectColorHex)
                        )
                        val doneInUnit = item.topics.count { it.id.toString() in state.completedSyllabusTopicIds }
                        Text("$doneInUnit/${item.topics.size}", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(item.unit.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    item.topics.forEach { topic ->
                        val checked = topic.id.toString() in state.completedSyllabusTopicIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSyllabusTopic(topic.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.toggleSyllabusTopic(topic.id) },
                                modifier = Modifier.testTag("syllabus_topic_cb_${topic.id}")
                            )
                            Text(topic.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamChecklistsSection(
    state: EssentialsUiState,
    viewModel: EssentialsViewModel
) {
    val allItems = viewModel.checklistItems
    val doneCount = allItems.count { it.id in state.completedChecklistIds }
    val groups = allItems.groupBy { it.group }

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Exam-Day Readiness", "$doneCount / ${allItems.size} essentials checked")
                TextButton(onClick = { viewModel.resetAllChecklists() }) { Text("Reset") }
            }
            LinearProgressIndicator(
                progress = { if (allItems.isEmpty()) 0f else doneCount.toFloat() / allItems.size },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (groupTitle, itemsInGroup) ->
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(groupTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    itemsInGroup.forEach { item ->
                        val checked = item.id in state.completedChecklistIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleChecklistItem(item.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.toggleChecklistItem(item.id) },
                                modifier = Modifier.testTag("checklist_cb_${item.id}")
                            )
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketCheatSheetsSection() {
    val categories = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Aptitude", "Constants")
    var selected by remember { mutableStateOf("Mathematics") }

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("High-Yield EUEE Pocket Cheat Sheets", "Offline formulas, traps & grammar shortcuts")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selected == cat,
                        onClick = { selected = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selected) {
                "Mathematics" -> ReferenceData.mathFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                "Physics" -> ReferenceData.physicsFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                "Chemistry" -> ReferenceData.chemistryFormulas.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.secondary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                "Biology" -> ReferenceData.biologyTerms.forEach { b ->
                    Text("${b.term} (${b.category})", fontWeight = FontWeight.Bold)
                    Text(b.definition, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                "English" -> ReferenceData.englishGrammarRules.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.tertiary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                "Aptitude" -> ReferenceData.aptitudeShortcuts.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
                else -> ReferenceData.physicalConstants.forEach { f ->
                    Text(f.title, fontWeight = FontWeight.Bold)
                    Text(f.formula, fontFamily = MonoFontFamily, color = MaterialTheme.colorScheme.primary)
                    Text(f.description, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                }
            }
        }
    }
}
