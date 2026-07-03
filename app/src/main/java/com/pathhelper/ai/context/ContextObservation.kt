package com.pathhelper.ai.context

import com.pathhelper.ai.world.LandmarkType
/**
* Represents the data structures or state of Context Observation.
*/
data
class ContextObservation(
    val landmarkId: String,
    val landmarkType: LandmarkType,
    val priority: ContextPriority,
    val distanceMeters: Float?,
    val description: String
)
