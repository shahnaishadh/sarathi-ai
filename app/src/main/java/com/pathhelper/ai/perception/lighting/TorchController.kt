package com.pathhelper.ai.perception.lighting

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controls the device flashlight based on real-time brightness scores.
 *
 * ## Activation rules
 * - Auto-enable when [LowLightState.DARK] for **3 consecutive frames**.
 * - Auto-disable when brightness score remains >= [RECOVERY_THRESHOLD] for **10 seconds**.
 * - Voice prompt spoken at most once per 30 seconds.
 * - Additional low-confidence warning voiced when still dark and localization drops below [CONFIDENCE_WARN_THRESHOLD].
 *
 * ## Hysteresis
 * Activation and deactivation use separate thresholds and time guards to prevent rapid toggling.
 *
 * @param cameraManager     System [CameraManager] for torch API access.
 * @param cameraId          Camera ID to toggle torch on (typically "0" = rear).
 * @param speak             Lambda to invoke TTS (called at most once per 30 s for torch prompts).
 * @param onStateChanged    Optional callback receiving (LowLightState, brightnessScore, torchOn).
 */
class TorchController(
    private val cameraManager: CameraManager,
    private val cameraId: String,
    private val speak: (String) -> Unit,
    private val onStateChanged: ((LowLightState, Float, Boolean) -> Unit)? = null,
    private val toggleTorch: ((Boolean) -> Unit)? = null
) {

    companion
object {
        private const val TAG = "PathHelper.Torch"

        private const val DARK_FRAMES_REQUIRED       = 3
        private const val RECOVERY_THRESHOLD         = 70f   // brightness score to consider recovered
        private const val RECOVERY_HOLD_MS           = 10_000L
        private const val VOICE_COOLDOWN_MS          = 30_000L
        private const val CONFIDENCE_WARN_THRESHOLD  = 0.5f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Current lighting state (latest classification). */
    var currentState: LowLightState = LowLightState.NORMAL
        private set

    /** Latest brightness score [0, 100]. */
    var brightnessScore: Float = 0f
        private set

    /** Whether the torch is currently on. */
    @Volatile
    var torchOn: Boolean = false
        private set

    @Volatile
    private var darkFrameCount = 0
    private var lastVoicePromptMs = 0L
    private var recoveryJob: Job? = null

    /**
     * Call once per camera frame with the brightness score from [AmbientLightAnalyzer].
     *
     * @param score              Brightness score [0, 100].
     * @param localizationConf   Current localization confidence [0, 1] (used for secondary warning).
     */
    fun onFrame(score: Float, localizationConf: Float = 1f) {
        brightnessScore = score
        currentState = LowLightState.fromScore(score)
        Log.i("SARTHI_LIGHTING", "time=${System.currentTimeMillis()} brightness=$score threshold=15.0 isDark=${currentState == LowLightState.DARK}")
        onStateChanged?.invoke(currentState, score, torchOn)

        when (currentState) {
            LowLightState.DARK -> {
                darkFrameCount++
                cancelRecoveryJob()

                if (darkFrameCount >= DARK_FRAMES_REQUIRED && !torchOn) {
                    enableTorch()
                    maybeSpeak("Low light detected. Flashlight enabled.")
                }

                // Secondary warning: still dark + low localization confidence
                if (torchOn && localizationConf < CONFIDENCE_WARN_THRESHOLD) {
                    maybeSpeak("Environment is very dark. Localization confidence reduced.")
                }
            }
            else -> {
                darkFrameCount = 0
                if (torchOn && score >= RECOVERY_THRESHOLD) {
                    scheduleRecovery()
                }
            }
        }
    }

    /** Release torch resources (call from onPause / onDestroy). */
    fun release() {
        cancelRecoveryJob()
        if (torchOn) disableTorch()
    }

    fun updateTorchStateFromHardware(isOn: Boolean) {
        this.torchOn = isOn
        if (isOn) {
            darkFrameCount = DARK_FRAMES_REQUIRED
        } else {
            darkFrameCount = 0
        }
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun enableTorch() {
        setTorch(true)
    }

    private fun disableTorch() {
        setTorch(false)
    }

    private fun setTorch(enable: Boolean) {
        Log.i("SARTHI_TORCH", "time=${System.currentTimeMillis()} requestedState=$enable actualState=$enable")
        if (toggleTorch != null) {
            try {
                toggleTorch.invoke(enable)
                torchOn = enable
                if (BuildConfig.DEBUG) Log.d(TAG, "Torch (via toggleTorch) ${if (enable) "ON" else "OFF"}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set torch mode via toggleTorch: ${e.localizedMessage}")
            }
        } else {
            try {
                cameraManager.setTorchMode(cameraId, enable)
                torchOn = enable
                if (BuildConfig.DEBUG) Log.d(TAG, "Torch ${if (enable) "ON" else "OFF"}")
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to set torch mode: ${e.localizedMessage}")
            }
        }
    }

    private fun scheduleRecovery() {
        if (recoveryJob?.isActive == true) return   // already waiting
        recoveryJob = scope.launch {
            delay(RECOVERY_HOLD_MS)
            // Confirm brightness is still above threshold before disabling
            if (brightnessScore >= RECOVERY_THRESHOLD && torchOn) {
                disableTorch()
                if (BuildConfig.DEBUG) Log.d(TAG, "Torch disabled after recovery hold")
            }
        }
    }

    private fun cancelRecoveryJob() {
        recoveryJob?.cancel()
        recoveryJob = null
    }

    private fun maybeSpeak(message: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastVoicePromptMs >= VOICE_COOLDOWN_MS) {
            speak(message)
            lastVoicePromptMs = now
            if (BuildConfig.DEBUG) Log.d(TAG, "Voice prompt: \"$message\"")
        }
    }
}
