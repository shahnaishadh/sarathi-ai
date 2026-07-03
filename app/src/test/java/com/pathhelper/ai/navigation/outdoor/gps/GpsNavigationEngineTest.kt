package com.pathhelper.ai.navigation.outdoor.gps

import android.location.Location
import com.pathhelper.ai.navigation.common.target.GpsTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import org.junit.Assert.*
import org.junit.Test

class GpsNavigationEngineTest {

    class TestLocation(
        private val mockLat: Double,
        private val mockLng: Double,
        private val mockBearing: Float = 0f,
        private val mockAccuracy: Float = 5f
    ) : Location("gps") {
        override fun getLatitude(): Double = mockLat
        override fun getLongitude(): Double = mockLng
        override fun getBearing(): Float = mockBearing
        override fun getAccuracy(): Float = mockAccuracy
    }

    @Test
    fun testGpsSearchingState() {
        val engine = GpsNavigationEngine()
        val target = GpsTarget(37.7749, -122.4194)

        // 1. If location is null: state should be SEARCHING
        val (state1, metadata1) = engine.navigate(target, null, null, false, 0)
        assertFalse(metadata1.hasLocation)
        assertEquals(NavigationProgress.SEARCHING, state1.progress)
        assertTrue(state1.currentInstruction!!.contains("Waiting", ignoreCase = true))

        // 2. If activeWaypoint is null: state should be CALCULATING route
        val startLoc = TestLocation(37.7700, -122.4100)
        val (state2, metadata2) = engine.navigate(target, startLoc, null, false, 0)
        assertTrue(metadata2.hasLocation)
        assertEquals(NavigationProgress.SEARCHING, state2.progress)
        assertTrue(state2.currentInstruction!!.contains("Calculating", ignoreCase = true))
    }

    @Test
    fun testGpsRouteCalculated() {
        val engine = GpsNavigationEngine()
        val target = GpsTarget(37.7749, -122.4194)

        // Start coordinate
        val startLoc = TestLocation(37.7700, -122.4100, mockBearing = 0f, mockAccuracy = 5f)
        val activeWaypoint = TestLocation(37.7701, -122.4101)

        // Target bearing is North West (~315 degrees). Heading is 0 (North). Diff is -45 -> Turn left!
        val (state2, metadata2) = engine.navigate(target, startLoc, activeWaypoint, false, 0)
        assertTrue(metadata2.successful)
        assertEquals(NavigationProgress.ROUTE_FOUND, state2.progress)
        assertNotNull(state2.currentInstruction)
        assertTrue(state2.currentInstruction!!.contains("Turn left", ignoreCase = true))
    }

    @Test
    fun testArrivalThreshold() {
        val engine = GpsNavigationEngine()
        val target = GpsTarget(37.7749, -122.4194)
        val closeLoc = TestLocation(37.7749, -122.4194, mockBearing = 0f)

        // User is close to destination target
        val (state2, _) = engine.navigate(target, closeLoc, closeLoc, false, 0)
        assertEquals(NavigationProgress.ARRIVED, state2.progress)
        assertTrue(state2.currentInstruction!!.contains("Arrived", ignoreCase = true))
    }
}
