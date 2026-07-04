/**
* Coordinates Navigation Step operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Step.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.common.target
/**
* Represents the data structures or state of Navigation Step.
*/
data
class NavigationStep(
    val instruction: String,
    val confidence: Float,
    val timestamp: Long
)
