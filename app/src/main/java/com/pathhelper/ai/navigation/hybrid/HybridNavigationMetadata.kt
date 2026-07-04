/**
* Coordinates Hybrid Navigation Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Hybrid Navigation Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.hybrid
/**
* Represents the data structures or state of Hybrid Navigation Metadata.
*/
data
class HybridNavigationMetadata(
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
