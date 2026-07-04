/**
* Coordinates Relative Position operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Relative Position.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the data structures or state of Relative Position.
*/
data
class RelativePosition(
    val trackId: Int,
    val classId: Int,

    val horizontalZone: HorizontalZone,
    val verticalZone: VerticalZone,

    val centerX: Float,
    val centerY: Float
)
