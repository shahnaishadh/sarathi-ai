package com.pathhelper.ai.navigation.graph

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
/**
* Coordinates Route Planner operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Planner.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class RoutePlanner {
    fun plan(
        graph: RouteGraph,
        destination: NavigationTarget,
        worldModel: WorldModel
    ): RoutePlan {
        if (destination !is LandmarkTarget) {
            return RoutePlan(destination, null, emptyList(), emptyList(), null)
        }

        val targetType = destination.landmarkType

        // 1. Resolve Current Node (closest landmark in the World Model)
        val currentNode = worldModel.landmarks
            .filter { it.distanceMeters != null }
            .minByOrNull { it.distanceMeters!! }
            ?.let { startLm ->
                graph.nodes.find { it.nodeId == startLm.id }
            } ?: graph.nodes.firstOrNull()

        if (currentNode == null) {
            return RoutePlan(destination, null, emptyList(), emptyList(), null)
        }

        val destNode = graph.nodes.find { it.landmarkType == targetType }
        if (destNode == null) {
            return RoutePlan(destination, currentNode, emptyList(), emptyList(), null)
        }

        // 2. Execute Dijkstra's Algorithm
        val distances = mutableMapOf<GraphNode, Float>()
        val previous = mutableMapOf<GraphNode, GraphNode>()
        val unvisited = graph.nodes.toMutableSet()

        for (node in graph.nodes) {
            distances[node] = Float.MAX_VALUE
        }
        distances[currentNode] = 0.0f

        while (unvisited.isNotEmpty()) {
            val u = unvisited.minByOrNull { distances[it] ?: Float.MAX_VALUE } ?: break
            if (u == destNode || distances[u] == Float.MAX_VALUE) {
                break
            }
            unvisited.remove(u)

            // Find all outgoing edges from node u
            val outgoingEdges = graph.edges.filter { it.source.nodeId == u.nodeId }
            for (edge in outgoingEdges) {
                val v = edge.destination
                if (unvisited.contains(v)) {
                    val alt = (distances[u] ?: 0.0f) + edge.weight
                    if (alt < (distances[v] ?: Float.MAX_VALUE)) {
                        distances[v] = alt
                        previous[v] = u
                    }
                }
            }
        }

        // Reconstruct shortest path
        val path = mutableListOf<GraphNode>()
        var curr: GraphNode? = destNode
        while (curr != null) {
            path.add(0, curr)
            curr = previous[curr]
        }

        // Validate if path connects to starting node
        val validPath = if (path.firstOrNull()?.nodeId == currentNode.nodeId) {
            path.toList()
        } else {
            emptyList()
        }

        // 3. Generate dynamic instructions based on edge transitions
        val instructions = mutableListOf<String>()
        var totalDistance = 0.0f

        for (i in 0 until validPath.size - 1) {
            val u = validPath[i]
            val v = validPath[i + 1]

            val edge = graph.edges.find { it.source.nodeId == u.nodeId && it.destination.nodeId == v.nodeId }
            totalDistance += edge?.weight ?: 1.0f

            val stepText = when (edge?.relationship) {
                GraphRelationship.AHEAD_OF -> {
                    if (u.landmarkType == LandmarkType.DOOR && v.landmarkType == LandmarkType.CROSSWALK) {
                        "Crosswalk beyond doorway"
                    } else {
                        "${v.landmarkType.name.lowercase()} ahead"
                    }
                }
                GraphRelationship.CONNECTED_TO -> {
                    if (u.landmarkType == LandmarkType.HALLWAY && v.landmarkType == LandmarkType.DOOR) {
                        "Door at end of hallway"
                    } else if (u.landmarkType == LandmarkType.DOOR && v.landmarkType == LandmarkType.HALLWAY) {
                        "Continue through hallway"
                    } else if (v.landmarkType == LandmarkType.HALLWAY) {
                        "Continue through hallway"
                    } else {
                        "Proceed to ${v.landmarkType.name.lowercase()}"
                    }
                }
                else -> {
                    when (v.landmarkType) {
                        LandmarkType.DOOR, LandmarkType.ENTRANCE -> "Proceed through doorway"
                        LandmarkType.HALLWAY -> "Continue through hallway"
                        LandmarkType.ELEVATOR -> "Elevator ahead"
                        LandmarkType.CROSSWALK -> "Crosswalk ahead"
                        LandmarkType.STAIRS -> "Stairs ahead"
                        LandmarkType.ESCALATOR -> "Escalator ahead"
                    }
                }
            }
            instructions.add(stepText)
        }

        if (BuildConfig.DEBUG && validPath.isNotEmpty()) {
            val pathString = validPath.joinToString(" -> ") { it.landmarkType.name }
            Log.d("RoutePlanner", "PATH FOUND $pathString")
        }

        return RoutePlan(
            destination = destination,
            currentNode = currentNode,
            plannedPath = validPath,
            instructions = instructions,
            estimatedDistanceMeters = if (validPath.isNotEmpty()) totalDistance else null
        )
    }
}
