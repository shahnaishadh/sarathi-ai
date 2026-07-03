/**
* Coordinates Threat Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Threat Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Threat Metadata.
*/
data
class ThreatMetadata(
    val processedTracks: Int,
    val rankedThreats: Int,
    val criticalThreats: Int,
    val highestPriorityScore: Float,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
