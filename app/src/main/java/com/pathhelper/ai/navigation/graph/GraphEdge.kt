/**
* Coordinates Graph Edge operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Graph Edge.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.graph
/**
* Represents the data structures or state of Graph Edge.
*/
data
class GraphEdge(
    val source: GraphNode,
    val destination: GraphNode,
    val relationship: GraphRelationship,
    val weight: Float
)
