/**
* Coordinates Entrance Candidate operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Entrance Candidate.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.hybrid.entrance
/**
* Represents the data structures or state of Entrance Candidate.
*/
data
class EntranceCandidate(
    val trackId: Int,
    val distanceEstimateMeters: Float,
    val detectionConfidence: Float,
    val aspectRatio: Float,
    val positionScore: Float,
    val gpsProximityScore: Float
)
