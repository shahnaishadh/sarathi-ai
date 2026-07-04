package com.pathhelper.ai.navigation.outdoor.gps

import android.location.Location
import com.pathhelper.ai.navigation.common.contracts.RouteProvider
/**
* Coordinates Simple Route Provider operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Simple Route Provider.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class SimpleRouteProvider : RouteProvider {
    override fun calculateRoute(start: Location, destination: Location, callback: (List<Location>) -> Unit) {
        val points = mutableListOf<Location>()
        val segments = 3
        for (i in 1..segments) {
            val fraction = i.toDouble() / segments.toDouble()
            val lat = start.latitude + (destination.latitude - start.latitude) * fraction
            val lon = start.longitude + (destination.longitude - start.longitude) * fraction

            // Create Android Location instance with provider name
            val loc = Location("simple_route").apply {
                latitude = lat
                longitude = lon
            }
            points.add(loc)
        }
        callback(points)
    }
}
