/**
* Coordinates Visual Feature operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Visual Feature.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Visual Feature.
*/
data
class VisualFeature(
    val point: FeaturePoint,
    val descriptor: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualFeature
        if (point != other.point) return false
        if (!descriptor.contentEquals(other.descriptor)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = point.hashCode()
        result = 31 * result + descriptor.contentHashCode()
        return result
    }
}
