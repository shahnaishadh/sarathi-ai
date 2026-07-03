/**
* Coordinates Tensor Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Tensor Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Tensor Metadata.
*/
data
class TensorMetadata(
    val tensorCreated: Boolean,
    val shape: LongArray,
    val preprocessingTimeMs: Long,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TensorMetadata

        if (tensorCreated != other.tensorCreated) return false
        if (!shape.contentEquals(other.shape)) return false
        if (preprocessingTimeMs != other.preprocessingTimeMs) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tensorCreated.hashCode()
        result = 31 * result + shape.contentHashCode()
        result = 31 * result + preprocessingTimeMs.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
