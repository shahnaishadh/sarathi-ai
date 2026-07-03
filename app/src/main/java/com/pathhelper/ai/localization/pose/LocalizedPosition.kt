/**
* Coordinates Localized Position operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Localized Position.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.pose
/**
* Represents the data structures or state of Localized Position.
*/
data
class LocalizedPosition(
    val currentRoom: String?,
    val nearestLandmark: String?,
    val landmarkDistance: Float,
    val landmarkBearing: Float,
    val confidence: Float,
    val state: LocalizationState,
    val timestamp: Long
)
