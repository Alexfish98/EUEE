package com.example.core.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {

    fun tap(context: Context, enabled: Boolean) {
        if (!enabled) return
        vibrateMs(context, 18L, 80)
    }

    fun success(context: Context, enabled: Boolean) {
        if (!enabled) return
        vibrateMs(context, 45L, 140)
    }

    fun error(context: Context, enabled: Boolean) {
        if (!enabled) return
        runCatching {
            val pattern = longArrayOf(0, 60, 50, 60)
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun vibrateMs(context: Context, ms: Long, amplitude: Int) {
        runCatching {
            getVibrator(context)?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
