package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Distance Estimation Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Distance Estimation Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class DistanceEstimationEngine {

    private val OBJECT_HEIGHTS = mapOf(
        0 to 1.70f,  // PERSON
        1 to 1.10f,  // BICYCLE
        3 to 1.20f,  // MOTORCYCLE
        56 to 0.85f, // CHAIR
        57 to 0.80f, // COUCH
        60 to 0.75f, // DINING TABLE
        13 to 0.80f, // BENCH
        2 to 1.50f   // CAR
    )

    private val DEFAULT_HEIGHT = 1.2f

    fun estimate(
        relativePositions: List<RelativePosition>,
        tracks: List<Track>,
        deltaTimeSeconds: Float
    ): Pair<List<DistanceEstimate>, DistanceMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        if (tracks.isEmpty() || relativePositions.isEmpty()) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                DistanceMetadata(
                    processedTracks = tracks.size,
                    estimatedTracks = 0,
                    averageDistanceMeters = 0.0f,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        }

        val estimates = mutableListOf<DistanceEstimate>()
        var sumDistance = 0.0f
        var countDistance = 0

        try {
            for (pos in relativePositions) {
                val track = tracks.find { it.id == pos.trackId } ?: continue
                val hReal = OBJECT_HEIGHTS[pos.classId] ?: DEFAULT_HEIGHT

                // Prevent division by zero
                val boxHeight = if (track.height > 0f) track.height else 1.0f

                /**
                 * CALIBRATION FIX: 
                 * The previous formula was: (FOCAL_LENGTH_PIXELS * letterboxScale * hReal) / boxHeight
                 * Since boxHeight is already in the 640x640 model space, FOCAL_LENGTH should 
                 * correspond to that same space. Multiplying by letterboxScale again was 
                 * causing a double-scaling error.
                 *
                 * Additionally, FOCAL_LENGTH_PIXELS is adjusted to 210f to account for 
                 * the wide-angle lenses typical in modern mobile devices.
                 */
                var distance = (FOCAL_LENGTH_PIXELS * hReal) / boxHeight

                // Clamp distance estimates between MIN and MAX constants
                if (distance < MIN_DISTANCE_METERS) {
                    distance = MIN_DISTANCE_METERS
                } else if (distance > MAX_DISTANCE_METERS) {
                    distance = MAX_DISTANCE_METERS
                }

                // Update track's distance history with EMA smoothing (tuned alpha for stability)
                val prevDistance = track.distanceMeters
                val smoothedDistance = if (prevDistance > 0.0f && track.age > 1) {
                    0.25f * distance + 0.75f * prevDistance
                } else {
                    distance
                }
                track.previousDistanceMeters = prevDistance
                track.distanceMeters = smoothedDistance

                // Calculate distance velocity: closing velocity (negative means getting closer)
                val dt = if (deltaTimeSeconds > 0.0001f) deltaTimeSeconds else 0.0333f
                val rawVelocity = (smoothedDistance - prevDistance) / dt

                // Protect velocity against divide by zero, NaN, Infinity
                track.distanceVelocityMetersPerSecond = when {
                    rawVelocity.isNaN() -> 0.0f
                    rawVelocity.isInfinite() -> 0.0f
                    else -> rawVelocity
                }

                // Validation debug logging (debug builds only)
                if (BuildConfig.DEBUG) {
                    Log.d("DistanceHardening", "Track #${track.id} Distance = ${track.distanceMeters}m")
                    Log.d("DistanceHardening", "Track #${track.id} Previous Distance = ${track.previousDistanceMeters}m")
                    Log.d("DistanceHardening", "Track #${track.id} Velocity = ${track.distanceVelocityMetersPerSecond} m/s")
                }

                estimates.add(
                    DistanceEstimate(
                        trackId = pos.trackId,
                        classId = pos.classId,
                        distanceMeters = smoothedDistance,
                        horizontalZone = pos.horizontalZone,
                        verticalZone = pos.verticalZone
                    )
                )

                sumDistance += smoothedDistance
                countDistance++
            }

            val avgDistance = if (countDistance > 0) sumDistance / countDistance else 0.0f
            val duration = SystemClock.elapsedRealtime() - startTime

            return Pair(
                estimates,
                DistanceMetadata(
                    processedTracks = tracks.size,
                    estimatedTracks = estimates.size,
                    averageDistanceMeters = avgDistance,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                DistanceMetadata(
                    processedTracks = tracks.size,
                    estimatedTracks = 0,
                    averageDistanceMeters = 0.0f,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown distance hardening processing error."
                )
            )
        }
    }

    private companion
object {
        const val FOCAL_LENGTH_PIXELS = 210f
        const val MIN_DISTANCE_METERS = 0.3f
        const val MAX_DISTANCE_METERS = 20f
    }
}
