/**
* Coordinates Hybrid Route operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Hybrid Route.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.hybrid
/**
* Represents the data structures or state of Hybrid Route.
*/
data
class HybridRoute(
    val steps: List<HybridRouteStep>,
    val totalDistanceMeters: Float,
    val etaSeconds: Long
)
