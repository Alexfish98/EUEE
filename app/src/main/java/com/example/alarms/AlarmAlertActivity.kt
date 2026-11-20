package com.example.alarms

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EueeMasteryTheme

class AlarmAlertActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "EUEE Study Alarm"
        val category = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_CATEGORY) ?: "STUDY_REMINDER"
        val snoozeMin = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_SNOOZE_MIN, 10)

        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.also { it.play() }
        }

        setContent {
            EueeMasteryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm ringing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = category.replace('_', ' '),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    stopRingtone()
                                    sendBroadcast(Intent(this@AlarmAlertActivity, AlarmReceiver::class.java).apply {
                                        action = AlarmScheduler.ACTION_SNOOZE_ALARM
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                                        putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
                                        putExtra(AlarmScheduler.EXTRA_ALARM_SNOOZE_MIN, snoozeMin)
                                    })
                                    finish()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Default.Snooze, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Snooze ${snoozeMin}m")
                            }

                            Button(
                                onClick = {
                                    stopRingtone()
                                    sendBroadcast(Intent(this@AlarmAlertActivity, AlarmReceiver::class.java).apply {
                                        action = AlarmScheduler.ACTION_DISMISS_ALARM
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                                        putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
                                    })
                                    finish()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopRingtone() {
        runCatching { ringtone?.stop() }
    }

    override fun onDestroy() {
        stopRingtone()
        super.onDestroy()
    }
}
