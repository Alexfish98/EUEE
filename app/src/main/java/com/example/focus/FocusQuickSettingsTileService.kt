package com.example.focus

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusQuickSettingsTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val prefs = PreferencesRepository(applicationContext)
            val (active, endMs) = prefs.focusActiveFlow.first()
            val isRunning = active && endMs > System.currentTimeMillis()
            qsTile?.apply {
                state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (isRunning) "Focusing" else "EUEE Focus"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = if (isRunning) "Tap to stop" else "25 min"
                }
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val prefs = PreferencesRepository(applicationContext)
            val (active, endMs) = prefs.focusActiveFlow.first()
            val isRunning = active && endMs > System.currentTimeMillis()

            if (isRunning) {
                val stopIntent = Intent(applicationContext, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_STOP
                }
                startService(stopIntent)
                prefs.saveFocusRuntimeState(false, 0L, null)
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "EUEE Focus"
                    updateTile()
                }
            } else {
                val userPrefs = prefs.preferencesFlow.first()
                val durationMin = userPrefs.pomodoroFocusMinutes
                val endEpochMs = System.currentTimeMillis() + durationMin * 60_000L
                val prevFilter = if (userPrefs.dndAutoEnableForFocus) {
                    DndManager.enableFocusDnd(applicationContext)
                } else {
                    null
                }
                prefs.saveFocusRuntimeState(true, endEpochMs, prevFilter)
                val startIntent = Intent(applicationContext, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_START
                    putExtra(FocusForegroundService.EXTRA_LABEL, "Quick Settings Focus")
                    putExtra(FocusForegroundService.EXTRA_DURATION_MIN, durationMin)
                }
                ContextCompat.startForegroundService(applicationContext, startIntent)
                qsTile?.apply {
                    state = Tile.STATE_ACTIVE
                    label = "Focusing"
                    updateTile()
                }
            }
        }
    }
}
