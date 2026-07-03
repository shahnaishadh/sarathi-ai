/**
* Coordinates Route Graph Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Graph Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.graph
/**
* Represents the data structures or state of Route Graph Metadata.
*/
data
class RouteGraphMetadata(
    val nodeCount: Int,
    val edgeCount: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
