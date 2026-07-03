/**
* Coordinates Entrance Confidence operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Entrance Confidence.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.hybrid.entrance
/**
* Represents the data structures or state of Entrance Confidence.
*/
data
class EntranceConfidence(
    val score: Float, // Aggregated confidence between 0.0 and 1.0
    val doorConfidence: Float,
    val gpsProximityScore: Float,
    val sceneMemoryEvidence: Float,
    val worldModelEvidence: Float,
    val historicalSuccessScore: Float
)
