/**
* Coordinates Scene Memory operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Scene Memory.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.memory
/**
* Represents the data structures or state of Scene Memory.
*/
data
class SceneMemory(
    val observations: List<MemoryObservation>
)
