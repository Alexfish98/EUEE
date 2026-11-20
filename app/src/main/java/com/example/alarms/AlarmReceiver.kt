package com.example.alarms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.database.AlarmHistoryEntity
import com.example.database.EueeDatabase
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "EUEE Study Alarm"
        val category = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_CATEGORY) ?: "STUDY_REMINDER"
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_VIBRATE, true)
        val snoozeMin = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_SNOOZE_MIN, 10)

        NotificationHelper.createChannels(context)

        when (intent.action) {
            AlarmScheduler.ACTION_TRIGGER_ALARM -> {
                if (vibrate) triggerAlarmVibration(context)
                showAlarmNotification(context, alarmId, label, category, snoozeMin)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = EueeDatabase.getInstance(context)
                        val dao = db.plannerAndAlarmDao()
                        dao.insertAlarmHistory(
                            AlarmHistoryEntity(
                                alarmId = alarmId,
                                label = label,
                                actionType = "TRIGGERED"
                            )
                        )
                        val alarm = dao.getAlarmById(alarmId)
                        if (alarm != null) {
                            if (alarm.isRepeating) {
                                AlarmScheduler.scheduleAlarm(context, alarm)
                            } else {
                                dao.upsertAlarm(alarm.copy(isEnabled = false))
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_SNOOZE_ALARM -> {
                NotificationManagerCompat.from(context).cancel(alarmId.toInt() + 2000)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = EueeDatabase.getInstance(context)
                        val dao = db.plannerAndAlarmDao()
                        val alarm = dao.getAlarmById(alarmId)
                        if (alarm != null) {
                            AlarmScheduler.scheduleSnooze(context, alarm, snoozeMin)
                        }
                        dao.insertAlarmHistory(
                            AlarmHistoryEntity(
                                alarmId = alarmId,
                                label = label,
                                actionType = "SNOOZED"
                            )
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_DISMISS_ALARM -> {
                NotificationManagerCompat.from(context).cancel(alarmId.toInt() + 2000)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = EueeDatabase.getInstance(context)
                        db.plannerAndAlarmDao().insertAlarmHistory(
                            AlarmHistoryEntity(
                                alarmId = alarmId,
                                label = label,
                                actionType = "DISMISSED"
                            )
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun triggerAlarmVibration(context: Context) {
        runCatching {
            val pattern = longArrayOf(0, 400, 200, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        }
    }

    private fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        label: String,
        category: String,
        snoozeMin: Int
    ) {
        if (!NotificationHelper.hasNotificationPermission(context)) return

        val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_ALARM_CATEGORY, category)
            putExtra(AlarmScheduler.EXTRA_ALARM_SNOOZE_MIN, snoozeMin)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            alarmId.toInt() + 3000,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePi = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 4000,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_SNOOZE_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
                putExtra(AlarmScheduler.EXTRA_ALARM_SNOOZE_MIN, snoozeMin)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPi = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 5000,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_DISMISS_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText("EUEE Mastery Alarm • Tap to open or snooze")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setContentIntent(contentPi)
            .addAction(0, "Snooze ${snoozeMin}m", snoozePi)
            .addAction(0, "Dismiss", dismissPi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(alarmId.toInt() + 2000, notification)
    }
}
