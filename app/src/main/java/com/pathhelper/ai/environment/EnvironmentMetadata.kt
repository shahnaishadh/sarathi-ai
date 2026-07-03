/**
* Coordinates Environment Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Environment Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.environment
/**
* Represents the data structures or state of Environment Metadata.
*/
data
class EnvironmentMetadata(
    val observations: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
