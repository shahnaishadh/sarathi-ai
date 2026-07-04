/**
* Coordinates Navigation Context Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Context Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.context
/**
* Represents the data structures or state of Navigation Context Metadata.
*/
data
class NavigationContextMetadata(
    val processedLandmarks: Int,
    val prioritizedLandmarks: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
