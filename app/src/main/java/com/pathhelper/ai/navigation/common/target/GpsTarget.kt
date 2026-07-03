/**
* Coordinates Gps Target operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Gps Target.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.common.target
/**
* Represents the data structures or state of Gps Target.
*/
data
class GpsTarget(
    val latitude: Double,
    val longitude: Double
) : NavigationTarget
