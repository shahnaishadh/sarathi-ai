/**
* Coordinates Navigation Progress operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Progress.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.common.target
/**
* Represents the different states or configurations of Navigation Progress.
*/
enum
class NavigationProgress {
    SEARCHING,
    ROUTE_FOUND,
    APPROACHING,
    ARRIVED,
    LOST
}
