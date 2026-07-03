package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.navigation.graph.RouteGraph
/**
* Coordinates Pose Fusion Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Pose Fusion Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class PoseFusionEngine {

    companion
object {
        private const val ALPHA = 0.3f            // Blend weight for incoming data (30% new)
        private const val CONFIDENCE_THRESHOLD = 0.45f
    }

    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedConfidence = 0f
    private var frameCount = 0

    fun reset() {
        smoothedX = 0f
        smoothedY = 0f
        smoothedConfidence = 0f
        frameCount = 0
    }

    fun fuse(
        pose: PoseEstimate,
        observations: List<LandmarkObservation>,
        routeGraph: RouteGraph,
        localizationConfidence: LocalizationConfidence,
        timestamp: Long
    ): LocalizedPosition {
        frameCount++

        // Exponential smoothing on position
        smoothedX = if (frameCount == 1) pose.positionX
        else ALPHA * pose.positionX + (1f - ALPHA) * smoothedX

        smoothedY = if (frameCount == 1) pose.positionY
        else ALPHA * pose.positionY + (1f - ALPHA) * smoothedY

        smoothedConfidence = if (frameCount == 1) localizationConfidence.score
        else ALPHA * localizationConfidence.score + (1f - ALPHA) * smoothedConfidence

        // Determine localization state
        val state = when {
            frameCount == 1 -> LocalizationState.INITIALIZING
            smoothedConfidence < 0.15f -> LocalizationState.LOST
            smoothedConfidence < CONFIDENCE_THRESHOLD -> LocalizationState.SEARCHING
            else -> LocalizationState.LOCALIZED
        }

        // Resolve nearest landmark from sorted observations
        val best = observations.firstOrNull()
        val nearestLandmark = best?.label
        val landmarkDistance = best?.estimatedDistance ?: 0f
        val landmarkBearing = best?.bearing ?: pose.headingDegrees

        // Resolve current room from nearest route graph node (by LandmarkType label)
        val currentRoom = resolveRoom(routeGraph, observations)

        return LocalizedPosition(
            currentRoom = currentRoom,
            nearestLandmark = nearestLandmark,
            landmarkDistance = landmarkDistance,
            landmarkBearing = landmarkBearing,
            confidence = smoothedConfidence,
            state = state,
            timestamp = timestamp
        )
    }

    private fun resolveRoom(routeGraph: RouteGraph, observations: List<LandmarkObservation>): String? {
        if (routeGraph.nodes.isEmpty() || observations.isEmpty()) return null
        val bestObs = observations.first()
        // Find route graph node whose landmark type matches the best observation label
        val matchedNode = routeGraph.nodes.firstOrNull { node ->
            node.landmarkType.name.equals(bestObs.label, ignoreCase = true)
        }
        return matchedNode?.nodeId
    }
}
