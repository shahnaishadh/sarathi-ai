/**
* Coordinates Landmark Navigation Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Landmark Navigation Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.landmark
/**
* Represents the data structures or state of Landmark Navigation Metadata.
*/
data
class LandmarkNavigationMetadata(
    val destinationFound: Boolean,
    val generatedSteps: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
