/**
* Coordinates Nms Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Nms Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Nms Metadata.
*/
data
class NmsMetadata(
    val filteringSuccessful: Boolean,
    val candidateCount: Int,
    val confidenceFilteredCount: Int,
    val finalDetectionCount: Int,
    val maxConfidence: Float,
    val nmsTimeMs: Long,
    val errorMessage: String? = null
)
