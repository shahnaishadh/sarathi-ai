/**
* Coordinates Pose Estimate operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Pose Estimate.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.slam
/**
* Represents the data structures or state of Pose Estimate.
*/
data
class PoseEstimate(
    val positionX: Float,
    val positionY: Float,
    val headingDegrees: Float,
    val confidence: Float,
    val timestamp: Long
)
