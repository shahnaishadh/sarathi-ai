package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
/**
* Coordinates Guidance Decision Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Guidance Decision Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class GuidanceDecisionEngine {

    private var stopUntilTimestamp = 0L
    private var lastAction: GuidanceAction = GuidanceAction.KEEP_CENTER

    fun process(
        threats: List<ThreatPriority>,
        corridors: List<SafeCorridor>
    ): Pair<GuidanceDecision, GuidanceMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        val metadata = GuidanceMetadata(
            evaluatedThreats = threats.size,
            evaluatedCorridors = corridors.size,
            decisionTimeMs = 0L,
            successful = false
        )

        // Safety Overrides
        val highestThreat = threats.maxByOrNull { it.priorityScore }
        val centerObstacle = threats.find { it.horizontalZone == HorizontalZone.CENTER && it.distanceMeters <= 1.2f }

        if (currentTime < stopUntilTimestamp || centerObstacle != null) {
            val duration = currentTime - startTime
            if (centerObstacle != null) stopUntilTimestamp = currentTime + 1500L
            lastAction = GuidanceAction.STOP

            return Pair(
                GuidanceDecision(
                    action = GuidanceAction.STOP,
                    reason = centerObstacle?.let { "${it.className} detected ahead" } ?: "Safety hold active",
                    selectedCorridor = null,
                    highestThreatId = centerObstacle?.trackId,
                    highestThreatClassName = centerObstacle?.className,
                    highestThreatLevel = centerObstacle?.threatLevel ?: ThreatLevel.CRITICAL,
                    confidence = 1.0f,
                    highestThreatDistance = centerObstacle?.distanceMeters
                ),
                metadata.copy(decisionTimeMs = duration, successful = true)
            )
        }

        // --- Local Navigation Logic (5-Zone) ---
        val sharpLeftCor = corridors.find { it.horizontalZone == HorizontalZone.SHARP_LEFT }
        val leftCor = corridors.find { it.horizontalZone == HorizontalZone.LEFT }
        val centerCor = corridors.find { it.horizontalZone == HorizontalZone.CENTER }
        val rightCor = corridors.find { it.horizontalZone == HorizontalZone.RIGHT }
        val sharpRightCor = corridors.find { it.horizontalZone == HorizontalZone.SHARP_RIGHT }

        val sLeftScore = sharpLeftCor?.score ?: 0f
        val leftScore = leftCor?.score ?: 0f
        val centerScore = centerCor?.score ?: 0f
        val rightScore = rightCor?.score ?: 0f
        val sRightScore = sharpRightCor?.score ?: 0f

        // Apply hysteresis bias for decision stability
        var biasedLeftScore = leftScore
        var biasedRightScore = rightScore
        var biasedSLeftScore = sLeftScore
        var biasedSRightScore = sRightScore

        val isLastActionLeft = lastAction in listOf(GuidanceAction.MOVE_LEFT, GuidanceAction.MOVE_SLIGHTLY_LEFT, GuidanceAction.MOVE_SHARP_LEFT)
        val isLastActionRight = lastAction in listOf(GuidanceAction.MOVE_RIGHT, GuidanceAction.MOVE_SLIGHTLY_RIGHT, GuidanceAction.MOVE_SHARP_RIGHT)

        if (isLastActionLeft) {
            biasedLeftScore += 15f
            biasedSLeftScore += 15f
        } else if (isLastActionRight) {
            biasedRightScore += 15f
            biasedSRightScore += 15f
        }

        // Rule 1: Center is SAFE -> Keep going
        if (centerScore >= 70f) {
            return generateDecision(GuidanceAction.KEEP_CENTER, "Path clear", HorizontalZone.CENTER, highestThreat, startTime, metadata)
        }

        // Rule 2: Center is cautioned/blocked -> Evaluate best alternative symmetrically
        return when {
            // Standard Left vs Standard Right (using raw scores for thresholds, biased scores for comparison)
            leftScore >= 70f && biasedLeftScore >= biasedRightScore -> {
                val action = if (centerScore >= 45f) GuidanceAction.MOVE_SLIGHTLY_LEFT else GuidanceAction.MOVE_LEFT
                generateDecision(action, "Avoid obstacle via left", HorizontalZone.LEFT, highestThreat, startTime, metadata)
            }
            rightScore >= 70f && biasedRightScore > biasedLeftScore -> {
                val action = if (centerScore >= 45f) GuidanceAction.MOVE_SLIGHTLY_RIGHT else GuidanceAction.MOVE_RIGHT
                generateDecision(action, "Avoid obstacle via right", HorizontalZone.RIGHT, highestThreat, startTime, metadata)
            }

            // Sharp Left vs Sharp Right
            sLeftScore >= 70f && biasedSLeftScore >= biasedSRightScore -> {
                generateDecision(GuidanceAction.MOVE_SHARP_LEFT, "Sharp turn left", HorizontalZone.SHARP_LEFT, highestThreat, startTime, metadata)
            }
            sRightScore >= 70f && biasedSRightScore > biasedSLeftScore -> {
                generateDecision(GuidanceAction.MOVE_SHARP_RIGHT, "Sharp turn right", HorizontalZone.SHARP_RIGHT, highestThreat, startTime, metadata)
            }

            // Fallback: Standard Left vs Standard Right (Caution levels)
            leftScore >= 30f && biasedLeftScore >= biasedRightScore -> {
                generateDecision(GuidanceAction.MOVE_LEFT, "Turn left", HorizontalZone.LEFT, highestThreat, startTime, metadata)
            }
            rightScore >= 30f && biasedRightScore > biasedLeftScore -> {
                generateDecision(GuidanceAction.MOVE_RIGHT, "Turn right", HorizontalZone.RIGHT, highestThreat, startTime, metadata)
            }

            // Fallback: Sharp Left vs Sharp Right (Caution levels)
            sLeftScore >= 30f && biasedSLeftScore >= biasedSRightScore -> {
                generateDecision(GuidanceAction.MOVE_SHARP_LEFT, "Sharp turn left", HorizontalZone.SHARP_LEFT, highestThreat, startTime, metadata)
            }
            sRightScore >= 30f && biasedSRightScore > biasedSLeftScore -> {
                generateDecision(GuidanceAction.MOVE_SHARP_RIGHT, "Sharp turn right", HorizontalZone.SHARP_RIGHT, highestThreat, startTime, metadata)
            }

            else -> 
                generateDecision(GuidanceAction.WAIT, "All paths blocked", null, highestThreat, startTime, metadata)
        }
    }

    private fun generateDecision(
        action: GuidanceAction,
        reason: String,
        zone: HorizontalZone?,
        threat: ThreatPriority?,
        startTime: Long,
        metadata: GuidanceMetadata,
        secondaryAction: GuidanceAction? = null,
        secondaryReason: String? = null
    ): Pair<GuidanceDecision, GuidanceMetadata> {
        val duration = SystemClock.elapsedRealtime() - startTime
        val decision = GuidanceDecision(
            action = action,
            reason = reason,
            selectedCorridor = zone,
            highestThreatId = threat?.trackId,
            highestThreatClassName = threat?.className,
            highestThreatLevel = threat?.threatLevel,
            confidence = 0.9f,
            secondaryAction = secondaryAction,
            secondaryReason = secondaryReason,
            highestThreatDistance = threat?.distanceMeters
        )
        if (BuildConfig.DEBUG) Log.d("DecisionEngine", "action=${decision.action} reason=$reason secondary=${decision.secondaryAction}")
        lastAction = action
        return Pair(decision, metadata.copy(decisionTimeMs = duration, successful = true))
    }
}
