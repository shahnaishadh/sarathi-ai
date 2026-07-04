package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.world.WorldModel
import kotlin.math.atan2
import kotlin.math.sqrt
/**
* Coordinates Landmark Matcher operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Landmark Matcher.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class LandmarkMatcher {

    fun match(
        pose: PoseEstimate,
        sceneMemory: SceneMemory,
        worldModel: WorldModel
    ): List<LandmarkObservation> {
        val results = mutableListOf<LandmarkObservation>()
        val now = System.currentTimeMillis()

        // Count active landmark-related observations in scene memory for evidence scoring
        val sceneMemoryLandmarkCount = sceneMemory.observations
            .filter { it.description.contains("door", ignoreCase = true) ||
                      it.description.contains("elevator", ignoreCase = true) ||
                      it.description.contains("stairs", ignoreCase = true) }
            .size
        val sceneMemoryEvidenceScore = minOf(1.0f, sceneMemoryLandmarkCount * 0.25f)

        for (landmark in worldModel.landmarks) {
            // Use landmark distanceMeters if available, else proxy from SLAM position
            val estimatedDistance = landmark.distanceMeters
                ?: sqrt(pose.positionX * pose.positionX + pose.positionY * pose.positionY)
                    .coerceAtLeast(0.5f)

            // Weighted confidence score
            val typeMatchScore = 0.5f
            val sceneBonus = sceneMemoryEvidenceScore * 0.3f
            val poseBonus = pose.confidence * 0.2f
            val confidence = (typeMatchScore + sceneBonus + poseBonus).coerceIn(0f, 1f)

            // Bearing from current pose direction toward this landmark
            val bearing = if (pose.positionX == 0f && pose.positionY == 0f) 0f
            else (atan2(pose.positionY.toDouble(), pose.positionX.toDouble()) * 180.0 / Math.PI).toFloat()

            results.add(
                LandmarkObservation(
                    landmarkId = landmark.id,
                    label = landmark.type.name,
                    estimatedDistance = estimatedDistance,
                    bearing = bearing,
                    confidence = confidence,
                    timestamp = now
                )
            )
        }

        // Return sorted by confidence descending, then closest distance
        return results.sortedWith(
            compareByDescending<LandmarkObservation> { it.confidence }
                .thenBy { it.estimatedDistance }
        )
    }
}
