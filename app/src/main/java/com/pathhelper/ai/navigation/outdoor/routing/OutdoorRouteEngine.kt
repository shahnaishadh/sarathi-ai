package com.pathhelper.ai.navigation.outdoor.routing

import android.location.Location
/**
* Represents the data structures or state of Route Engine State.
*/
data
class RouteEngineState(
    val waypoints: List<RouteWaypoint>,
    val currentWaypointIndex: Int,
    val isCalculated: Boolean,
    val deviationDetected: Boolean,
    val totalDistanceMeters: Float,
    val totalDurationSeconds: Float
)
/**
* Coordinates Outdoor Route Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Outdoor Route Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class OutdoorRouteEngine(
    private val routeProvider: OpenStreetMapRouteProvider
) {
    private var waypoints = mutableListOf<RouteWaypoint>()
    private var instructions = mutableListOf<RouteInstruction>()
    private var currentWaypointIndex = 0
    private var isCalculating = false
    private var deviationDetected = false
    private var totalDistanceMeters = 0.0f
    private var totalDurationSeconds = 0.0f
    private var activeDestination: Location? = null

    private var lastDeviationTriggerTime = 0L
    private val DEVIATION_RETRY_INTERVAL_MS = 5000L

    fun getActiveWaypoint(): RouteWaypoint? {
        return waypoints.getOrNull(currentWaypointIndex)
    }

    fun getInstructions(): List<RouteInstruction> {
        return instructions
    }

    fun isRouteEmpty(): Boolean {
        return waypoints.isEmpty()
    }

    fun getRecalculationsCount(): Int {
        return if (lastDeviationTriggerTime > 0) 1 else 0
    }

    fun updateRouteProgress(userLocation: Location): RouteEngineState {
        if (waypoints.isEmpty()) {
            return RouteEngineState(emptyList(), 0, false, false, 0f, 0f)
        }

        val activeWaypoint = getActiveWaypoint()
        if (activeWaypoint != null) {
            val distToWaypoint = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                activeWaypoint.location.latitude, activeWaypoint.location.longitude
            )

            if (distToWaypoint < activeWaypoint.targetDistanceMeters) {
                if (currentWaypointIndex < waypoints.size - 1) {
                    currentWaypointIndex++
                    deviationDetected = false
                }
            }
        }

        val dev = checkDeviation(userLocation)
        if (dev && !deviationDetected) {
            deviationDetected = true
            triggerReRoute(userLocation)
        } else if (!dev) {
            deviationDetected = false
        }

        return RouteEngineState(
            waypoints = waypoints,
            currentWaypointIndex = currentWaypointIndex,
            isCalculated = waypoints.isNotEmpty(),
            deviationDetected = deviationDetected,
            totalDistanceMeters = totalDistanceMeters,
            totalDurationSeconds = totalDurationSeconds
        )
    }

    fun requestRoute(start: Location, destination: Location, onCompleted: (RouteEngineState) -> Unit) {
        if (isCalculating) return
        isCalculating = true
        activeDestination = destination

        routeProvider.calculateDetailedRoute(start, destination) { response ->
            waypoints.clear()
            waypoints.addAll(response.waypoints)
            instructions.clear()
            instructions.addAll(response.instructions)
            currentWaypointIndex = 0
            isCalculating = false
            deviationDetected = false
            totalDistanceMeters = response.totalDistanceMeters
            totalDurationSeconds = response.totalDurationSeconds

            onCompleted(
                RouteEngineState(
                    waypoints = waypoints,
                    currentWaypointIndex = currentWaypointIndex,
                    isCalculated = true,
                    deviationDetected = false,
                    totalDistanceMeters = totalDistanceMeters,
                    totalDurationSeconds = totalDurationSeconds
                )
            )
        }
    }

    private fun triggerReRoute(currentLoc: Location) {
        val dest = activeDestination ?: return
        val now = System.currentTimeMillis()
        if (now - lastDeviationTriggerTime > DEVIATION_RETRY_INTERVAL_MS) {
            lastDeviationTriggerTime = now
            requestRoute(currentLoc, dest) {}
        }
    }

    private fun checkDeviation(userLoc: Location): Boolean {
        val activeWaypoint = getActiveWaypoint() ?: return false
        val prevWaypoint = waypoints.getOrNull((currentWaypointIndex - 1).coerceAtLeast(0)) ?: return false

        val distToSegment = distanceToSegment(
            userLoc.latitude, userLoc.longitude,
            prevWaypoint.location.latitude, prevWaypoint.location.longitude,
            activeWaypoint.location.latitude, activeWaypoint.location.longitude
        )
        return distToSegment > 30.0
    }

    private fun distanceToSegment(
        px: Double, py: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) {
            return calculateDistance(px, py, x1, y1).toDouble()
        }

        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val nearestLat: Double
        val nearestLng: Double

        if (t < 0.0) {
            nearestLat = x1
            nearestLng = y1
        } else if (t > 1.0) {
            nearestLat = x2
            nearestLng = y2
        } else {
            nearestLat = x1 + t * dx
            nearestLng = y1 + t * dy
        }

        return calculateDistance(px, py, nearestLat, nearestLng).toDouble()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                SimpleRouteProviderCalculations.sinHalf(dLon)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}
/**
* Singleton instance or companion helper for Simple Route Provider Calculations.
*/
object SimpleRouteProviderCalculations {
    fun sinHalf(dLon: Double): Double {
        return Math.sin(dLon / 2) * Math.sin(dLon / 2)
    }
}
