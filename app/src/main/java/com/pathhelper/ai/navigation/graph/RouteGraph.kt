/**
* Coordinates Route Graph operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Graph.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.graph
/**
* Represents the data structures or state of Route Graph.
*/
data
class RouteGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val timestamp: Long
)
