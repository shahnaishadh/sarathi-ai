/**
* Coordinates Relative Position Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Relative Position Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Relative Position Metadata.
*/
data
class RelativePositionMetadata(
    val trackingObjects: Int,
    val processedObjects: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
