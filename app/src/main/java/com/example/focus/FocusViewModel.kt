package com.example.focus

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.haptic.HapticHelper
import com.example.data.EueeRepository
import com.example.data.PreferencesRepository
import com.example.database.FocusSessionEntity
import com.example.widgets.WidgetUpdateHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FocusUiState(
    val isRunning: Boolean = false,
    val isBreakPhase: Boolean = false,
    val sessionLabel: String = "Deep EUEE Focus",
    val plannedMinutes: Int = 25,
    val remainingSeconds: Long = 25 * 60L,
    val completedCycles: Int = 0,
    val isPomodoro: Boolean = true,
    val enableDndForSession: Boolean = false,
    val dndPolicyAccessGranted: Boolean = false,
    val todayFocusMinutes: Int = 0,
    val dailyFocusGoalMinutes: Int = 90,
    val history: List<FocusSessionEntity> = emptyList(),
    val statusNote: String? = null
)

class FocusViewModel(
    private val appContext: Context,
    private val repository: EueeRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _localState = MutableStateFlow(FocusUiState())
    private var tickerJob: Job? = null
    private var sessionStartedAtMs: Long = 0L

    val uiState: StateFlow<FocusUiState> = combine(
        _localState,
        preferencesRepository.preferencesFlow,
        repository.observeTodayFocusMinutes(),
        repository.observeFocusSessions()
    ) { local, prefs, todayMin, history ->
        val dnd = DndManager.getStatus(appContext)
        local.copy(
            dndPolicyAccessGranted = dnd.hasPolicyAccess,
            todayFocusMinutes = todayMin,
            dailyFocusGoalMinutes = prefs.dailyFocusGoalMinutes,
            history = history,
            plannedMinutes = if (!local.isRunning) {
                if (local.isPomodoro) prefs.pomodoroFocusMinutes else local.plannedMinutes
            } else local.plannedMinutes,
            remainingSeconds = if (!local.isRunning && local.remainingSeconds == 0L) {
                prefs.pomodoroFocusMinutes * 60L
            } else local.remainingSeconds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    init {
        viewModelScope.launch {
            val (active, endMs) = preferencesRepository.focusActiveFlow.first()
            val prefs = preferencesRepository.preferencesFlow.first()
            if (active && endMs > System.currentTimeMillis()) {
                val remSec = (endMs - System.currentTimeMillis()) / 1000L
                _localState.update {
                    it.copy(
                        isRunning = true,
                        remainingSeconds = remSec,
                        enableDndForSession = prefs.dndAutoEnableForFocus
                    )
                }
                startUiTicker(endMs)
            } else {
                _localState.update {
                    it.copy(
                        plannedMinutes = prefs.pomodoroFocusMinutes,
                        remainingSeconds = prefs.pomodoroFocusMinutes * 60L,
                        enableDndForSession = prefs.dndAutoEnableForFocus && DndManager.getStatus(appContext).hasPolicyAccess
                    )
                }
            }
        }
    }

    fun setSessionLabel(label: String) { _localState.update { it.copy(sessionLabel = label) } }

    fun setCustomMinutes(mins: Int) {
        val m = mins.coerceIn(5, 180)
        _localState.update { it.copy(isPomodoro = false, plannedMinutes = m, remainingSeconds = m * 60L) }
    }

    fun setPomodoroMode(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val m = if (enabled) prefs.pomodoroFocusMinutes else _localState.value.plannedMinutes
            _localState.update { it.copy(isPomodoro = enabled, plannedMinutes = m, remainingSeconds = m * 60L) }
        }
    }

    fun toggleDndForSession(enable: Boolean) {
        val hasAccess = DndManager.getStatus(appContext).hasPolicyAccess
        if (enable && !hasAccess) {
            _localState.update {
                it.copy(
                    enableDndForSession = false,
                    statusNote = "Notification Policy Access is required before enabling Android Do Not Disturb. Independent Focus Timer works without it."
                )
            }
            return
        }
        _localState.update { it.copy(enableDndForSession = enable, statusNote = null) }
    }

    fun refreshDndStatus() {
        val hasAccess = DndManager.getStatus(appContext).hasPolicyAccess
        _localState.update { it.copy(dndPolicyAccessGranted = hasAccess) }
    }

    fun startFocusSession() {
        val current = _localState.value
        if (current.isRunning) return

        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val durationMin = if (current.isBreakPhase) {
                if ((current.completedCycles + 1) % 4 == 0) prefs.pomodoroLongBreakMinutes else prefs.pomodoroShortBreakMinutes
            } else {
                current.plannedMinutes
            }
            val endEpochMs = System.currentTimeMillis() + durationMin * 60_000L
            sessionStartedAtMs = System.currentTimeMillis()

            val prevFilter = if (current.enableDndForSession && !current.isBreakPhase) {
                DndManager.enableFocusDnd(appContext)
            } else null

            preferencesRepository.saveFocusRuntimeState(true, endEpochMs, prevFilter)

            val serviceIntent = Intent(appContext, FocusForegroundService::class.java).apply {
                action = FocusForegroundService.ACTION_START
                putExtra(FocusForegroundService.EXTRA_LABEL, current.sessionLabel.ifBlank { "EUEE Focus" })
                putExtra(FocusForegroundService.EXTRA_DURATION_MIN, durationMin)
            }
            runCatching { ContextCompat.startForegroundService(appContext, serviceIntent) }

            HapticHelper.tap(appContext, prefs.vibrationEnabled)
            _localState.update {
                it.copy(
                    isRunning = true,
                    remainingSeconds = durationMin * 60L,
                    statusNote = if (prevFilter != null) "Focus active with Android DND enabled (previous state saved)." else "Focus active (independent timer mode)."
                )
            }
            WidgetUpdateHelper.requestRefreshAll(appContext)
            startUiTicker(endEpochMs)
        }
    }

    fun stopFocusSession(completedNaturally: Boolean = false) {
        tickerJob?.cancel()
        val current = _localState.value
        if (!current.isRunning && !completedNaturally) return

        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val prevFilter = preferencesRepository.getSavedPrevDndFilter()
            DndManager.restoreDnd(appContext, prevFilter)
            preferencesRepository.saveFocusRuntimeState(false, 0L, null)

            runCatching {
                appContext.startService(Intent(appContext, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_STOP
                })
            }

            val elapsedMin = if (completedNaturally) {
                current.plannedMinutes
            } else {
                ((System.currentTimeMillis() - sessionStartedAtMs) / 60_000L).toInt().coerceAtLeast(1)
            }

            if (!current.isBreakPhase) {
                repository.recordFocusSession(
                    FocusSessionEntity(
                        label = current.sessionLabel.ifBlank { "EUEE Focus" },
                        plannedMinutes = current.plannedMinutes,
                        actualMinutes = elapsedMin,
                        isPomodoro = current.isPomodoro,
                        completedCycles = current.completedCycles + if (completedNaturally) 1 else 0,
                        dndEnabled = prevFilter != null,
                        startedAt = sessionStartedAtMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        endedAt = System.currentTimeMillis()
                    )
                )
            }

            HapticHelper.success(appContext, prefs.vibrationEnabled)
            val nextBreak = completedNaturally && current.isPomodoro && !current.isBreakPhase
            _localState.update {
                it.copy(
                    isRunning = false,
                    isBreakPhase = nextBreak,
                    completedCycles = it.completedCycles + if (completedNaturally && !it.isBreakPhase) 1 else 0,
                    remainingSeconds = if (nextBreak) prefs.pomodoroShortBreakMinutes * 60L else prefs.pomodoroFocusMinutes * 60L,
                    statusNote = if (prevFilter != null) "Session ended & previous DND mode restored." else "Focus session recorded."
                )
            }
            WidgetUpdateHelper.requestRefreshAll(appContext)
        }
    }

    private fun startUiTicker(endEpochMs: Long) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val rem = ((endEpochMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                _localState.update { it.copy(remainingSeconds = rem) }
                if (rem <= 0L) {
                    stopFocusSession(completedNaturally = true)
                    break
                }
                delay(1000L)
            }
        }
    }
}
