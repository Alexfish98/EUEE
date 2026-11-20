package com.example.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.AccentPalette
import com.example.core.model.AppLanguage
import com.example.core.model.ThemeMode
import com.example.core.ui.SectionHeader
import com.example.focus.DndManager

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenExports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val diag by viewModel.diagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshDiagnostics() }

    LaunchedEffect(Unit) { viewModel.refreshDiagnostics() }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader("Appearance & Typography", "Material 3 theme, accent palette & accessibility scale")
            Spacer(Modifier.height(8.dp))
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ThemeMode.entries) { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccentPalette.entries) { pal ->
                            FilterChip(
                                selected = prefs.accentPalette == pal,
                                onClick = { viewModel.setAccentPalette(pal) },
                                label = { Text(pal.label) },
                                leadingIcon = {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(pal.primaryHex)))
                                }
                            )
                        }
                    }

                    Text("Font Size Scale: ${"%.2f".format(prefs.fontScale)}×", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = prefs.fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.85f..1.35f
                    )

                    Text("Language Architecture", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppLanguage.entries) { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Study, Exam & Spaced Repetition")
            Spacer(Modifier.height(8.dp))
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily Study Goal: ${prefs.dailyStudyGoalMinutes} min", style = MaterialTheme.typography.labelLarge)
                    Slider(value = prefs.dailyStudyGoalMinutes.toFloat(), onValueChange = { viewModel.setDailyStudyGoal(it.toInt()) }, valueRange = 15f..360f)

                    Text("Daily Focus Goal: ${prefs.dailyFocusGoalMinutes} min", style = MaterialTheme.typography.labelLarge)
                    Slider(value = prefs.dailyFocusGoalMinutes.toFloat(), onValueChange = { viewModel.setDailyFocusGoal(it.toInt()) }, valueRange = 15f..300f)

                    Text("Pomodoro Focus Block: ${prefs.pomodoroFocusMinutes} min", style = MaterialTheme.typography.labelLarge)
                    Slider(value = prefs.pomodoroFocusMinutes.toFloat(), onValueChange = { viewModel.setPomodoroFocus(it.toInt()) }, valueRange = 10f..90f)

                    Text("SRS Daily New Limit: ${prefs.srsDailyNewLimit} • Review Limit: ${prefs.srsDailyReviewLimit}", style = MaterialTheme.typography.labelLarge)
                    Slider(value = prefs.srsDailyNewLimit.toFloat(), onValueChange = { viewModel.setSrsNewLimit(it.toInt()) }, valueRange = 5f..100f)
                    Slider(value = prefs.srsDailyReviewLimit.toFloat(), onValueChange = { viewModel.setSrsReviewLimit(it.toInt()) }, valueRange = 10f..200f)

                    Text("Text-to-Speech Speed: ${"%.1f".format(prefs.speechRate)}×", style = MaterialTheme.typography.labelLarge)
                    Slider(value = prefs.speechRate, onValueChange = { viewModel.setSpeechRate(it) }, valueRange = 0.6f..1.8f)

                    SettingSwitch("Vibration & Haptic Feedback", prefs.vibrationEnabled) { viewModel.setVibrationEnabled(it) }
                    SettingSwitch("Sound Feedback", prefs.soundEnabled) { viewModel.setSoundEnabled(it) }
                    SettingSwitch("Study Notifications", prefs.notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
                    SettingSwitch("Keep Screen Awake in Study", prefs.keepScreenAwakeInStudy) { viewModel.setKeepAwakeStudy(it) }
                    SettingSwitch("Keep Screen Awake in Mock Exams", prefs.keepScreenAwakeInExam) { viewModel.setKeepAwakeExam(it) }
                    SettingSwitch("Auto-Enable DND on Quick Focus", prefs.dndAutoEnableForFocus) { viewModel.setDndAutoEnable(it) }
                }
            }
        }

        item {
            SectionHeader("Android System Permissions & Diagnostics")
            Spacer(Modifier.height(8.dp))
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications: ${if (diag.notificationsGranted) "Granted" else "Not Granted"}")
                        if (!diag.notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Request") }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Exact Alarms: ${if (diag.exactAlarmsGranted) "Granted" else "Denied"}")
                        if (!diag.exactAlarmsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }) { Text("Grant") }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("DND Policy Access: ${if (diag.dndPolicyGranted) "Granted" else "Not Granted"}")
                        if (!diag.dndPolicyGranted) {
                            OutlinedButton(onClick = { context.startActivity(DndManager.buildPolicyAccessIntent()) }) { Text("Grant") }
                        }
                    }
                    HorizontalDivider()
                    Text("Storage & Database Diagnostics", fontWeight = FontWeight.Bold)
                    Text("SQLite Size: ${diag.dbFileBytes / 1024} KB • Questions: ${diag.questionCount} • Notes: ${diag.noteCount} • Exams: ${diag.examCount}")
                    Button(onClick = onOpenExports, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Backup & Restore Center")
                    }
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("About & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "EUEE Mastery v1.0 operates 100% offline with zero telemetry, zero advertising SDKs, and zero remote data uploads. All questions, notes, flashcards, exam results, alarms, and focus logs stay exclusively on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
