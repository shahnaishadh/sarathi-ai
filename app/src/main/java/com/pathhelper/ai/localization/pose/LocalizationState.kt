/**
* Coordinates Localization State operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Localization State.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.pose
/**
* Represents the different states or configurations of Localization State.
*/
enum
class LocalizationState {
    INITIALIZING,
    SEARCHING,
    LOCALIZED,
    LOST
}
