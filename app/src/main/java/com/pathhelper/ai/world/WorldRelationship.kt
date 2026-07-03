/**
* Coordinates World Relationship operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for World Relationship.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.world
/**
* Represents the data structures or state of World Relationship.
*/
data
class WorldRelationship(
    val sourceId: String,
    val targetId: String,
    val relation: LandmarkRelation
)
