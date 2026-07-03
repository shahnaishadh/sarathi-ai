package com.pathhelper.ai.environment

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Environment Understanding Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Environment Understanding Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class EnvironmentUnderstandingEngine {

    fun process(
        tracks: List<Track>
    ): Pair<List<EnvironmentObservation>, EnvironmentMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        val observations = mutableListOf<EnvironmentObservation>()

        try {
            var doorDetected = false
            var doorDistance = 0.0f
            var doorZone = HorizontalZone.CENTER
            var doorTrackId: Int? = null

            // 1. Evaluate Door, Stairs, Crosswalk heuristics per track
            for (track in tracks) {
                val normX = track.centerX / 640.0f
                val zone = when {
                    normX < 0.3333f -> HorizontalZone.LEFT
                    normX < 0.6666f -> HorizontalZone.CENTER
                    else -> HorizontalZone.RIGHT
                }

                val aspectVertical = if (track.width > 0) track.height / track.width else 0.0f
                val aspectHorizontal = if (track.height > 0) track.width / track.height else 0.0f
                val area = track.width * track.height

                // A. Door heuristic
                if (aspectVertical >= 1.4f && zone == HorizontalZone.CENTER && track.distanceMeters < 5.0f) {
                    doorDetected = true
                    doorDistance = track.distanceMeters
                    doorZone = zone
                    doorTrackId = track.id
                }

                // B. Stairs heuristic
                if (aspectHorizontal >= 1.4f && track.centerY > 320f && area >= 25000f) {
                    observations.add(
                        EnvironmentObservation(
                            type = EnvironmentType.STAIRS,
                            confidence = 0.75f,
                            horizontalZone = zone,
                            distanceMeters = track.distanceMeters,
                            description = "Stairs detected",
                            trackId = track.id
                        )
                    )
                }

                // C. Crosswalk heuristic
                if (aspectHorizontal >= 2.2f && track.centerY > 400f) {
                    observations.add(
                        EnvironmentObservation(
                            type = EnvironmentType.CROSSWALK,
                            confidence = 0.70f,
                            horizontalZone = zone,
                            distanceMeters = track.distanceMeters,
                            description = "Crosswalk ahead",
                            trackId = track.id
                        )
                    )
                }
            }

            // 2. Evaluate Hallway and Entrance heuristics
            val centerTracks = tracks.filter { (it.centerX / 640.0f) in 0.3333f..0.6666f }
            val isCenterSafe = centerTracks.isEmpty() || centerTracks.all { it.distanceMeters > 6.0f }
            val lowDensity = tracks.size <= 1

            if (doorDetected) {
                if (isCenterSafe) {
                    observations.add(
                        EnvironmentObservation(
                            type = EnvironmentType.ENTRANCE,
                            confidence = 0.90f,
                            horizontalZone = doorZone,
                            distanceMeters = doorDistance,
                            description = "Entrance detected ahead",
                            trackId = doorTrackId
                        )
                    )
                } else {
                    observations.add(
                        EnvironmentObservation(
                            type = EnvironmentType.DOOR,
                            confidence = 0.82f,
                            horizontalZone = doorZone,
                            distanceMeters = doorDistance,
                            description = "Door detected ahead",
                            trackId = doorTrackId
                        )
                    )
                }
            }

            if (lowDensity && isCenterSafe) {
                observations.add(
                    EnvironmentObservation(
                        type = EnvironmentType.HALLWAY,
                        confidence = 0.85f,
                        horizontalZone = HorizontalZone.CENTER,
                        distanceMeters = null,
                        description = "Hallway clear",
                        trackId = null
                    )
                )
            }

            // If no structural semantic clues were resolved, add UNKNOWN fallback if there are tracks
            if (observations.isEmpty() && tracks.isNotEmpty()) {
                val topTrack = tracks.maxByOrNull { it.confidence }
                val topNormX = (topTrack?.centerX ?: 320f) / 640.0f
                val topZone = when {
                    topNormX < 0.3333f -> HorizontalZone.LEFT
                    topNormX < 0.6666f -> HorizontalZone.CENTER
                    else -> HorizontalZone.RIGHT
                }
                observations.add(
                    EnvironmentObservation(
                        type = EnvironmentType.UNKNOWN,
                        confidence = 0.50f,
                        horizontalZone = topZone,
                        distanceMeters = topTrack?.distanceMeters,
                        description = "Unknown structure",
                        trackId = topTrack?.id
                    )
                )
            }

            // Debug logging
            if (BuildConfig.DEBUG && observations.isNotEmpty()) {
                val topObs = observations.maxByOrNull { it.confidence }
                topObs?.let {
                    Log.d("EnvironmentEngine", "Detected: ${it.type.name} Confidence: ${it.confidence} Distance: ${it.distanceMeters}m")
                }
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                observations,
                EnvironmentMetadata(
                    observations = observations.size,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                EnvironmentMetadata(
                    observations = 0,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown environment understanding logic error."
                )
            )
        }
    }
}
