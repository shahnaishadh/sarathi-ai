package com.pathhelper.ai.navigation.graph

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.LandmarkRelation
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.persistence.PersistentMap
/**
* Coordinates Route Graph Builder operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Graph Builder.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class RouteGraphBuilder {
    fun build(
        worldModel: WorldModel,
        routeMemory: RouteMemory,
        persistentMap: PersistentMap? = null
    ): Pair<RouteGraph, RouteGraphMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        try {
            val nodes = worldModel.landmarks.map { lm ->
                GraphNode(
                    nodeId = lm.id,
                    landmarkType = lm.type,
                    position = lm.horizontalZone,
                    confidence = lm.confidence
                )
            }.toMutableList()

            // 1. Connect historical landmarks from route memory to build continuous paths
            for (routeLm in routeMemory.landmarks) {
                if (nodes.none { it.nodeId == routeLm.landmarkId }) {
                    nodes.add(
                        GraphNode(
                            nodeId = routeLm.landmarkId,
                            landmarkType = routeLm.landmarkType,
                            position = HorizontalZone.CENTER,
                            confidence = 1.0f
                        )
                    )
                }
            }

            // 2. Connect historical map nodes from PersistentMap
            persistentMap?.let { map ->
                for (mapNode in map.nodes) {
                    if (nodes.none { it.nodeId == mapNode.nodeId }) {
                        nodes.add(
                            GraphNode(
                                nodeId = mapNode.nodeId,
                                landmarkType = mapNode.landmarkType,
                                position = HorizontalZone.CENTER,
                                confidence = mapNode.confidence
                            )
                        )
                    }
                }
            }

            val edges = mutableListOf<GraphEdge>()

            // 3. Map current World Model relationships
            for (relation in worldModel.relations) {
                val srcNode = nodes.find { it.nodeId == relation.sourceId }
                val destNode = nodes.find { it.nodeId == relation.targetId }

                if (srcNode != null && destNode != null) {
                    val graphRel = when (relation.relation) {
                        LandmarkRelation.CONNECTED_TO -> GraphRelationship.CONNECTED_TO
                        LandmarkRelation.AHEAD_OF -> GraphRelationship.AHEAD_OF
                        LandmarkRelation.LEFT_OF -> GraphRelationship.LEFT_OF
                        LandmarkRelation.RIGHT_OF -> GraphRelationship.RIGHT_OF
                        else -> GraphRelationship.CONNECTED_TO
                    }

                    val srcLm = worldModel.landmarks.find { it.id == relation.sourceId }
                    val destLm = worldModel.landmarks.find { it.id == relation.targetId }
                    val weight = if (srcLm?.distanceMeters != null && destLm?.distanceMeters != null) {
                        Math.abs(srcLm.distanceMeters - destLm.distanceMeters)
                    } else {
                        1.0f
                    }

                    edges.add(GraphEdge(srcNode, destNode, graphRel, weight))
                }
            }

            // 4. Link consecutive route memory landmarks
            for (i in 0 until routeMemory.landmarks.size - 1) {
                val srcLm = routeMemory.landmarks[i]
                val destLm = routeMemory.landmarks[i + 1]
                val srcNode = nodes.find { it.nodeId == srcLm.landmarkId }
                val destNode = nodes.find { it.nodeId == destLm.landmarkId }

                if (srcNode != null && destNode != null) {
                    if (edges.none { it.source.nodeId == srcNode.nodeId && it.destination.nodeId == destNode.nodeId }) {
                        edges.add(GraphEdge(srcNode, destNode, GraphRelationship.CONNECTED_TO, 1.0f))
                    }
                }
            }

            // 5. Connect persistent map edges from PersistentMap
            persistentMap?.let { map ->
                for (mapEdge in map.edges) {
                    val srcNode = nodes.find { it.nodeId == mapEdge.sourceId }
                    val destNode = nodes.find { it.nodeId == mapEdge.destinationId }

                    if (srcNode != null && destNode != null) {
                        if (edges.none { it.source.nodeId == srcNode.nodeId && it.destination.nodeId == destNode.nodeId }) {
                            edges.add(GraphEdge(srcNode, destNode, mapEdge.relationship, mapEdge.weight))
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d("RouteGraph", "GRAPH CREATED Nodes: ${nodes.size} Edges: ${edges.size}")
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            val metadata = RouteGraphMetadata(
                nodeCount = nodes.size,
                edgeCount = edges.size,
                processingTimeMs = duration,
                successful = true
            )

            return Pair(RouteGraph(nodes, edges, currentTime), metadata)
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackMetadata = RouteGraphMetadata(
                nodeCount = 0,
                edgeCount = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown Route Graph builder update error."
            )
            return Pair(RouteGraph(emptyList(), emptyList(), currentTime), fallbackMetadata)
        }
    }
}
