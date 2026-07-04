package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.perception.SegmentationResult
import com.pathhelper.ai.tracking.Track
import kotlin.math.max
import kotlin.math.min

/**
 * Evaluates the occupancy and safety levels of local spatial corridors.
 *
 * Explain:
 * * Purpose of the component: Analyzes tracked obstacle positions and distances to score navigation corridors.
 * * Role within the Sarthi architecture: Serves as the local obstacle-avoidance engine to guide safe stepping paths.
 * * Major inputs and outputs: Inputs a list of [Track] objects representing detected obstacles; outputs a list of [SafeCorridor]s with safety states and metadata.
 */
class SafeCorridorEngine {

    /**
     * Analyzes track bounding boxes and distances to determine zone blockage.
     * Calculates occupancy ratios and safety threat levels to prioritize path corridors.
     */
    fun process(tracks: List<Track>): Pair<List<SafeCorridor>, CorridorMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        
        // Initialize scores for 5 zones
        val zoneScores = mutableMapOf(
            HorizontalZone.SHARP_LEFT to 100f,
            HorizontalZone.LEFT to 100f,
            HorizontalZone.CENTER to 100f,
            HorizontalZone.RIGHT to 100f,
            HorizontalZone.SHARP_RIGHT to 100f
        )
        
        val zoneHighestThreat = mutableMapOf(
            HorizontalZone.SHARP_LEFT to ThreatLevel.LOW,
            HorizontalZone.LEFT to ThreatLevel.LOW,
            HorizontalZone.CENTER to ThreatLevel.LOW,
            HorizontalZone.RIGHT to ThreatLevel.LOW,
            HorizontalZone.SHARP_RIGHT to ThreatLevel.LOW
        )

        for (track in tracks) {
            // Normalize track coordinates to 0..1 range (YOLO input is 640x640)
            val left = (track.centerX - (track.width / 2)) / 640f
            val right = (track.centerX + (track.width / 2)) / 640f
            val dist = track.distanceMeters
            
            // Define 5 Zone Boundaries
            val zones = listOf(
                Triple(HorizontalZone.SHARP_LEFT, 0.0f, 0.15f),
                Triple(HorizontalZone.LEFT, 0.15f, 0.35f),
                Triple(HorizontalZone.CENTER, 0.35f, 0.65f),
                Triple(HorizontalZone.RIGHT, 0.65f, 0.85f),
                Triple(HorizontalZone.SHARP_RIGHT, 0.85f, 1.0f)
            )

            for ((zone, zStart, zEnd) in zones) {
                // Calculate horizontal overlap between obstacle and zone
                val overlapStart = max(left, zStart)
                val overlapEnd = min(right, zEnd)
                val overlapWidth = max(0f, overlapEnd - overlapStart)
                val zoneWidth = zEnd - zStart
                val occupancyRatio = overlapWidth / zoneWidth

                if (occupancyRatio > 0.1f) { // If obstacle occupies > 10% of the zone width
                    // Calculate Penalty based on distance and occupancy
                    val distFactor = when {
                        dist < 1.0f -> 2.5f  // Massive penalty for extremely close obstacles
                        dist < 1.5f -> 1.8f  // Increased from 1.0f
                        dist < 3.0f -> 0.9f  // Increased from 0.6f
                        dist < 5.0f -> 0.4f  // Increased from 0.3f
                        else -> 0.15f        // Increased from 0.1f
                    }
                    
                    val penalty = 100f * occupancyRatio * distFactor
                    zoneScores[zone] = max(0f, zoneScores[zone]!! - penalty)
                    
                    // Update highest threat level for the zone
                    if (track.threatLevel.ordinal > zoneHighestThreat[zone]!!.ordinal) {
                        zoneHighestThreat[zone] = track.threatLevel
                    }
                }
            }
        }

        // 2. Override scores for zones where a safe traversable gap exists between obstacles
        val zones = listOf(
            Triple(HorizontalZone.SHARP_LEFT, 0.0f, 0.15f),
            Triple(HorizontalZone.LEFT, 0.15f, 0.35f),
            Triple(HorizontalZone.CENTER, 0.35f, 0.65f),
            Triple(HorizontalZone.RIGHT, 0.65f, 0.85f),
            Triple(HorizontalZone.SHARP_RIGHT, 0.85f, 1.0f)
        )

        for ((zone, zStart, zEnd) in zones) {
            // Find all close obstacles inside or overlapping this zone
            val closeTracksInZone = tracks.filter { track ->
                val trackLeft = (track.centerX - (track.width / 2)) / 640f
                val trackRight = (track.centerX + (track.width / 2)) / 640f
                track.distanceMeters < 3.5f && max(trackLeft, zStart) < min(trackRight, zEnd)
            }

            var maxGapWidth = zEnd - zStart
            if (closeTracksInZone.isNotEmpty()) {
                val intervals = closeTracksInZone.map { track ->
                    val trackLeft = (track.centerX - (track.width / 2)) / 640f
                    val trackRight = (track.centerX + (track.width / 2)) / 640f
                    Pair(max(trackLeft, zStart), min(trackRight, zEnd))
                }.sortedBy { it.first }

                val merged = mutableListOf<Pair<Float, Float>>()
                for (interval in intervals) {
                    if (merged.isEmpty() || merged.last().second < interval.first) {
                        merged.add(interval)
                    } else {
                        val last = merged.removeAt(merged.size - 1)
                        merged.add(Pair(last.first, max(last.second, interval.second)))
                    }
                }

                var currentStart = zStart
                maxGapWidth = 0f
                for (blocked in merged) {
                    if (blocked.first > currentStart) {
                        val gap = blocked.first - currentStart
                        if (gap > maxGapWidth) {
                            maxGapWidth = gap
                        }
                    }
                    currentStart = max(currentStart, blocked.second)
                }
                if (zEnd > currentStart) {
                    val gap = zEnd - currentStart
                    if (gap > maxGapWidth) {
                        maxGapWidth = gap
                    }
                }

                val nearestDist = closeTracksInZone.minOf { it.distanceMeters }
                // If there's a continuous gap of at least 12% screen width, and the nearest obstacle is >= 1.0m away:
                if (maxGapWidth >= 0.12f && nearestDist >= 1.0f) {
                    // Mark as safe by setting score to at least 70f
                    zoneScores[zone] = max(70f, zoneScores[zone] ?: 0f)
                }
            }
            Log.i("SARTHI_CORRIDOR", "gapWidth=$maxGapWidth zoneScore=${zoneScores[zone]} selectedPath=$zone")
        }

        val corridors = zoneScores.map { (zone, score) ->
            val state = when {
                score >= 70f -> CorridorState.SAFE // Relaxed from 80f
                score >= 30f -> CorridorState.CAUTION // Relaxed from 40f
                else -> CorridorState.BLOCKED
            }
            SafeCorridor(
                horizontalZone = zone,
                state = state,
                threatCount = tracks.count { it.assignedCorridor == zone },
                highestThreatLevel = zoneHighestThreat[zone]!!,
                averageDistanceMeters = 2.0f,
                score = score
            )
        }

        val duration = SystemClock.elapsedRealtime() - startTime
        return Pair(corridors, CorridorMetadata(tracks.size, corridors.count { it.state == CorridorState.SAFE }, corridors.count { it.state == CorridorState.BLOCKED }, duration, true))
    }

    // Structural awareness placeholder (integration with Segmentation)
    fun processFromSegmentation(seg: SegmentationResult): Pair<List<SafeCorridor>, CorridorMetadata> {
        return Pair(emptyList(), CorridorMetadata(0, 0, 0, 0, true))
    }
}
