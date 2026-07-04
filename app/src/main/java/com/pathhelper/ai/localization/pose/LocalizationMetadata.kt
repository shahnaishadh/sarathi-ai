/**
* Coordinates Localization Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Localization Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.pose
/**
* Represents the data structures or state of Localization Metadata.
*/
data
class LocalizationMetadata(
    val candidateCount: Int,
    val matchedLandmarks: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
