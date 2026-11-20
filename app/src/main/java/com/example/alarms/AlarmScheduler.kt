package com.example.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.database.AlarmEntity
import java.time.DayOfWeek
import java.time.ZonedDateTime

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val exactGuaranteed: Boolean,
    val triggerAtMillis: Long,
    val message: String
)

object AlarmScheduler {

    const val ACTION_TRIGGER_ALARM = "com.example.action.TRIGGER_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.action.SNOOZE_ALARM"
    const val ACTION_DISMISS_ALARM = "com.example.action.DISMISS_ALARM"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    const val EXTRA_ALARM_CATEGORY = "extra_alarm_category"
    const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
    const val EXTRA_ALARM_SNOOZE_MIN = "extra_alarm_snooze_min"

    fun canScheduleExactAlarms(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        isRepeating: Boolean,
        daysOfWeekMask: Int,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Long {
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!isRepeating || daysOfWeekMask == 0) {
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1)
            }
            return candidate.toInstant().toEpochMilli()
        }

        for (offset in 0..7) {
            val dayCandidate = candidate.plusDays(offset.toLong())
            val bit = dayBitFor(dayCandidate.dayOfWeek)
            if ((daysOfWeekMask and bit) != 0 && dayCandidate.isAfter(now)) {
                return dayCandidate.toInstant().toEpochMilli()
            }
        }
        return candidate.plusDays(1).toInstant().toEpochMilli()
    }

    fun dayBitFor(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> 1 shl 0
        DayOfWeek.MONDAY -> 1 shl 1
        DayOfWeek.TUESDAY -> 1 shl 2
        DayOfWeek.WEDNESDAY -> 1 shl 3
        DayOfWeek.THURSDAY -> 1 shl 4
        DayOfWeek.FRIDAY -> 1 shl 5
        DayOfWeek.SATURDAY -> 1 shl 6
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity): AlarmScheduleResult {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return AlarmScheduleResult(false, canScheduleExactAlarms(context), 0L, "Alarm disabled.")
        }

        val triggerAt = computeNextTriggerMillis(
            hour = alarm.hour,
            minute = alarm.minute,
            isRepeating = alarm.isRepeating,
            daysOfWeekMask = alarm.daysOfWeekMask
        )
        return scheduleAtMillis(context, alarm, triggerAt)
    }

    fun scheduleSnooze(context: Context, alarm: AlarmEntity, snoozeMinutes: Int): AlarmScheduleResult {
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        return scheduleAtMillis(context, alarm, triggerAt)
    }

    private fun scheduleAtMillis(
        context: Context,
        alarm: AlarmEntity,
        triggerAt: Long
    ): AlarmScheduleResult {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = buildTriggerPendingIntent(context, alarm)
        val exactAllowed = canScheduleExactAlarms(context)

        return if (exactAllowed) {
            val showIntent = PendingIntent.getActivity(
                context,
                alarm.id.toInt() + 10_000,
                Intent(context, MainActivity::class.java).apply { putExtra("route", "alarms") },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
            AlarmScheduleResult(
                scheduled = true,
                exactGuaranteed = true,
                triggerAtMillis = triggerAt,
                message = "Exact alarm scheduled via Android AlarmManager."
            )
        } else {
            // Honest fallback: OS denied exact alarm access, use inexact window
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            AlarmScheduleResult(
                scheduled = true,
                exactGuaranteed = false,
                triggerAtMillis = triggerAt,
                message = "Exact alarm permission not granted. Scheduled as an approximate system alarm; grant 'Alarms & reminders' in Settings for exact timing."
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pi = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildTriggerPendingIntent(context: Context, alarm: AlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_CATEGORY, alarm.category)
            putExtra(EXTRA_ALARM_VIBRATE, alarm.vibrate)
            putExtra(EXTRA_ALARM_SNOOZE_MIN, alarm.snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
