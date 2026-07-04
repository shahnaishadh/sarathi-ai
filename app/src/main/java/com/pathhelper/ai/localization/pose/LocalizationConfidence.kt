/**
* Coordinates Localization Confidence operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Localization Confidence.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
package com.pathhelper.ai.localization.pose
/**
* Represents the data structures or state of Localization Confidence.
*/
data
class LocalizationConfidence(
    val poseConfidence: Float,
    val landmarkMatchScore: Float,
    val routeGraphMatchScore: Float,
    val sceneMemoryScore: Float,
    val score: Float
) {
    companion
object {
        fun compute(
            poseConfidence: Float,
            landmarkMatchScore: Float,
            routeGraphMatchScore: Float,
            sceneMemoryScore: Float
        ): LocalizationConfidence {
            val weighted = (poseConfidence * 0.2f) +
                    (landmarkMatchScore * 0.5f) +
                    (routeGraphMatchScore * 0.2f) +
                    (sceneMemoryScore * 0.1f)
            return LocalizationConfidence(
                poseConfidence = poseConfidence,
                landmarkMatchScore = landmarkMatchScore,
                routeGraphMatchScore = routeGraphMatchScore,
                sceneMemoryScore = sceneMemoryScore,
                score = weighted.coerceIn(0f, 1f)
            )
        }

        val ZERO = LocalizationConfidence(0f, 0f, 0f, 0f, 0f)
    }
}
