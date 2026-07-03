package com.pathhelper.ai.navigation.hybrid.entrance

import android.os.SystemClock
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.tracking.Track
import com.pathhelper.ai.environment.EnvironmentObservation
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.LandmarkType
import kotlin.math.abs
/**
* Coordinates Building Entrance Detector operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Building Entrance Detector.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class BuildingEntranceDetector {
    private val analytics = EntranceAnalytics()

    fun getAnalytics(): EntranceAnalytics = analytics

    fun evaluate(
        tracks: List<Track>,
        sceneMemory: SceneMemory,
        worldModel: WorldModel,
        gpsDistance: Float,
        historicalSuccessScore: Float = 1.0f
    ): Pair<EntranceDecision, Pair<EntranceConfidence, EntranceMetadata>> {
        val startTime = SystemClock.elapsedRealtime()

        val candidates = mutableListOf<EntranceCandidate>()

        val gpsProximityScore = (1.0f - (gpsDistance / 15.0f)).coerceIn(0.0f, 1.0f)

        for (track in tracks) {
            val normX = track.centerX / 640.0f
            val zone = when {
                normX < 0.3333f -> HorizontalZone.LEFT
                normX < 0.6666f -> HorizontalZone.CENTER
                else -> HorizontalZone.RIGHT
            }
            val aspectVertical = if (track.width > 0) track.height / track.width else 0.0f

            if (aspectVertical >= 1.4f && zone == HorizontalZone.CENTER && track.distanceMeters < 5.0f) {
                val positionScore = 1.0f - abs(track.centerX - 320f) / 320f
                candidates.add(
                    EntranceCandidate(
                        trackId = track.id,
                        distanceEstimateMeters = track.distanceMeters,
                        detectionConfidence = track.confidence,
                        aspectRatio = aspectVertical,
                        positionScore = positionScore,
                        gpsProximityScore = gpsProximityScore
                    )
                )
            }
        }

        val bestCandidate = candidates.maxByOrNull { it.detectionConfidence }
        val doorConfidence = bestCandidate?.detectionConfidence ?: 0.0f

        val doorMemoryCount = sceneMemory.observations.count {
            it.type == EnvironmentType.DOOR || it.type == EnvironmentType.ENTRANCE
        }
        val sceneMemoryEvidence = (doorMemoryCount * 0.25f).coerceAtMost(1.0f)

        val hasWorldLandmark = worldModel.landmarks.any {
            it.type == LandmarkType.DOOR || it.type == LandmarkType.ELEVATOR
        }
        val worldModelEvidence = if (hasWorldLandmark) 1.0f else 0.0f

        val score = (doorConfidence * 0.4f) +
                (gpsProximityScore * 0.3f) +
                (sceneMemoryEvidence * 0.15f) +
                (worldModelEvidence * 0.15f)

        val decision = when {
            score >= 0.70f -> EntranceDecision.ENTRANCE_CONFIRMED
            score < 0.25f -> EntranceDecision.ENTRANCE_REJECTED
            else -> EntranceDecision.MORE_EVIDENCE_REQUIRED
        }

        analytics.logEvaluation(candidates.size, score, decision)

        val confidence = EntranceConfidence(
            score = score,
            doorConfidence = doorConfidence,
            gpsProximityScore = gpsProximityScore,
            sceneMemoryEvidence = sceneMemoryEvidence,
            worldModelEvidence = worldModelEvidence,
            historicalSuccessScore = historicalSuccessScore
        )

        val meta = EntranceMetadata(
            processingTimeMs = SystemClock.elapsedRealtime() - startTime,
            candidateCount = candidates.size,
            bestConfidenceScore = score,
            successful = true
        )

        return Pair(decision, Pair(confidence, meta))
    }
}
