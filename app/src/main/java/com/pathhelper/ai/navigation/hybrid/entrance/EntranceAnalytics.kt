package com.pathhelper.ai.navigation.hybrid.entrance
/**
* Coordinates Entrance Analytics operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Entrance Analytics.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class EntranceAnalytics {
    var totalCandidatesEvaluated = 0
    var highestConfidenceObserved = 0.0f
    var confirmedEntrancesCount = 0
    var rejectedEntrancesCount = 0

    fun logEvaluation(candidatesCount: Int, bestConfidence: Float, decision: EntranceDecision) {
        totalCandidatesEvaluated += candidatesCount
        if (bestConfidence > highestConfidenceObserved) {
            highestConfidenceObserved = bestConfidence
        }
        when (decision) {
            EntranceDecision.ENTRANCE_CONFIRMED -> confirmedEntrancesCount++
            EntranceDecision.ENTRANCE_REJECTED -> rejectedEntrancesCount++
            EntranceDecision.MORE_EVIDENCE_REQUIRED -> {}
        }
    }
}
