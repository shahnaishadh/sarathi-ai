/**
* Coordinates Guidance Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Guidance Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Guidance Metadata.
*/
data
class GuidanceMetadata(
    val evaluatedThreats: Int,
    val evaluatedCorridors: Int,
    val decisionTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
