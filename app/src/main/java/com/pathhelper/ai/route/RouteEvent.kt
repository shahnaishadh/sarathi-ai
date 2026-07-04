/**
* Coordinates Route Event operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Event.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.route
/**
* Represents the different states or configurations of Route Event.
*/
enum
class RouteEvent {
    DISCOVERED,
    APPROACHING,
    REACHED,
    PASSED,
    REVISITED
}
