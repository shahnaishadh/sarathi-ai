package com.pathhelper.ai.route

import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.context.ContextObservation
import com.pathhelper.ai.context.ContextPriority
import com.pathhelper.ai.navigation.HorizontalZone
import org.junit.Assert.*
import org.junit.Test

class RouteMemoryEngineTest {
    @Test
    fun testRouteMemoryProgression() {
        val engine = RouteMemoryEngine()

        // Scenario 1: Door appears (DISCOVERED)
        val door = Landmark("DOOR_01", LandmarkType.DOOR, 3.5f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        var worldModel = WorldModel(listOf(door), emptyList())
        var navContext = NavigationContext(
            primaryLandmark = ContextObservation("DOOR_01", LandmarkType.DOOR, ContextPriority.HIGH, 3.5f, "Door ahead"),
            secondaryLandmark = null,
            approachingLandmark = null,
            activeContext = "Open Path Forward",
            observations = emptyList()
        )

        var (memory, metadata) = engine.update(worldModel, navContext)
        assertTrue(metadata.successful)
        assertEquals(1, memory.landmarks.size)
        assertEquals(RouteEvent.DISCOVERED, memory.landmarks[0].event)
        assertEquals(1, memory.landmarks[0].visitCount)
        assertEquals(1, engine.lastEvents.size)
        assertEquals(RouteEvent.DISCOVERED, engine.lastEvents[0].second)

        // Scenario 2: Distance decreasing (APPROACHING)
        val doorApproaching = Landmark("DOOR_01", LandmarkType.DOOR, 2.0f, HorizontalZone.CENTER, 0.9f, 1000L, 2000L)
        worldModel = WorldModel(listOf(doorApproaching), emptyList())
        navContext = NavigationContext(
            primaryLandmark = ContextObservation("DOOR_01", LandmarkType.DOOR, ContextPriority.HIGH, 2.0f, "Door ahead"),
            secondaryLandmark = null,
            approachingLandmark = ContextObservation("DOOR_01", LandmarkType.DOOR, ContextPriority.HIGH, 2.0f, "Door ahead"),
            activeContext = "Approaching Entrance",
            observations = emptyList()
        )

        val result2 = engine.update(worldModel, navContext)
        assertEquals(RouteEvent.APPROACHING, result2.first.landmarks[0].event)

        // Scenario 3: Distance < 1 meter (REACHED)
        val doorReached = Landmark("DOOR_01", LandmarkType.DOOR, 0.8f, HorizontalZone.CENTER, 0.9f, 1000L, 3000L)
        worldModel = WorldModel(listOf(doorReached), emptyList())
        navContext = NavigationContext(
            primaryLandmark = ContextObservation("DOOR_01", LandmarkType.DOOR, ContextPriority.HIGH, 0.8f, "Door ahead"),
            secondaryLandmark = null,
            approachingLandmark = null,
            activeContext = "Open Path Forward",
            observations = emptyList()
        )

        val result3 = engine.update(worldModel, navContext)
        assertEquals(RouteEvent.REACHED, result3.first.landmarks[0].event)

        // Scenario 4: Door disappears (PASSED)
        worldModel = WorldModel(emptyList(), emptyList())
        val result4 = engine.update(worldModel, navContext)
        assertEquals(RouteEvent.PASSED, result4.first.landmarks[0].event)

        // Scenario 5: Door appears again (REVISITED)
        worldModel = WorldModel(listOf(door), emptyList())
        val result5 = engine.update(worldModel, navContext)
        assertEquals(RouteEvent.REVISITED, result5.first.landmarks[0].event)
        assertEquals(2, result5.first.landmarks[0].visitCount)
    }
}
