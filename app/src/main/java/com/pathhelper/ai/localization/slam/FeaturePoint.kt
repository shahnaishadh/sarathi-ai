/**
* Coordinates Feature Point operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Feature Point.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Feature Point.
*/
data
class FeaturePoint(
    val x: Float,
    val y: Float,
    val response: Float,
    val octave: Int
)
