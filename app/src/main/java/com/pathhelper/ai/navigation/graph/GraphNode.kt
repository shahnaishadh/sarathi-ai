package com.pathhelper.ai.navigation.graph

import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.navigation.HorizontalZone
/**
* Represents the data structures or state of Graph Node.
*/
data
class GraphNode(
    val nodeId: String,
    val landmarkType: LandmarkType,
    val position: HorizontalZone,
    val confidence: Float
)
