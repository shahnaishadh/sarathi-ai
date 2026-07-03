/**
* Coordinates Persistent Map operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Persistent Map.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.persistence
/**
* Represents the data structures or state of Persistent Map.
*/
data
class PersistentMap(
    val mapId: String,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>,
    val createdTimestamp: Long,
    val lastModifiedTimestamp: Long,
    val version: Int
)
