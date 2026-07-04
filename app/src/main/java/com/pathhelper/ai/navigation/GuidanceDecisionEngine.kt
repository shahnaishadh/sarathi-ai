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
    private var lastScoreDiff = 0f

    fun process(
        threats: List<ThreatPriority>,
        corridors: List<SafeCorridor>,
        frameId: Long = 0L,
        isSpeaking: Boolean = false
    ): Pair<GuidanceDecision, GuidanceMetadata> {
        val result = processInternal(threats, corridors, isSpeaking)
        val decision = result.first

        Log.i("SARTHI_GUIDANCE_DECISION", "action=${decision.action} timestamp=${System.currentTimeMillis()}")

        for (threat in threats) {
            val objectCenterX = when (threat.horizontalZone) {
                HorizontalZone.SHARP_LEFT -> 48f
                HorizontalZone.LEFT -> 160f
                HorizontalZone.CENTER -> 320f
                HorizontalZone.RIGHT -> 480f
                HorizontalZone.SHARP_RIGHT -> 592f
            }
            Log.i("SARTHI_POSITION", "objectCenterX=$objectCenterX frameCenterX=320.0 relativePosition=${threat.horizontalZone} guidanceAction=${decision.action}")
        }

        Log.i("SARTHI_HYSTERESIS", "previousAction=$lastAction newAction=${decision.action} scoreDifference=$lastScoreDiff")
        lastAction = decision.action

        Log.i("SARTHI_DEBUG", """
            [GUIDANCE_DECISION]
            time=${System.currentTimeMillis()}
            frameId=$frameId
            trackerId=${decision.highestThreatId ?: "null"}
            action=${decision.action}
            distance=${decision.highestThreatDistance ?: "null"}
            threatLevel=${decision.highestThreatLevel ?: "null"}
        """.trimIndent())
        return result
    }

    private fun processInternal(
        threats: List<ThreatPriority>,
        corridors: List<SafeCorridor>,
        isSpeaking: Boolean = false
    ): Pair<GuidanceDecision, GuidanceMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        val metadata = GuidanceMetadata(
            evaluatedThreats = threats.size,
            evaluatedCorridors = corridors.size,
            decisionTimeMs = 0L,
            successful = false
        )

        if (isSpeaking) {
            val duration = currentTime - startTime
            lastScoreDiff = 0f
            val highestThreat = threats.maxByOrNull { it.priorityScore }
            return Pair(
                GuidanceDecision(
                    action = GuidanceAction.STOP,
                    reason = "Speech in progress",
                    selectedCorridor = null,
                    highestThreatId = highestThreat?.trackId,
                    highestThreatClassName = highestThreat?.className,
                    highestThreatLevel = highestThreat?.threatLevel ?: ThreatLevel.LOW,
                    confidence = 1.0f,
                    highestThreatDistance = highestThreat?.distanceMeters
                ),
                metadata.copy(decisionTimeMs = duration, successful = true)
            )
        }

        // --- Emergency Stop Safety Filter ---
        val stopThreats = threats.filter {
            it.threatLevel == ThreatLevel.CRITICAL && 
            it.horizontalZone == HorizontalZone.CENTER
        }
        val centerObstacle = stopThreats.minByOrNull { it.distanceMeters }
        val highestThreat = threats.maxByOrNull { it.priorityScore }

        if (centerObstacle != null) {
            val duration = currentTime - startTime
            lastScoreDiff = 0f

            val otherSafeL = corridors.find { it.horizontalZone == HorizontalZone.LEFT }?.score ?: 0f
            val otherSafeR = corridors.find { it.horizontalZone == HorizontalZone.RIGHT }?.score ?: 0f
            
            val bestSecondaryAction = when {
                otherSafeL >= 70f && otherSafeR >= 70f -> GuidanceAction.MOVE_SLIGHTLY_LEFT
                otherSafeL >= 70f -> GuidanceAction.MOVE_SLIGHTLY_LEFT
                otherSafeR >= 70f -> GuidanceAction.MOVE_SLIGHTLY_RIGHT
                else -> null
            }

            val secondaryReason = when (bestSecondaryAction) {
                GuidanceAction.MOVE_SLIGHTLY_LEFT -> "Turn slight left"
                GuidanceAction.MOVE_SLIGHTLY_RIGHT -> "Turn slight right"
                else -> null
            }

            return Pair(
                GuidanceDecision(
                    action = GuidanceAction.STOP,
                    reason = centerObstacle.className + " detected ahead",
                    selectedCorridor = null,
                    highestThreatId = centerObstacle.trackId,
                    highestThreatClassName = centerObstacle.className,
                    highestThreatLevel = centerObstacle.threatLevel,
                    confidence = 1.0f,
                    highestThreatDistance = centerObstacle.distanceMeters,
                    secondaryAction = bestSecondaryAction,
                    secondaryReason = secondaryReason
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

        lastScoreDiff = kotlin.math.abs(biasedLeftScore - biasedRightScore)

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

            // Sharp Left vs Sharp Right (if standard paths are blocked/not safe)
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
        return Pair(decision, metadata.copy(decisionTimeMs = duration, successful = true))
    }
}
