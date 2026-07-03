/**
* Coordinates Voice Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Voice Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.voice
/**
* Represents the data structures or state of Voice Metadata.
*/
data
class VoiceMetadata(
    val generatedCommands: Int,
    val suppressedCommands: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
