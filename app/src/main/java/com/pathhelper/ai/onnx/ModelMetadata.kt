/**
* Coordinates Model Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Model Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Model Metadata.
*/
data
class ModelMetadata(
    val isLoaded: Boolean,
    val inputName: String,
    val inputShape: LongArray,
    val outputName: String,
    val outputShape: LongArray,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelMetadata

        if (isLoaded != other.isLoaded) return false
        if (inputName != other.inputName) return false
        if (!inputShape.contentEquals(other.inputShape)) return false
        if (outputName != other.outputName) return false
        if (!outputShape.contentEquals(other.outputShape)) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoaded.hashCode()
        result = 31 * result + inputName.hashCode()
        result = 31 * result + inputShape.contentHashCode()
        result = 31 * result + outputName.hashCode()
        result = 31 * result + outputShape.contentHashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
