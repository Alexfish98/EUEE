package com.example.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.database.EueeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = EueeDatabase.getInstance(context)
                    val enabledAlarms = db.plannerAndAlarmDao().getEnabledAlarms()
                    enabledAlarms.forEach { alarm ->
                        AlarmScheduler.scheduleAlarm(context, alarm)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
