package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Obstacle Prioritization Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Obstacle Prioritization Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class ObstaclePrioritizationEngine {

    /**
     * Ranks and prioritizes obstacles based on distance, zone placement, and threat level.
     * Implements safety-critical filtering to prevent alerts on stale or ghost tracks.
     */
    fun process(
        tracks: List<Track>
    ): Pair<List<ThreatPriority>, ThreatMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        if (tracks.isEmpty()) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                ThreatMetadata(
                    processedTracks = 0,
                    rankedThreats = 0,
                    criticalThreats = 0,
                    highestPriorityScore = 0.0f,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        }

        try {
            var criticalThreatsCount = 0

            // 1. Calculate score, horizontal zone, and threat level for each track
            val scoredTracks = tracks
                .filter { it.missedFrames < 3 } // BUG FIX: Don't prioritize ghost tracks that have been missing for too long
                .map { track ->
                val riskWeight = when (track.riskLevel) {
                    TtcRiskLevel.CRITICAL -> 100f
                    TtcRiskLevel.WARNING -> 60f
                    TtcRiskLevel.CAUTION -> 25f
                    TtcRiskLevel.SAFE -> 0f
                }

                val distanceWeight = when {
                    track.distanceMeters < 1.0f -> 30f
                    track.distanceMeters < 2.0f -> 20f
                    track.distanceMeters < 4.0f -> 10f
                    else -> 0f
                }

                // Map HorizontalZone based on relative center position
                val normX = track.centerX / 640.0f
                val horizontalZone = when {
                    normX < 0.15f -> HorizontalZone.SHARP_LEFT
                    normX < 0.35f -> HorizontalZone.LEFT
                    normX < 0.65f -> HorizontalZone.CENTER
                    normX < 0.85f -> HorizontalZone.RIGHT
                    else -> HorizontalZone.SHARP_RIGHT
                }

                val positionWeight = when (horizontalZone) {
                    HorizontalZone.CENTER -> 30f
                    HorizontalZone.LEFT -> 10f
                    HorizontalZone.RIGHT -> 10f
                    HorizontalZone.SHARP_LEFT -> 5f
                    HorizontalZone.SHARP_RIGHT -> 5f
                }

                //val rawScore = riskWeight + distanceWeight + positionWeight
                // ---------------------------
// Object semantic weighting
// ---------------------------
                val objectWeight = when (track.classId) {

                    // PERSON
                    0 -> 40f

                    // Bicycle
                    1 -> 30f

                    // Motorcycle
                    3 -> 35f

                    // Chair
                    56 -> 30f

                    // Couch
                    57 -> 25f

                    // Potted Plant
                    58 -> 20f

                    // Bed
                    59 -> 25f

                    // Dining Table
                    60 -> 25f

                    // Toilet
                    61 -> 20f

                    // TV
                    62 -> 10f

                    // Laptop
                    63 -> 10f

                    // Bench
                    13 -> 20f

                    else -> 0f
                }
                val rawScore =
                    riskWeight +
                            distanceWeight +
                            positionWeight +
                            objectWeight
                val priorityScore = if (rawScore.isNaN() || rawScore.isInfinite()) 0.0f else rawScore

//                val threatLevel = when {
//                    priorityScore >= 130f -> {
//                        criticalThreatsCount++
//                        ThreatLevel.CRITICAL
//                    }
//                    priorityScore >= 90f -> ThreatLevel.HIGH
//                    priorityScore >= 50f -> ThreatLevel.MEDIUM
//                    else -> ThreatLevel.LOW
//                }

                var adjustedScore = priorityScore
                val isPerson = track.classId == 0
                val isCenter = horizontalZone == HorizontalZone.CENTER

                if (isPerson && isCenter) {
                    when {
                        track.distanceMeters < 1.5f -> {
                            adjustedScore += 80f
                        }

                        track.distanceMeters < 3.0f -> {
                            adjustedScore += 50f
                        }

                        track.distanceMeters < 5.0f -> {
                            adjustedScore += 25f
                        }
                    }
                } else if (isCenter) {
                    when {
                        track.distanceMeters < 1.5f -> {
                            adjustedScore += 50f
                        }

                        track.distanceMeters < 3.0f -> {
                            adjustedScore += 30f
                        }

                        track.distanceMeters < 5.0f -> {
                            adjustedScore += 15f
                        }
                    }
                }

                val finalScore = adjustedScore

                val threatLevel = when {
                    finalScore >= 120f -> {
                        criticalThreatsCount++
                        ThreatLevel.CRITICAL
                    }

                    finalScore >= 80f -> ThreatLevel.HIGH

                    finalScore >= 50f -> ThreatLevel.MEDIUM

                    else -> ThreatLevel.LOW
                }

                if (track.classId == 0) { // PERSON
                    Log.d("PERSON_WARNING", 
                        "className=PERSON confidence=${track.confidence} distance=${track.distanceMeters} " +
                        "trackId=${track.id} trackAge=${track.age} source=ObstaclePrioritizationEngine")
                }

                Log.d(
                    "THREAT_SCORE",
                    "id=${track.id} " +
                            "class=${track.classId} " +
                            "distance=${track.distanceMeters} " +
                            "risk=${track.riskLevel} " +
                            "score=$finalScore " +
                            "threat=${threatLevel.name}"
                )

                if (BuildConfig.DEBUG) {
                    Log.d(
                        "THREAT_DEBUG",
                        "id=${track.id} class=${track.classId} dist=${track.distanceMeters} score=$finalScore threat=${threatLevel.name}"
                    )
                }

                TrackScoringData(
                    track = track,
                    horizontalZone = horizontalZone,
                    priorityScore = finalScore,
                    threatLevel = threatLevel
                )
            }

            // 2. Sort scored tracks descending by priorityScore
            val sortedScored = scoredTracks.sortedByDescending { it.priorityScore }

            // 3. Assign ranks and populate return structures
            val threatPriorities = sortedScored.mapIndexed { index, data ->
                val rank = index + 1

                // Mutate original track attributes
                data.track.priorityScore = data.priorityScore
                data.track.threatLevel = data.threatLevel
                data.track.threatRank = rank

                // Debug logging (debug builds only)
                if (BuildConfig.DEBUG) {
                    Log.d("Prioritization", "Track #${data.track.id}")
                    Log.d("Prioritization", "  Risk = ${data.track.riskLevel.name}")
                    Log.d("Prioritization", "  Distance = ${data.track.distanceMeters}m")
                    Log.d("Prioritization", "  Position = ${data.horizontalZone.name}")
                    Log.d("Prioritization", "  Score = ${data.priorityScore}")
                    Log.d("Prioritization", "  Rank = $rank")
                    Log.d("Prioritization", "  Threat = ${data.threatLevel.name}")
                }

                ThreatPriority(
                    trackId = data.track.id,
                    classId = data.track.classId,
                    className = SemanticLabels.getClassName(data.track.classId),
                    confidence = data.track.confidence,
                    horizontalZone = data.horizontalZone,
                    distanceMeters = data.track.distanceMeters,
                    ttcSeconds = data.track.ttcSeconds,
                    riskLevel = data.track.riskLevel,
                    priorityScore = data.priorityScore,
                    threatLevel = data.threatLevel,
                    rank = rank
                )
            }

            val highestScore = threatPriorities.firstOrNull()?.priorityScore ?: 0.0f
            val duration = SystemClock.elapsedRealtime() - startTime

            return Pair(
                threatPriorities,
                ThreatMetadata(
                    processedTracks = tracks.size,
                    rankedThreats = threatPriorities.size,
                    criticalThreats = criticalThreatsCount,
                    highestPriorityScore = highestScore,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                ThreatMetadata(
                    processedTracks = tracks.size,
                    rankedThreats = 0,
                    criticalThreats = 0,
                    highestPriorityScore = 0.0f,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown obstacle prioritization processing error."
                )
            )
        }
    }

    private
/**
* Represents the data structures or state of Track Scoring Data.
*/
data class TrackScoringData(
        val track: Track,
        val horizontalZone: HorizontalZone,
        val priorityScore: Float,
        val threatLevel: ThreatLevel
    )
}
