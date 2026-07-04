package com.pathhelper.ai.localization.slam

import kotlin.math.sqrt
/**
* Coordinates Visual Odometry Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Visual Odometry Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class VisualOdometryEngine {
    fun matchAndEstimate(
        prevFrame: SlamFrame?,
        currFrame: SlamFrame
    ): Pair<Int, Pair<Float, Float>> {
        if (prevFrame == null) return Pair(0, Pair(0f, 0f))

        var matchedCount = 0
        var sumDx = 0f
        var sumDy = 0f

        for (currFeat in currFrame.features) {
            var bestMatch: VisualFeature? = null
            var minDistance = Float.MAX_VALUE

            for (prevFeat in prevFrame.features) {
                val dist = euclideanDistance(currFeat.descriptor, prevFeat.descriptor)
                if (dist < minDistance) {
                    minDistance = dist
                    bestMatch = prevFeat
                }
            }

            if (bestMatch != null && minDistance < 0.2f) {
                matchedCount++
                sumDx += (currFeat.point.x - bestMatch.point.x)
                sumDy += (currFeat.point.y - bestMatch.point.y)
            }
        }

        val dx = if (matchedCount > 0) sumDx / matchedCount else 0f
        val dy = if (matchedCount > 0) sumDy / matchedCount else 0f
        return Pair(matchedCount, Pair(dx, dy))
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        val size = minOf(a.size, b.size)
        for (i in 0 until size) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}
