package com.pathhelper.ai.navigation.hybrid

import android.os.SystemClock
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceDecision
/**
* Coordinates Hybrid Navigation Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Hybrid Navigation Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class HybridNavigationEngine {
    private var currentMode = NavigationMode.OUTDOOR
    private var transitionStatus = "PENDING"

    fun reset() {
        currentMode = NavigationMode.OUTDOOR
        transitionStatus = "PENDING"
    }

    fun getCurrentMode(): NavigationMode = currentMode
    fun getTransitionStatus(): String = transitionStatus

    fun process(
        target: NavigationTarget,
        entranceDecision: EntranceDecision,
        indoorReached: Boolean
    ): Pair<HybridNavigationState, HybridNavigationMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        if (indoorReached) {
            currentMode = NavigationMode.INDOOR
            transitionStatus = "COMPLETED"
        } else {
            if (currentMode == NavigationMode.OUTDOOR) {
                if (entranceDecision == EntranceDecision.ENTRANCE_CONFIRMED) {
                    currentMode = NavigationMode.INDOOR
                    transitionStatus = "COMPLETED"
                } else if (entranceDecision == EntranceDecision.MORE_EVIDENCE_REQUIRED) {
                    currentMode = NavigationMode.ENTRANCE_APPROACH
                    transitionStatus = "APPROACHING"
                }
            } else if (currentMode == NavigationMode.ENTRANCE_APPROACH) {
                if (entranceDecision == EntranceDecision.ENTRANCE_CONFIRMED) {
                    currentMode = NavigationMode.INDOOR
                    transitionStatus = "COMPLETED"
                } else if (entranceDecision == EntranceDecision.ENTRANCE_REJECTED) {
                    currentMode = NavigationMode.OUTDOOR
                    transitionStatus = "PENDING"
                }
            }
        }

        val instruction = when (currentMode) {
            NavigationMode.OUTDOOR -> "Follow outdoor path to building entrance."
            NavigationMode.ENTRANCE_APPROACH -> "Entrance is near. Locate the entrance door."
            NavigationMode.INDOOR -> "Entrance reached. Transitioning to indoor navigation."
            NavigationMode.ARRIVED -> "Arrived at destination."
        }

        val state = HybridNavigationState(
            currentMode = currentMode,
            activeTarget = target,
            currentInstruction = instruction,
            outdoorProgress = if (currentMode == NavigationMode.OUTDOOR) 0.5f else 1.0f,
            indoorProgress = if (currentMode == NavigationMode.INDOOR) 0.3f else 0.0f,
            transitionStatus = transitionStatus,
            etaSeconds = 300L,
            completionPercentage = if (currentMode == NavigationMode.INDOOR) 80f else 40f
        )

        val meta = HybridNavigationMetadata(
            processingTimeMs = SystemClock.elapsedRealtime() - startTime,
            successful = true
        )

        return Pair(state, meta)
    }
}
