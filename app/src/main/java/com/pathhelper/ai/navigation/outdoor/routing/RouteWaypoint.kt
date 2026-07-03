package com.pathhelper.ai.navigation.outdoor.routing

import android.location.Location
/**
* Represents the data structures or state of Route Waypoint.
*/
data
class RouteWaypoint(
    val location: Location,
    val name: String,
    val instruction: String,
    val targetDistanceMeters: Float = 5.0f
)
