package com.pathhelper.ai.navigation.persistence

import com.pathhelper.ai.navigation.graph.GraphRelationship
/**
* Represents the data structures or state of Map Edge.
*/
data
class MapEdge(
    val sourceId: String,
    val destinationId: String,
    val relationship: GraphRelationship,
    val weight: Float
)
