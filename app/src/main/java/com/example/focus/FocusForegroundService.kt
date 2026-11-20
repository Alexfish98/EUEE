package com.example.focus

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.*

class FocusForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.createChannels(this)
        when (intent?.action) {
            ACTION_STOP -> {
                stopFocusAndSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Deep EUEE Study"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MIN, 25)
                val endEpochMs = System.currentTimeMillis() + durationMinutes * 60_000L

                val notification = buildNotification(label, "$durationMinutes:00 remaining")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startCountdown(label, durationMinutes, endEpochMs)
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(label: String, durationMinutes: Int, endEpochMs: Long) {
        timerJob?.cancel()
        val prefs = PreferencesRepository(applicationContext)
        timerJob = serviceScope.launch {
            while (isActive) {
                val remainingMs = (endEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
                val totalSec = remainingMs / 1000L
                val min = totalSec / 60L
                val sec = totalSec % 60L
                val formatted = "%02d:%02d remaining".format(min, sec)

                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(label, formatted))

                if (remainingMs <= 0L) {
                    val prevFilter = prefs.getSavedPrevDndFilter()
                    DndManager.restoreDnd(applicationContext, prevFilter)
                    prefs.saveFocusRuntimeState(false, 0L, null)
                    NotificationHelper.showFocusCompletedNotification(applicationContext, label, durationMinutes)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun stopFocusAndSelf() {
        timerJob?.cancel()
        val prefs = PreferencesRepository(applicationContext)
        serviceScope.launch {
            val prevFilter = prefs.getSavedPrevDndFilter()
            DndManager.restoreDnd(applicationContext, prevFilter)
            prefs.saveFocusRuntimeState(false, 0L, null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(label: String, subtitle: String) =
        NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus: $label")
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    501,
                    Intent(this, MainActivity::class.java).apply { putExtra("route", "focus") },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                "End Session",
                PendingIntent.getService(
                    this,
                    502,
                    Intent(this, FocusForegroundService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.focus.START"
        const val ACTION_STOP = "com.example.focus.STOP"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_DURATION_MIN = "extra_duration_min"
        private const val NOTIFICATION_ID = 5001
    }
}
