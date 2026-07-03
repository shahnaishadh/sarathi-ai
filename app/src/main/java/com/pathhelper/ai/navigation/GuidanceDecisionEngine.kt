package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
/**
* Coordinates Guidance Decision Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Guidance Decision Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class GuidanceDecisionEngine {

    private var stopUntilTimestamp = 0L

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

        // --- Local Navigation Logic ---
        val leftCor = corridors.find { it.horizontalZone == HorizontalZone.LEFT }
        val centerCor = corridors.find { it.horizontalZone == HorizontalZone.CENTER }
        val rightCor = corridors.find { it.horizontalZone == HorizontalZone.RIGHT }

        val leftScore = leftCor?.score ?: 0f
        val centerScore = centerCor?.score ?: 0f
        val rightScore = rightCor?.score ?: 0f

        val leftState = leftCor?.state ?: CorridorState.BLOCKED
        val centerState = centerCor?.state ?: CorridorState.BLOCKED
        val rightState = rightCor?.state ?: CorridorState.BLOCKED

        Log.d("GUIDANCE_DECISION", 
            "leftState=$leftState centerState=$centerState rightState=$rightState " +
            "leftScore=$leftScore centerScore=$centerScore rightScore=$rightScore")

        // Rule 1: Center is SAFE -> Keep going
        if (centerCor != null && centerCor.state == CorridorState.SAFE) {
            return generateDecision(GuidanceAction.KEEP_CENTER, "Path clear", HorizontalZone.CENTER, highestThreat, startTime, metadata)
        }

        // Center is caution, but if it is still the best option compared to sides, keep center
        if (centerScore >= leftScore && centerScore >= rightScore && centerScore >= 30f) {
            return generateDecision(GuidanceAction.KEEP_CENTER, "Proceed with caution", HorizontalZone.CENTER, highestThreat, startTime, metadata)
        }

        // Rule 2: Center is blocked or cautioned -> Find safest escape path
        return when {
            leftScore >= 70f && leftScore >= rightScore -> {
                val secondaryAction = if (centerScore >= 40f) GuidanceAction.KEEP_CENTER else null
                generateDecision(GuidanceAction.MOVE_LEFT, "Avoid obstacle via left", HorizontalZone.LEFT, highestThreat, startTime, metadata, secondaryAction, "then go straight")
            }
            
            rightScore >= 70f -> {
                val secondaryAction = if (centerScore >= 40f) GuidanceAction.KEEP_CENTER else null
                generateDecision(GuidanceAction.MOVE_RIGHT, "Avoid obstacle via right", HorizontalZone.RIGHT, highestThreat, startTime, metadata, secondaryAction, "then go straight")
            }

            leftScore >= 30f && leftScore >= rightScore -> {
                val secondaryAction = if (centerScore >= 40f) GuidanceAction.KEEP_CENTER else null
                generateDecision(GuidanceAction.MOVE_SLIGHTLY_LEFT, "Turn slightly left", HorizontalZone.LEFT, highestThreat, startTime, metadata, secondaryAction, "then go straight")
            }

            rightScore >= 30f -> {
                val secondaryAction = if (centerScore >= 40f) GuidanceAction.KEEP_CENTER else null
                generateDecision(GuidanceAction.MOVE_SLIGHTLY_RIGHT, "Turn slightly right", HorizontalZone.RIGHT, highestThreat, startTime, metadata, secondaryAction, "then go straight")
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
