package com.pathhelper.ai.haptics

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.GuidanceDecision
/**
* Coordinates Haptic Guidance Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Haptic Guidance Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class HapticGuidanceEngine {
    private var lastHapticAction: GuidanceAction? = null
    private var lastHapticTimestamp: Long = 0L

    private var generatedCount = 0
    private var suppressedCount = 0

    fun process(
        decision: GuidanceDecision
    ): Pair<HapticCommand?, HapticMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        try {
            val pattern = when (decision.action) {
                GuidanceAction.MOVE_LEFT -> HapticPattern.LEFT
                GuidanceAction.MOVE_SLIGHTLY_LEFT -> HapticPattern.SLIGHTLY_LEFT
                GuidanceAction.MOVE_RIGHT -> HapticPattern.RIGHT
                GuidanceAction.MOVE_SLIGHTLY_RIGHT -> HapticPattern.SLIGHTLY_RIGHT
                GuidanceAction.KEEP_CENTER -> HapticPattern.CENTER
                GuidanceAction.STOP -> HapticPattern.STOP
                GuidanceAction.WAIT -> HapticPattern.WAIT
            }

            val priority = when (decision.action) {
                GuidanceAction.STOP -> 100
                GuidanceAction.WAIT -> 80
                GuidanceAction.MOVE_LEFT -> 60
                GuidanceAction.MOVE_RIGHT -> 60
                GuidanceAction.MOVE_SLIGHTLY_LEFT -> 50
                GuidanceAction.MOVE_SLIGHTLY_RIGHT -> 50
                GuidanceAction.KEEP_CENTER -> 40
            }

            val isSameAction = decision.action == lastHapticAction
            val timeElapsed = currentTime - lastHapticTimestamp
            val shouldSuppress = isSameAction && timeElapsed < 3000L

            if (shouldSuppress) {
                suppressedCount++
                if (BuildConfig.DEBUG) {
                    Log.d("HapticGuidance", "Action: ${decision.action.name} Suppressed")
                }
                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(
                    null,
                    HapticMetadata(
                        generatedCommands = generatedCount,
                        suppressedCommands = suppressedCount,
                        processingTimeMs = duration,
                        successful = true
                    )
                )
            }

            generatedCount++
            lastHapticAction = decision.action
            lastHapticTimestamp = currentTime

            if (BuildConfig.DEBUG) {
                if (decision.action == GuidanceAction.STOP) {
                    Log.d("HapticGuidance", "Action: STOP Pattern: STOP Interrupting Active Pattern")
                } else {
                    Log.d("HapticGuidance", "Action: ${decision.action.name} Pattern: ${pattern.name} Executed")
                }
            }

            val command = HapticCommand(
                pattern = pattern,
                action = decision.action,
                timestamp = currentTime,
                priority = priority
            )

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                command,
                HapticMetadata(
                    generatedCommands = generatedCount,
                    suppressedCommands = suppressedCount,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                null,
                HapticMetadata(
                    generatedCommands = generatedCount,
                    suppressedCommands = suppressedCount,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown Haptic Guidance engine processing error."
                )
            )
        }
    }
}
