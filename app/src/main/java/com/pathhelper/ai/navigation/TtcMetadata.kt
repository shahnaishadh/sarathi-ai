/**
* Coordinates Ttc Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Ttc Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Ttc Metadata.
*/
data
class TtcMetadata(
    val processedTracks: Int,
    val validTtcCount: Int,
    val criticalRiskCount: Int,
    val averageTtcSeconds: Float,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
