/**
* Coordinates Building Target operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Building Target.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.common.target
/**
* Represents the data structures or state of Building Target.
*/
data
class BuildingTarget(
    val buildingId: String,
    val entranceLatitude: Double = 37.7749,
    val entranceLongitude: Double = -122.4194
) : NavigationTarget
