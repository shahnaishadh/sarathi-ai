/**
* Coordinates Route Metadata operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Metadata.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.route
/**
* Represents the data structures or state of Route Metadata.
*/
data
class RouteMetadata(
    val trackedLandmarks: Int,
    val passedLandmarks: Int,
    val revisitedLandmarks: Int,
    val processingTimeMs: Long,
    val successful: Boolean,
    val errorMessage: String? = null
)
