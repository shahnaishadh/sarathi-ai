package com.pathhelper.ai.navigation.outdoor.routing

import android.location.Location
/**
* Represents the data structures or state of Route Request.
*/
data
class RouteRequest(
    val start: Location,
    val destination: Location,
    val mode: String = "walking"
)
