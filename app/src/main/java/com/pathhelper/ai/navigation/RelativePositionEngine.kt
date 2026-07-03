package com.pathhelper.ai.navigation

import android.os.SystemClock
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Relative Position Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Relative Position Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class RelativePositionEngine {

    fun process(
        tracks: List<Track>,
        screenWidth: Float,
        screenHeight: Float
    ): Pair<List<RelativePosition>, RelativePositionMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        try {
            if (screenWidth <= 0f || screenHeight <= 0f) {
                return Pair(
                    emptyList(),
                    RelativePositionMetadata(
                        trackingObjects = tracks.size,
                        processedObjects = 0,
                        processingTimeMs = SystemClock.elapsedRealtime() - startTime,
                        successful = false,
                        errorMessage = "Invalid screen dimensions: ${screenWidth}x${screenHeight}"
                    )
                )
            }

            if (tracks.isEmpty()) {
                return Pair(
                    emptyList(),
                    RelativePositionMetadata(
                        trackingObjects = 0,
                        processedObjects = 0,
                        processingTimeMs = SystemClock.elapsedRealtime() - startTime,
                        successful = true,
                        errorMessage = null
                    )
                )
            }

            val results = mutableListOf<RelativePosition>()

            for (track in tracks) {
                // Handle NaN and negative coordinate checks
                if (track.centerX.isNaN() || track.centerY.isNaN() || track.centerX < 0f || track.centerY < 0f) {
                    continue
                }

                // Horizontal zoning classification
                val normX = track.centerX / screenWidth
                val horizontalZone = when {
                    normX < 0.3333f -> HorizontalZone.LEFT
                    normX < 0.6666f -> HorizontalZone.CENTER
                    else -> HorizontalZone.RIGHT
                }

                // Vertical zoning classification
                val normY = track.centerY / screenHeight
                val verticalZone = when {
                    normY < 0.3333f -> VerticalZone.TOP
                    normY < 0.6666f -> VerticalZone.MIDDLE
                    else -> VerticalZone.BOTTOM
                }

                results.add(
                    RelativePosition(
                        trackId = track.id,
                        classId = track.classId,
                        horizontalZone = horizontalZone,
                        verticalZone = verticalZone,
                        centerX = track.centerX,
                        centerY = track.centerY
                    )
                )
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                results,
                RelativePositionMetadata(
                    trackingObjects = tracks.size,
                    processedObjects = results.size,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                RelativePositionMetadata(
                    trackingObjects = tracks.size,
                    processedObjects = 0,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown relative position classification error."
                )
            )
        }
    }
}
