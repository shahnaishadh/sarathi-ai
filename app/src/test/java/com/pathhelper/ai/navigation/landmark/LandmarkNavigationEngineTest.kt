package com.pathhelper.ai.navigation.landmark

import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.context.ContextObservation
import com.pathhelper.ai.context.ContextPriority
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.route.RouteLandmark
import com.pathhelper.ai.route.RouteEvent
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.graph.RoutePlan
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.NavigationStep
import org.junit.Assert.*
import org.junit.Test

class LandmarkNavigationEngineTest {
    @Test
    fun testNavigationScenarios() {
        val engine = LandmarkNavigationEngine()
        val destination = LandmarkTarget(LandmarkType.ELEVATOR)

        // Scenario 5: Destination absent (SEARCHING)
        var worldModel = WorldModel(emptyList(), emptyList())
        var context = NavigationContext(null, null, null, "Open Path Forward", emptyList())
        var routeMemory = RouteMemory(emptyList())
        var routePlan = RoutePlan(destination, null, emptyList(), emptyList(), null)

        var (state, metadata) = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertTrue(metadata.successful)
        assertFalse(metadata.destinationFound)
        assertEquals(NavigationProgress.SEARCHING, state.progress)
        assertNotNull(state.currentStep)
        assertTrue(state.currentStep!!.instruction.contains("searching", ignoreCase = true))

        // Scenario 1: Destination visible (ROUTE_FOUND)
        val elevator = Landmark("ELEVATOR_01", LandmarkType.ELEVATOR, 5.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        worldModel = WorldModel(listOf(elevator), emptyList())
        context = NavigationContext(
            primaryLandmark = ContextObservation("ELEVATOR_01", LandmarkType.ELEVATOR, ContextPriority.HIGH, 5.0f, "Elevator ahead"),
            secondaryLandmark = null,
            approachingLandmark = null,
            activeContext = "Elevator Available",
            observations = emptyList()
        )

        // Create dummy graph node to simulate visible destination path
        val elevatorNode = com.pathhelper.ai.navigation.graph.GraphNode("ELEVATOR_01", LandmarkType.ELEVATOR, HorizontalZone.CENTER, 0.9f)
        routePlan = RoutePlan(destination, elevatorNode, listOf(elevatorNode), listOf("Elevator ahead"), 5.0f)

        val result1 = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertTrue(result1.second.destinationFound)
        assertEquals(NavigationProgress.ROUTE_FOUND, result1.first.progress)

        // Scenario 2: Distance decreasing (APPROACHING)
        context = NavigationContext(
            primaryLandmark = ContextObservation("ELEVATOR_01", LandmarkType.ELEVATOR, ContextPriority.HIGH, 3.0f, "Elevator ahead"),
            secondaryLandmark = null,
            approachingLandmark = ContextObservation("ELEVATOR_01", LandmarkType.ELEVATOR, ContextPriority.HIGH, 3.0f, "Elevator ahead"),
            activeContext = "Elevator Available",
            observations = emptyList()
        )

        val result2 = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertEquals(NavigationProgress.APPROACHING, result2.first.progress)

        // Scenario 3: Distance < 1 meter (ARRIVED)
        val elevatorArrived = Landmark("ELEVATOR_01", LandmarkType.ELEVATOR, 0.8f, HorizontalZone.CENTER, 0.9f, 1000L, 2000L)
        worldModel = WorldModel(listOf(elevatorArrived), emptyList())
        routePlan = RoutePlan(destination, elevatorNode, listOf(elevatorNode), listOf("Elevator ahead"), 0.8f)

        val result3 = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertEquals(NavigationProgress.ARRIVED, result3.first.progress)

        // Scenario 4: Destination disappears (LOST)
        worldModel = WorldModel(emptyList(), emptyList())
        routePlan = RoutePlan(destination, null, emptyList(), emptyList(), null)
        val result4 = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertEquals(NavigationProgress.LOST, result4.first.progress)

        // Scenario 6: Door passed previously (Door instruction suppressed)
        val elevatorWithDoor = Landmark("ELEVATOR_01", LandmarkType.ELEVATOR, 5.0f, HorizontalZone.CENTER, 0.9f, 1000L, 3000L)
        val door = Landmark("DOOR_01", LandmarkType.DOOR, 3.0f, HorizontalZone.CENTER, 0.9f, 1000L, 3000L)
        worldModel = WorldModel(listOf(elevatorWithDoor, door), emptyList())
        context = NavigationContext(
            primaryLandmark = ContextObservation("ELEVATOR_01", LandmarkType.ELEVATOR, ContextPriority.HIGH, 5.0f, "Elevator ahead"),
            secondaryLandmark = null,
            approachingLandmark = null,
            activeContext = "Elevator Available",
            observations = emptyList()
        )
        val doorNode = com.pathhelper.ai.navigation.graph.GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        routePlan = RoutePlan(destination, doorNode, listOf(doorNode, elevatorNode), listOf("Pass doorway", "Elevator ahead"), 5.0f)

        // Test with door NOT passed first
        val resultWithoutPass = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertEquals(NavigationProgress.ROUTE_FOUND, resultWithoutPass.first.progress)
        
        // Test with door PASSED in route memory
        routeMemory = RouteMemory(listOf(
            RouteLandmark("DOOR_01", LandmarkType.DOOR, 1000L, 2000L, 1, RouteEvent.PASSED)
        ))
        val resultWithPass = engine.navigate(destination, worldModel, context, routeMemory, routePlan)
        assertEquals(NavigationProgress.ROUTE_FOUND, resultWithPass.first.progress)
    }
}
