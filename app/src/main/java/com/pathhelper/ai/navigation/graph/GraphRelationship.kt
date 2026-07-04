/**
* Coordinates Graph Relationship operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Graph Relationship.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.navigation.graph
/**
* Represents the different states or configurations of Graph Relationship.
*/
enum
class GraphRelationship {
    CONNECTED_TO,
    AHEAD_OF,
    LEFT_OF,
    RIGHT_OF,
    BEFORE,
    AFTER
}
