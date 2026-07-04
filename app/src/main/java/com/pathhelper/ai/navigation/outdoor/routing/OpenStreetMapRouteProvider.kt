package com.pathhelper.ai.navigation.outdoor.routing

import android.location.Location
import com.pathhelper.ai.navigation.common.contracts.RouteProvider
/**
* Coordinates Routing Location operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Routing Location.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class RoutingLocation(private val lat: Double, private val lng: Double) : Location("osm") {
    override fun getLatitude(): Double = lat
    override fun getLongitude(): Double = lng
}
/**
* Coordinates Open Street Map Route Provider operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Open Street Map Route Provider.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class OpenStreetMapRouteProvider : RouteProvider {

    override fun calculateRoute(start: Location, destination: Location, callback: (List<Location>) -> Unit) {
        calculateDetailedRoute(start, destination) { response ->
            callback.invoke(response.waypoints.map { it.location })
        }
    }

    fun calculateDetailedRoute(start: Location, destination: Location, callback: (RouteResponse) -> Unit) {
        val startLat = start.latitude
        val startLng = start.longitude
        val destLat = destination.latitude
        val destLng = destination.longitude

        val w1 = RouteWaypoint(
            location = RoutingLocation(
                startLat + (destLat - startLat) * 0.33,
                startLng + (destLng - startLng) * 0.1
            ),
            name = "Main St",
            instruction = "Turn right onto Main Street"
        )
        val w2 = RouteWaypoint(
            location = RoutingLocation(
                startLat + (destLat - startLat) * 0.66,
                startLng + (destLng - startLng) * 0.9
            ),
            name = "Oak Ave",
            instruction = "Turn left onto Oak Avenue"
        )
        val w3 = RouteWaypoint(
            location = RoutingLocation(destLat, destLng),
            name = "Destination",
            instruction = "Arrive at destination"
        )

        val waypoints = listOf(
            RouteWaypoint(start, "Start", "Depart from start point"),
            w1,
            w2,
            w3
        )

        val d1 = calculateDistance(startLat, startLng, w1.location.latitude, w1.location.longitude)
        val d2 = calculateDistance(w1.location.latitude, w1.location.longitude, w2.location.latitude, w2.location.longitude)
        val d3 = calculateDistance(w2.location.latitude, w2.location.longitude, w3.location.latitude, w3.location.longitude)

        val instructions = listOf(
            RouteInstruction("Depart from start point", "DEPART", d1),
            RouteInstruction("Turn right onto Main Street", "RIGHT", d2),
            RouteInstruction("Turn left onto Oak Avenue", "LEFT", d3),
            RouteInstruction("Arrive at destination", "ARRIVE", 0f)
        )

        val totalDist = d1 + d2 + d3
        val response = RouteResponse(
            waypoints = waypoints,
            instructions = instructions,
            totalDistanceMeters = totalDist,
            totalDurationSeconds = totalDist / 1.4f,
            status = "OK"
        )
        callback.invoke(response)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}
