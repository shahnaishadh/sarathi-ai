package com.pathhelper.ai.navigation.graph

import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import org.junit.Assert.*
import org.junit.Test

class RoutePlannerTest {
    @Test
    fun testShortestPathPlanner() {
        val planner = RoutePlanner()

        // Construct nodes: Door, Hallway, Elevator
        val doorNode = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        val hallwayNode = GraphNode("HALLWAY_01", LandmarkType.HALLWAY, HorizontalZone.CENTER, 0.9f)
        val elevatorNode = GraphNode("ELEVATOR_01", LandmarkType.ELEVATOR, HorizontalZone.CENTER, 0.9f)

        // Construct edges: Door -> Hallway (weight 2.0), Hallway -> Elevator (weight 3.0)
        val edges = listOf(
            GraphEdge(doorNode, hallwayNode, GraphRelationship.CONNECTED_TO, 2.0f),
            GraphEdge(hallwayNode, elevatorNode, GraphRelationship.CONNECTED_TO, 3.0f)
        )
        val graph = RouteGraph(listOf(doorNode, hallwayNode, elevatorNode), edges, 1000L)

        // Set World Model: Door at 1.0m, Hallway at 3.0m, Elevator at 6.0m
        val doorLm = Landmark("DOOR_01", LandmarkType.DOOR, 1.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val hallwayLm = Landmark("HALLWAY_01", LandmarkType.HALLWAY, 3.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val elevatorLm = Landmark("ELEVATOR_01", LandmarkType.ELEVATOR, 6.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val worldModel = WorldModel(listOf(doorLm, hallwayLm, elevatorLm), emptyList())

        // Target: Elevator
        val destination = LandmarkTarget(LandmarkType.ELEVATOR)

        val plan = planner.plan(graph, destination, worldModel)
        assertNotNull(plan.currentNode)
        assertEquals("DOOR_01", plan.currentNode!!.nodeId) // closest landmark is Door (1.0m)
        assertEquals(3, plan.plannedPath.size)
        assertEquals("DOOR_01", plan.plannedPath[0].nodeId)
        assertEquals("HALLWAY_01", plan.plannedPath[1].nodeId)
        assertEquals("ELEVATOR_01", plan.plannedPath[2].nodeId)

        // Check instructions
        assertEquals(2, plan.instructions.size)
        assertTrue(plan.instructions[0].contains("hallway", ignoreCase = true))
        assertTrue(plan.instructions[1].contains("elevator", ignoreCase = true))

        // Check estimated distance: 2.0 + 3.0 = 5.0
        assertNotNull(plan.estimatedDistanceMeters)
        assertEquals(5.0f, plan.estimatedDistanceMeters!!, 0.01f)
    }

    @Test
    fun testAlternativeRouteSelection() {
        val planner = RoutePlanner()

        val doorNode = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        val hallwayNode = GraphNode("HALLWAY_01", LandmarkType.HALLWAY, HorizontalZone.CENTER, 0.9f)
        val elevatorNode = GraphNode("ELEVATOR_01", LandmarkType.ELEVATOR, HorizontalZone.CENTER, 0.9f)

        // Path A: Door -> Hallway -> Elevator (weight 2.0 + 3.0 = 5.0)
        // Path B: Door -> Elevator Direct (weight 10.0)
        val edges = listOf(
            GraphEdge(doorNode, hallwayNode, GraphRelationship.CONNECTED_TO, 2.0f),
            GraphEdge(hallwayNode, elevatorNode, GraphRelationship.CONNECTED_TO, 3.0f),
            GraphEdge(doorNode, elevatorNode, GraphRelationship.CONNECTED_TO, 10.0f)
        )
        val graph = RouteGraph(listOf(doorNode, hallwayNode, elevatorNode), edges, 1000L)

        val doorLm = Landmark("DOOR_01", LandmarkType.DOOR, 1.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val worldModel = WorldModel(listOf(doorLm), emptyList())

        val destination = LandmarkTarget(LandmarkType.ELEVATOR)

        val plan = planner.plan(graph, destination, worldModel)
        assertEquals(3, plan.plannedPath.size) // Selects path with weight 5.0 (Door -> Hallway -> Elevator) instead of Direct (weight 10.0)
        assertEquals("HALLWAY_01", plan.plannedPath[1].nodeId)
    }

    @Test
    fun testNoRouteAvailable() {
        val planner = RoutePlanner()

        val doorNode = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        val elevatorNode = GraphNode("ELEVATOR_01", LandmarkType.ELEVATOR, HorizontalZone.CENTER, 0.9f)

        // No connection edge
        val graph = RouteGraph(listOf(doorNode, elevatorNode), emptyList(), 1000L)

        val doorLm = Landmark("DOOR_01", LandmarkType.DOOR, 1.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val worldModel = WorldModel(listOf(doorLm), emptyList())

        val destination = LandmarkTarget(LandmarkType.ELEVATOR)

        val plan = planner.plan(graph, destination, worldModel)
        assertTrue(plan.plannedPath.isEmpty())
        assertTrue(plan.instructions.isEmpty())
        assertNull(plan.estimatedDistanceMeters)
    }
}
