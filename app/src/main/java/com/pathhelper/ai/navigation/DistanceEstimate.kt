/**
* Coordinates Distance Estimate operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Distance Estimate.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Distance Estimate.
*/
data
class DistanceEstimate(
    val trackId: Int,
    val classId: Int,
    val distanceMeters: Float,
    val horizontalZone: HorizontalZone,
    val verticalZone: VerticalZone
)
