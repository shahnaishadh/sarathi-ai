package com.pathhelper.ai.route

import com.pathhelper.ai.world.LandmarkType
/**
* Represents the data structures or state of Route Landmark.
*/
data
class RouteLandmark(
    val landmarkId: String,
    val landmarkType: LandmarkType,
    val firstSeenTimestamp: Long,
    var lastSeenTimestamp: Long,
    var visitCount: Int,
    var event: RouteEvent
)
