package com.pathhelper.ai.navigation.persistence

import com.pathhelper.ai.world.LandmarkType
/**
* Represents the data structures or state of Map Node.
*/
data
class MapNode(
    val nodeId: String,
    val landmarkType: LandmarkType,
    val confidence: Float
)
