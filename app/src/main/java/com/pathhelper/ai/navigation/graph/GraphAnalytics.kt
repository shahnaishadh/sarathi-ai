/**
* Coordinates Graph Analytics operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Graph Analytics.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.graph
/**
* Represents the data structures or state of Graph Analytics.
*/
data
class GraphAnalytics(
    val nodeCount: Int,
    val edgeCount: Int,
    val activePath: List<String>,
    val currentNodeId: String?,
    val destinationNodeId: String?,
    val planningTimeMs: Long
)
