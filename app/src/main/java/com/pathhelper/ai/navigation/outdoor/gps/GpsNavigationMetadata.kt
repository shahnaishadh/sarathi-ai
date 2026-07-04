/**
* Coordinates Gps Navigation Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Gps Navigation Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.outdoor.gps
/**
* Represents the data structures or state of Gps Navigation Metadata.
*/
data
class GpsNavigationMetadata(
    val hasLocation: Boolean,
    val accuracy: Float?,
    val satellitesCount: Int,
    val recalculationsCount: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
