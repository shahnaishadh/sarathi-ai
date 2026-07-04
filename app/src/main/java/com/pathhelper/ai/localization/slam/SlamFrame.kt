/**
* Coordinates Slam Frame operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Slam Frame.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Slam Frame.
*/
data
class SlamFrame(
    val id: Long,
    val timestamp: Long,
    val features: List<VisualFeature>
)
