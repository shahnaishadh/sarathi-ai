/**
* Coordinates Navigation Context operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Context.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.context
/**
* Represents the data structures or state of Navigation Context.
*/
data
class NavigationContext(
    val primaryLandmark: ContextObservation?,
    val secondaryLandmark: ContextObservation?,
    val approachingLandmark: ContextObservation?,
    val activeContext: String,
    val observations: List<ContextObservation>
)
