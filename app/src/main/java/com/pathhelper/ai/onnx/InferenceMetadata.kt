/**
* Coordinates Inference Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Inference Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Inference Metadata.
*/
data
class InferenceMetadata(
    val inferenceSuccessful: Boolean,
    val outputName: String,
    val outputShape: LongArray,
    val inferenceTimeMs: Long,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InferenceMetadata

        if (inferenceSuccessful != other.inferenceSuccessful) return false
        if (outputName != other.outputName) return false
        if (!outputShape.contentEquals(other.outputShape)) return false
        if (inferenceTimeMs != other.inferenceTimeMs) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inferenceSuccessful.hashCode()
        result = 31 * result + outputName.hashCode()
        result = 31 * result + outputShape.contentHashCode()
        result = 31 * result + inferenceTimeMs.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
