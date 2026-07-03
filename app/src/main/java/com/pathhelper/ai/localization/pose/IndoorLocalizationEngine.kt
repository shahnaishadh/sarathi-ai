package com.pathhelper.ai.localization.pose

import android.os.SystemClock
import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.graph.RouteGraph
import com.pathhelper.ai.world.WorldModel
/**
* Coordinates Indoor Localization Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Indoor Localization Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class IndoorLocalizationEngine {

    private val landmarkMatcher = LandmarkMatcher()
    private val fusionEngine = PoseFusionEngine()

    fun reset() {
        fusionEngine.reset()
    }

    fun process(
        pose: PoseEstimate,
        sceneMemory: SceneMemory,
        worldModel: WorldModel,
        routeGraph: RouteGraph
    ): Triple<LocalizedPosition, LocalizationConfidence, LocalizationMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        return try {
            // Step 1: Match scene memory + world model observations to landmark candidates
            val observations = landmarkMatcher.match(pose, sceneMemory, worldModel)

            // Step 2: Compute route graph proximity score
            val routeGraphMatchScore = if (routeGraph.nodes.isNotEmpty()) {
                minOf(1.0f, routeGraph.nodes.size * 0.1f)
            } else 0f

            // Step 3: Scene memory landmark evidence score
            val sceneMemoryScore = minOf(
                1.0f,
                sceneMemory.observations.size * 0.1f
            )

            // Step 4: Top landmark match score
            val landmarkMatchScore = observations.firstOrNull()?.confidence ?: 0f

            // Step 5: Build confidence model
            val confidence = LocalizationConfidence.compute(
                poseConfidence = pose.confidence,
                landmarkMatchScore = landmarkMatchScore,
                routeGraphMatchScore = routeGraphMatchScore,
                sceneMemoryScore = sceneMemoryScore
            )

            // Step 6: Fuse into stable LocalizedPosition
            val position = fusionEngine.fuse(
                pose = pose,
                observations = observations,
                routeGraph = routeGraph,
                localizationConfidence = confidence,
                timestamp = pose.timestamp
            )

            val processingTimeMs = SystemClock.elapsedRealtime() - startTime
            Triple(
                position,
                confidence,
                LocalizationMetadata(
                    candidateCount = worldModel.landmarks.size,
                    matchedLandmarks = observations.size,
                    processingTimeMs = processingTimeMs,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val fallbackPosition = LocalizedPosition(
                currentRoom = null,
                nearestLandmark = null,
                landmarkDistance = 0f,
                landmarkBearing = 0f,
                confidence = 0f,
                state = LocalizationState.LOST,
                timestamp = pose.timestamp
            )
            Triple(
                fallbackPosition,
                LocalizationConfidence.ZERO,
                LocalizationMetadata(
                    candidateCount = 0,
                    matchedLandmarks = 0,
                    processingTimeMs = SystemClock.elapsedRealtime() - startTime,
                    successful = false,
                    errorMessage = e.localizedMessage
                )
            )
        }
    }
}
