/**
* Coordinates Guidance Action operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Guidance Action.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation
/**
* Represents the different states or configurations of Guidance Action.
*/
enum
class GuidanceAction {
    KEEP_CENTER,
    MOVE_SLIGHTLY_LEFT,
    MOVE_LEFT,
    MOVE_SLIGHTLY_RIGHT,
    MOVE_RIGHT,
    WAIT,
    STOP
}
