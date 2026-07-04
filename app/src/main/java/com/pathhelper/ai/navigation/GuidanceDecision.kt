/**
* Coordinates Guidance Decision operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Guidance Decision.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Guidance Decision.
*/
data
class GuidanceDecision(
    val action: GuidanceAction,
    val reason: String,
    val selectedCorridor: HorizontalZone?,
    val highestThreatId: Int?,
    val highestThreatClassName: String?,
    val highestThreatLevel: ThreatLevel?,
    val confidence: Float,
    val secondaryAction: GuidanceAction? = null,
    val secondaryReason: String? = null,
    val highestThreatDistance: Float? = null
)
