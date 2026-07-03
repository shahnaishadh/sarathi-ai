package com.pathhelper.ai.navigation.persistence

import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.navigation.graph.GraphRelationship
import org.junit.Assert.*
import org.junit.Test

class MapSerializerTest {
    @Test
    fun testSerializationAndDeserialization() {
        val serializer = MapSerializer()

        // 1. Construct map nodes
        val node1 = MapNode("DOOR_01", LandmarkType.DOOR, 0.9f)
        val node2 = MapNode("HALLWAY_01", LandmarkType.HALLWAY, 0.8f)

        // 2. Construct map edges
        val edge = MapEdge("DOOR_01", "HALLWAY_01", GraphRelationship.CONNECTED_TO, 3.5f)

        val map = PersistentMap(
            mapId = "test_map",
            nodes = listOf(node1, node2),
            edges = listOf(edge),
            createdTimestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            version = 3
        )

        val jsonStr = serializer.serialize(map)
        assertNotNull(jsonStr)
        assertTrue(jsonStr.contains("test_map"))

        val deserialized = serializer.deserialize(jsonStr)
        assertEquals(map.mapId, deserialized.mapId)
        assertEquals(map.createdTimestamp, deserialized.createdTimestamp)
        assertEquals(map.lastModifiedTimestamp, deserialized.lastModifiedTimestamp)
        assertEquals(map.version, deserialized.version)

        assertEquals(2, deserialized.nodes.size)
        assertEquals("DOOR_01", deserialized.nodes[0].nodeId)
        assertEquals(LandmarkType.DOOR, deserialized.nodes[0].landmarkType)
        assertEquals(0.9f, deserialized.nodes[0].confidence, 0.01f)

        assertEquals(1, deserialized.edges.size)
        assertEquals("DOOR_01", deserialized.edges[0].sourceId)
        assertEquals("HALLWAY_01", deserialized.edges[0].destinationId)
        assertEquals(GraphRelationship.CONNECTED_TO, deserialized.edges[0].relationship)
        assertEquals(3.5f, deserialized.edges[0].weight, 0.01f)
    }
}
