/**
* Coordinates Memory Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Memory Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.memory
/**
* Represents the data structures or state of Memory Metadata.
*/
data
class MemoryMetadata(
    val activeObservations: Int,
    val newObservations: Int,
    val updatedObservations: Int,
    val expiredObservations: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
