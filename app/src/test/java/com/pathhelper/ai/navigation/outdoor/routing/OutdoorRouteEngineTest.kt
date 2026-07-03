package com.pathhelper.ai.navigation.outdoor.routing

import android.location.Location
import org.junit.Assert.*
import org.junit.Test

class OutdoorRouteEngineTest {

    class TestLocation(
        private val mockLat: Double,
        private val mockLng: Double
    ) : Location("gps") {
        override fun getLatitude(): Double = mockLat
        override fun getLongitude(): Double = mockLng
    }

    @Test
    fun testRequestRouteAndFetch() {
        val provider = OpenStreetMapRouteProvider()
        val engine = OutdoorRouteEngine(provider)

        val start = TestLocation(37.7700, -122.4100)
        val dest = TestLocation(37.7749, -122.4194)

        var routeCalculated = false
        engine.requestRoute(start, dest) { state ->
            routeCalculated = true
            assertTrue(state.isCalculated)
            assertEquals(4, state.waypoints.size)
            assertEquals(0, state.currentWaypointIndex)
            assertFalse(state.deviationDetected)
        }
        assertTrue(routeCalculated)
    }

    @Test
    fun testWaypointProgression() {
        val provider = OpenStreetMapRouteProvider()
        val engine = OutdoorRouteEngine(provider)

        val start = TestLocation(37.7700, -122.4100)
        val dest = TestLocation(37.7749, -122.4194)

        engine.requestRoute(start, dest) {}

        // User is at start location. Since start matches waypoints[0], index advances to 1
        val state1 = engine.updateRouteProgress(start)
        assertEquals(1, state1.currentWaypointIndex)

        // User moves close to waypoint 1 (Main St) at lat = 37.7716, lng = -122.4109
        val closeToW1 = TestLocation(37.7716, -122.4109)
        val state2 = engine.updateRouteProgress(closeToW1)
        
        // Waypoint index advances to 2 because we reached waypoint 1
        assertEquals(2, state2.currentWaypointIndex)
    }

    @Test
    fun testRouteDeviationDetection() {
        val provider = OpenStreetMapRouteProvider()
        val engine = OutdoorRouteEngine(provider)

        val start = TestLocation(37.7700, -122.4100)
        val dest = TestLocation(37.7749, -122.4194)

        engine.requestRoute(start, dest) {}

        // User drifts far away (e.g. 1 km East of start point)
        val farAwayLoc = TestLocation(37.7700, -122.3900)
        val state = engine.updateRouteProgress(farAwayLoc)

        // Route Engine detects deviation, triggers re-routing immediately, which resets deviation flag
        assertFalse(state.deviationDetected)
        assertEquals(1, engine.getRecalculationsCount())
    }
}
