/**
* Coordinates Safe Corridor operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Safe Corridor.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Safe Corridor.
*/
data
class SafeCorridor(
    val horizontalZone: HorizontalZone,
    val state: CorridorState,
    val threatCount: Int,
    val highestThreatLevel: ThreatLevel,
    val averageDistanceMeters: Float,
    val score: Float
)
