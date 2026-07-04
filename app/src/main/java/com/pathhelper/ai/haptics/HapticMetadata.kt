/**
* Coordinates Haptic Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Haptic Metadata.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.haptics
/**
* Represents the data structures or state of Haptic Metadata.
*/
data
class HapticMetadata(
    val generatedCommands: Int,
    val suppressedCommands: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
