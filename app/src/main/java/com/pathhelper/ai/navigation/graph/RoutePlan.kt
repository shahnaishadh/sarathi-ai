package com.pathhelper.ai.navigation.graph

import com.pathhelper.ai.navigation.common.target.NavigationTarget
/**
* Represents the data structures or state of Route Plan.
*/
data
class RoutePlan(
    val destination: NavigationTarget,
    val currentNode: GraphNode?,
    val plannedPath: List<GraphNode>,
    val instructions: List<String>,
    val estimatedDistanceMeters: Float?
)
