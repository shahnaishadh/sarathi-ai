package com.pathhelper.ai.navigation.persistence

import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.graph.GraphNode
import com.pathhelper.ai.navigation.graph.GraphEdge
import com.pathhelper.ai.navigation.graph.GraphRelationship
import com.pathhelper.ai.navigation.graph.RouteGraph
import org.junit.Assert.*
import org.junit.Test

class MapLearningEngineTest {
    @Test
    fun testInitialLearning() {
        val learningEngine = MapLearningEngine()

        val node = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        val graph = RouteGraph(listOf(node), emptyList(), 1000L)

        val map = learningEngine.learn(graph, null, "test_learning_map")
        assertEquals("test_learning_map", map.mapId)
        assertEquals(1, map.version)
        assertEquals(1, map.nodes.size)
        assertEquals("DOOR_01", map.nodes[0].nodeId)
        assertEquals(0.9f, map.nodes[0].confidence, 0.01f)
    }

    @Test
    fun testIncrementalMergingAndDecay() {
        val learningEngine = MapLearningEngine()

        // 1. Initial State: Map has Door at 0.5 confidence, and Hallway at 0.8 confidence
        val node1 = MapNode("DOOR_01", LandmarkType.DOOR, 0.5f)
        val node2 = MapNode("HALLWAY_01", LandmarkType.HALLWAY, 0.8f)
        val existingMap = PersistentMap(
            mapId = "test_map",
            nodes = listOf(node1, node2),
            edges = emptyList(),
            createdTimestamp = 1000L,
            lastModifiedTimestamp = 1000L,
            version = 1
        )

        // 2. Active Graph has Door (revisited) and Elevator (newly discovered). Hallway is absent (should decay)
        val graphNode1 = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 0.9f)
        val graphNode2 = GraphNode("ELEVATOR_01", LandmarkType.ELEVATOR, HorizontalZone.CENTER, 0.7f)
        val graph = RouteGraph(listOf(graphNode1, graphNode2), emptyList(), 2000L)

        val updatedMap = learningEngine.learn(graph, existingMap, "test_map")
        assertEquals(2, updatedMap.version)
        assertEquals(3, updatedMap.nodes.size) // Door, Hallway, Elevator

        // Door confidence boosted: 0.5 + 0.1 = 0.6
        val doorResult = updatedMap.nodes.find { it.nodeId == "DOOR_01" }
        assertNotNull(doorResult)
        assertEquals(0.6f, doorResult!!.confidence, 0.01f)

        // Hallway confidence decayed: 0.8 - 0.05 = 0.75
        val hallwayResult = updatedMap.nodes.find { it.nodeId == "HALLWAY_01" }
        assertNotNull(hallwayResult)
        assertEquals(0.75f, hallwayResult!!.confidence, 0.01f)

        // Elevator inserted at 0.7
        val elevatorResult = updatedMap.nodes.find { it.nodeId == "ELEVATOR_01" }
        assertNotNull(elevatorResult)
        assertEquals(0.7f, elevatorResult!!.confidence, 0.01f)
    }

    @Test
    fun testEdgeWeightReinforcement() {
        val learningEngine = MapLearningEngine()

        val mapNode1 = MapNode("DOOR_01", LandmarkType.DOOR, 1.0f)
        val mapNode2 = MapNode("HALLWAY_01", LandmarkType.HALLWAY, 1.0f)
        val mapEdge = MapEdge("DOOR_01", "HALLWAY_01", GraphRelationship.CONNECTED_TO, 10.0f)
        val existingMap = PersistentMap(
            mapId = "test_map",
            nodes = listOf(mapNode1, mapNode2),
            edges = listOf(mapEdge),
            createdTimestamp = 1000L,
            lastModifiedTimestamp = 1000L,
            version = 1
        )

        val graphNode1 = GraphNode("DOOR_01", LandmarkType.DOOR, HorizontalZone.CENTER, 1.0f)
        val graphNode2 = GraphNode("HALLWAY_01", LandmarkType.HALLWAY, HorizontalZone.CENTER, 1.0f)
        val graphEdge = GraphEdge(graphNode1, graphNode2, GraphRelationship.CONNECTED_TO, 5.0f)
        val graph = RouteGraph(listOf(graphNode1, graphNode2), listOf(graphEdge), 2000L)

        val updatedMap = learningEngine.learn(graph, existingMap, "test_map")
        assertEquals(1, updatedMap.edges.size)
        // Reinforced weight: 10.0 * 0.8 + 5.0 * 0.2 = 9.0
        assertEquals(9.0f, updatedMap.edges[0].weight, 0.01f)
    }
}
