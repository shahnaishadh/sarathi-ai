/**
* Coordinates World Model operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for World Model.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.world
/**
* Represents the data structures or state of World Model.
*/
data
class WorldModel(
    val landmarks: List<Landmark>,
    val relations: List<WorldRelationship>
)
