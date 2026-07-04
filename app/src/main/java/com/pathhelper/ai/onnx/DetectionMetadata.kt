/**
* Coordinates Detection Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Detection Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Detection Metadata.
*/
data
class DetectionMetadata(
    val parsingSuccessful: Boolean,
    val totalCandidates: Int,
    val maxConfidence: Float,
    val averageConfidence: Float,
    val parsingTimeMs: Long,
    val tensorShape: LongArray,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectionMetadata

        if (parsingSuccessful != other.parsingSuccessful) return false
        if (totalCandidates != other.totalCandidates) return false
        if (maxConfidence != other.maxConfidence) return false
        if (averageConfidence != other.averageConfidence) return false
        if (parsingTimeMs != other.parsingTimeMs) return false
        if (!tensorShape.contentEquals(other.tensorShape)) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parsingSuccessful.hashCode()
        result = 31 * result + totalCandidates
        result = 31 * result + maxConfidence.hashCode()
        result = 31 * result + averageConfidence.hashCode()
        result = 31 * result + parsingTimeMs.hashCode()
        result = 31 * result + tensorShape.contentHashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
