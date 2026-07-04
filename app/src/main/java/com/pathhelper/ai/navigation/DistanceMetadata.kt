/**
* Coordinates Distance Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Distance Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Distance Metadata.
*/
data
class DistanceMetadata(
    val processedTracks: Int,
    val estimatedTracks: Int,
    val averageDistanceMeters: Float,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
