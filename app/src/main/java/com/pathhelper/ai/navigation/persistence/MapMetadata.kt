/**
* Coordinates Map Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Map Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.persistence
/**
* Represents the data structures or state of Map Metadata.
*/
data
class MapMetadata(
    val mapId: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val version: Int,
    val saveSuccessful: Boolean,
    val processingTimeMs: Long,
    val errorMessage: String? = null
)
