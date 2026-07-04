/**
* Coordinates Slam Map Point operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Slam Map Point.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Slam Map Point.
*/
data
class SlamMapPoint(
    val id: Long,
    val x: Float,
    val y: Float,
    val descriptor: FloatArray,
    var observedCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SlamMapPoint
        if (id != other.id) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (!descriptor.contentEquals(other.descriptor)) return false
        if (observedCount != other.observedCount) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + descriptor.contentHashCode()
        result = 31 * result + observedCount
        return result
    }
}
