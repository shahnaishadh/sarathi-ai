package com.pathhelper.ai.navigation.persistence

import android.os.SystemClock
import com.pathhelper.ai.navigation.graph.RouteGraph
/**
* Coordinates Map Learning Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Map Learning Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class MapLearningEngine {
    fun learn(
        graph: RouteGraph,
        existingMap: PersistentMap?,
        mapId: String = "main_indoor_map"
    ): PersistentMap {
        val currentTime = SystemClock.elapsedRealtime()

        if (existingMap == null) {
            val mapNodes = graph.nodes.map { node ->
                MapNode(node.nodeId, node.landmarkType, node.confidence)
            }
            val mapEdges = graph.edges.map { edge ->
                MapEdge(edge.source.nodeId, edge.destination.nodeId, edge.relationship, edge.weight)
            }
            return PersistentMap(
                mapId = mapId,
                nodes = mapNodes,
                edges = mapEdges,
                createdTimestamp = currentTime,
                lastModifiedTimestamp = currentTime,
                version = 1
            )
        }

        // 1. Merge Nodes
        val mergedNodes = existingMap.nodes.map { MapNode(it.nodeId, it.landmarkType, it.confidence) }.toMutableList()

        for (graphNode in graph.nodes) {
            val idx = mergedNodes.indexOfFirst { it.nodeId == graphNode.nodeId }
            if (idx != -1) {
                // Boost confidence for revisited nodes
                val oldNode = mergedNodes[idx]
                val boostedConfidence = Math.min(1.0f, oldNode.confidence + 0.1f)
                mergedNodes[idx] = MapNode(oldNode.nodeId, oldNode.landmarkType, boostedConfidence)
            } else {
                // Insert new nodes with initial confidence
                mergedNodes.add(MapNode(graphNode.nodeId, graphNode.landmarkType, graphNode.confidence))
            }
        }

        // Decay unobserved nodes
        for (i in 0 until mergedNodes.size) {
            val node = mergedNodes[i]
            if (graph.nodes.none { it.nodeId == node.nodeId }) {
                val decayedConfidence = Math.max(0.0f, node.confidence - 0.05f)
                mergedNodes[i] = MapNode(node.nodeId, node.landmarkType, decayedConfidence)
            }
        }
        // Remove nodes whose confidence has decayed completely
        mergedNodes.removeAll { it.confidence <= 0.1f }

        // 2. Merge Edges
        val mergedEdges = existingMap.edges.map { MapEdge(it.sourceId, it.destinationId, it.relationship, it.weight) }.toMutableList()

        for (graphEdge in graph.edges) {
            val idx = mergedEdges.indexOfFirst {
                it.sourceId == graphEdge.source.nodeId && it.destinationId == graphEdge.destination.nodeId
            }
            if (idx != -1) {
                // Reinforce edge weights (moving average)
                val oldEdge = mergedEdges[idx]
                val reinforcedWeight = oldEdge.weight * 0.8f + graphEdge.weight * 0.2f
                mergedEdges[idx] = MapEdge(oldEdge.sourceId, oldEdge.destinationId, oldEdge.relationship, reinforcedWeight)
            } else {
                // Add new edge connections
                // Ensure nodes exist in the node set before adding
                val srcNodeExists = mergedNodes.any { it.nodeId == graphEdge.source.nodeId }
                val destNodeExists = mergedNodes.any { it.nodeId == graphEdge.destination.nodeId }
                if (srcNodeExists && destNodeExists) {
                    mergedEdges.add(MapEdge(graphEdge.source.nodeId, graphEdge.destination.nodeId, graphEdge.relationship, graphEdge.weight))
                }
            }
        }

        // Clean up edges belonging to removed nodes
        mergedEdges.removeAll { edge ->
            mergedNodes.none { it.nodeId == edge.sourceId } || mergedNodes.none { it.nodeId == edge.destinationId }
        }

        return PersistentMap(
            mapId = mapId,
            nodes = mergedNodes,
            edges = mergedEdges,
            createdTimestamp = existingMap.createdTimestamp,
            lastModifiedTimestamp = currentTime,
            version = existingMap.version + 1
        )
    }
}
