package com.pathhelper.ai.localization.slam

import kotlin.math.atan2
/**
* Coordinates Pose Estimator operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Pose Estimator.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class PoseEstimator {
    private var currentX = 0f
    private var currentY = 0f
    private var currentHeading = 0f

    fun reset() {
        currentX = 0f
        currentY = 0f
        currentHeading = 0f
    }

    fun updatePose(
        deltaX: Float,
        deltaY: Float,
        matchedRatio: Float,
        timestamp: Long
    ): PoseEstimate {
        val scale = 0.01f
        currentX += deltaX * scale
        currentY += deltaY * scale

        if (deltaX != 0f || deltaY != 0f) {
            currentHeading = (atan2(deltaY.toDouble(), deltaX.toDouble()) * 180.0 / Math.PI).toFloat()
        }

        val confidence = matchedRatio.coerceIn(0.0f, 1.0f)
        return PoseEstimate(
            positionX = currentX,
            positionY = currentY,
            headingDegrees = currentHeading,
            confidence = confidence,
            timestamp = timestamp
        )
    }
}
