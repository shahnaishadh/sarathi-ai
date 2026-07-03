package com.pathhelper.ai.navigation.common.analytics

import android.location.Location
/**
* Coordinates Gps Analytics operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Gps Analytics.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class GpsAnalytics {
    var routeDeviationEvents = 0
        private set
    var recalculationsTriggered = 0
        private set
    var waypointCompletes = 0
        private set
    private var totalDistanceTraveled = 0.0f
    private var lastLocation: Location? = null

    fun logLocationUpdate(location: Location) {
        lastLocation?.let { last ->
            totalDistanceTraveled += last.distanceTo(location)
        }
        lastLocation = location
    }

    fun logRouteDeviation() {
        routeDeviationEvents++
    }

    fun logRecalculation() {
        recalculationsTriggered++
    }

    fun logWaypointComplete() {
        waypointCompletes++
    }

    fun getTotalDistanceTraveled(): Float {
        return totalDistanceTraveled
    }

    fun reset() {
        routeDeviationEvents = 0
        recalculationsTriggered = 0
        waypointCompletes = 0
        totalDistanceTraveled = 0.0f
        lastLocation = null
    }
}
