package com.pathhelper.ai.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
/**
* Coordinates Haptic Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Haptic Manager.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
/**
* Coordinates Haptic Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Haptic Manager.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
/**
* Coordinates Haptic Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Haptic Manager.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
import android.os.VibratorManager@SuppressLint("MissingPermission")
class HapticManager(private val context: Context) {
    private var vibrator: Vibrator? = null

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun play(command: HapticCommand) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        // STOP command must interrupt active vibration immediately
        if (command.pattern == HapticPattern.STOP) {
            v.cancel()
        }

        val timings: LongArray
        val amplitudes: IntArray
        val repeatIndex: Int // -1 for no repeat

        when (command.pattern) {
            HapticPattern.LEFT -> {
                // Short, Pause, Short
                timings = longArrayOf(0, 150, 150, 150)
                amplitudes = intArrayOf(0, 255, 0, 255)
                repeatIndex = -1
            }
            HapticPattern.SLIGHTLY_LEFT -> {
                // Very short pulse
                timings = longArrayOf(0, 100)
                amplitudes = intArrayOf(0, 180)
                repeatIndex = -1
            }
            HapticPattern.RIGHT -> {
                // Long, Short
                timings = longArrayOf(0, 400, 150, 150)
                amplitudes = intArrayOf(0, 255, 0, 255)
                repeatIndex = -1
            }
            HapticPattern.SLIGHTLY_RIGHT -> {
                // Medium pulse
                timings = longArrayOf(0, 250)
                amplitudes = intArrayOf(0, 180)
                repeatIndex = -1
            }
            HapticPattern.CENTER -> {
                // Single gentle pulse
                timings = longArrayOf(0, 100)
                amplitudes = intArrayOf(0, 128)
                repeatIndex = -1
            }
            HapticPattern.WAIT -> {
                // Double pulse
                timings = longArrayOf(0, 150, 150, 150)
                amplitudes = intArrayOf(0, 200, 0, 200)
                repeatIndex = -1
            }
            HapticPattern.STOP -> {
                // Rapid emergency pulses
                timings = longArrayOf(0, 100, 50, 100, 50, 100, 50, 100)
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
                repeatIndex = -1
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, repeatIndex)
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }

    fun shutdown() {
        cancel()
        vibrator = null
    }
}
