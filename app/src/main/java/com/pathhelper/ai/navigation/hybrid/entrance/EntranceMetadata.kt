/**
* Coordinates Entrance Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Entrance Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.hybrid.entrance
/**
* Represents the data structures or state of Entrance Metadata.
*/
data
class EntranceMetadata(
    val processingTimeMs: Long,
    val candidateCount: Int,
    val bestConfidenceScore: Float,
    val successful: Boolean,
    val errorMessage: String? = null
)
