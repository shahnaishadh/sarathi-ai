/**
* Coordinates Route Response operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Response.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.outdoor.routing
/**
* Represents the data structures or state of Route Response.
*/
data
class RouteResponse(
    val waypoints: List<RouteWaypoint>,
    val instructions: List<RouteInstruction>,
    val totalDistanceMeters: Float,
    val totalDurationSeconds: Float,
    val status: String
)
