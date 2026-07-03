/**
* Coordinates Detection operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Detection.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.onnx
/**
* Represents the data structures or state of Detection.
*/
data
class Detection(
    val classId: Int,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
)
