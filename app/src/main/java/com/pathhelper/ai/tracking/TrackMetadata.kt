/**
* Coordinates Track Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Track Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.tracking
/**
* Represents the data structures or state of Track Metadata.
*/
data
class TrackMetadata(
    val activeTracks: Int,
    val newTracksCreated: Int,
    val removedTracks: Int,
    val processingTimeMs: Long,
    val trackingSuccessful: Boolean,
    val errorMessage: String? = null
)
