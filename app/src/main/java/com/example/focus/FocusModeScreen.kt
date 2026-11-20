package com.example.focus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FocusModeScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshDndStatus()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("focus_mode_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Focus Zen Banner & Live Countdown
        item {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.height(210.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_focus_zen),
                        contentDescription = "Focus Mode Illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0x660B172B), Color(0xD90B172B))))
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatusPill(
                                    text = if (state.isBreakPhase) "Break Phase" else if (state.isPomodoro) "Pomodoro Focus" else "Custom Focus",
                                    containerColor = Color(0x33F59E0B),
                                    contentColor = Color(0xFFFCD34D)
                                )
                                StatusPill(
                                    text = "${state.todayFocusMinutes}/${state.dailyFocusGoalMinutes}m goal",
                                    containerColor = Color(0x330D9488),
                                    contentColor = Color(0xFF5EEAD4)
                                )
                            }

                            Text(
                                text = "%02d:%02d".format(state.remainingSeconds / 60, state.remainingSeconds % 60),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (!state.isRunning) {
                                    Button(
                                        onClick = { viewModel.startFocusSession() },
                                        modifier = Modifier.height(48.dp).testTag("start_focus_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (state.isBreakPhase) "Start Break" else "Start Focus")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.stopFocusSession(false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        modifier = Modifier.height(48.dp).testTag("stop_focus_btn")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("End Session")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configuration Card
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.sessionLabel,
                        onValueChange = { viewModel.setSessionLabel(it) },
                        label = { Text("Focus Session Label") },
                        enabled = !state.isRunning,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.isPomodoro,
                            onClick = { if (!state.isRunning) viewModel.setPomodoroMode(true) },
                            label = { Text("Pomodoro") }
                        )
                        listOf(15, 25, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = !state.isPomodoro && state.plannedMinutes == mins,
                                onClick = { if (!state.isRunning) viewModel.setCustomMinutes(mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        }

        // Native Android Do Not Disturb Integration
        item {
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Android Do Not Disturb (DND)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (state.dndPolicyAccessGranted)
                                    "Notification Policy Access granted. Enabling DND automatically restores your prior filter when the session ends."
                                else
                                    "Optional: Grant Notification Policy Access in Android Settings to silence interruptions during focus. Focus timer still works independently without DND.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.enableDndForSession,
                            onCheckedChange = { viewModel.toggleDndForSession(it) }
                        )
                    }
                    if (!state.dndPolicyAccessGranted) {
                        OutlinedButton(onClick = {
                            context.startActivity(DndManager.buildPolicyAccessIntent())
                        }) {
                            Icon(Icons.Default.DoNotDisturbOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Grant DND Policy Access")
                        }
                    } else {
                        StatusPill("DND Integration Ready", EmeraldSuccess.copy(alpha = 0.14f), EmeraldSuccess)
                    }
                    state.statusNote?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Focus History
        item {
            SectionHeader("Focus Session History", "Completed cycles: ${state.completedCycles}")
        }
        if (state.history.isNotEmpty()) {
            val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            items(state.history.take(12), key = { it.id }) { s ->
                OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(s.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${fmt.format(Date(s.startedAt))} • ${if (s.dndEnabled) "DND Active" else "Timer Only"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill("${s.actualMinutes}m", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
