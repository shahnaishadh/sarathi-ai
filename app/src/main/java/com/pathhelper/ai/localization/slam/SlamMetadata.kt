/**
* Coordinates Slam Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Slam Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Slam Metadata.
*/
data
class SlamMetadata(
    val featureCount: Int,
    val matchedFeatures: Int,
    val trackingConfidence: Float,
    val poseConfidence: Float,
    val localMapPoints: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
