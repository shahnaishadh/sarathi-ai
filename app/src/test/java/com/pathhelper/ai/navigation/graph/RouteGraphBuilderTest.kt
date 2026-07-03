package com.pathhelper.ai.navigation.graph

import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.world.WorldRelationship
import com.pathhelper.ai.world.LandmarkRelation
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.route.RouteLandmark
import com.pathhelper.ai.route.RouteEvent
import com.pathhelper.ai.navigation.HorizontalZone
import org.junit.Assert.*
import org.junit.Test

class RouteGraphBuilderTest {
    @Test
    fun testGraphBuilder() {
        val builder = RouteGraphBuilder()

        // 1. Create a World Model with Door -> Hallway
        val door = Landmark("DOOR_01", LandmarkType.DOOR, 3.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val hallway = Landmark("HALLWAY_01", LandmarkType.HALLWAY, 6.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val relation = WorldRelationship("DOOR_01", "HALLWAY_01", LandmarkRelation.CONNECTED_TO)

        val worldModel = WorldModel(listOf(door, hallway), listOf(relation))
        val routeMemory = RouteMemory(emptyList())

        val (graph, metadata) = builder.build(worldModel, routeMemory)
        assertTrue(metadata.successful)
        assertEquals(2, metadata.nodeCount)
        assertEquals(1, metadata.edgeCount)

        val firstEdge = graph.edges.first()
        assertEquals("DOOR_01", firstEdge.source.nodeId)
        assertEquals("HALLWAY_01", firstEdge.destination.nodeId)
        assertEquals(GraphRelationship.CONNECTED_TO, firstEdge.relationship)
        assertEquals(3.0f, firstEdge.weight, 0.01f) // |3.0 - 6.0| = 3.0
    }

    @Test
    fun testGraphBuilderWithHistoricalRoute() {
        val builder = RouteGraphBuilder()

        // Visible landmarks (Elevator only)
        val elevator = Landmark("ELEVATOR_01", LandmarkType.ELEVATOR, 2.0f, HorizontalZone.CENTER, 0.9f, 1000L, 1000L)
        val worldModel = WorldModel(listOf(elevator), emptyList())

        // Traversal history: Door -> Hallway -> Elevator
        val routeMemory = RouteMemory(listOf(
            RouteLandmark("DOOR_01", LandmarkType.DOOR, 1000L, 2000L, 1, RouteEvent.PASSED),
            RouteLandmark("HALLWAY_01", LandmarkType.HALLWAY, 1000L, 2000L, 1, RouteEvent.REVISITED),
            RouteLandmark("ELEVATOR_01", LandmarkType.ELEVATOR, 1000L, 2000L, 1, RouteEvent.REACHED)
        ))

        val (graph, metadata) = builder.build(worldModel, routeMemory)
        assertTrue(metadata.successful)
        assertEquals(3, graph.nodes.size) // Door, Hallway, Elevator
        assertEquals(2, graph.edges.size) // Door -> Hallway, Hallway -> Elevator
    }
}
