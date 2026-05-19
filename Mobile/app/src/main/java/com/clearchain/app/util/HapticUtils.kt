package com.clearchain.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

object HapticUtils {

    /** Short tap — confirm actions, button press */
    fun confirm(context: Context) = vibrate(context, 40)

    /** Light tick — selection change, toggle */
    fun tick(context: Context) = vibrate(context, 15)

    /** Double bump — destructive action warning */
    fun warning(context: Context) {
        vibrate(context, 30)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            vibrate(context, 60)
        }, 80)
    }

    /** Pull-to-refresh snap */
    fun refresh(context: Context) = vibrate(context, 25)

    private fun vibrate(context: Context, durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService<VibratorManager>() ?: return
            val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            manager.defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService<Vibrator>() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }
}
