package com.pathhelper.ai.tracking

import android.os.SystemClock
import com.pathhelper.ai.onnx.Detection
/**
* Coordinates Track Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Track Manager.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class TrackManager {
    private var nextTrackId = 1
    private val activeTracks = mutableListOf<Track>()
    private val kalmanFilters = mutableMapOf<Int, KalmanFilter2D>()
    private val matcher = CentroidMatcher()

    fun update(detections: List<Detection>, deltaTime: Float): Pair<List<Track>, TrackMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        var newTracksCreated = 0
        var removedTracks = 0

        try {
            // 1. Prediction step: Run prediction on all active tracks
            for (track in activeTracks) {
                kalmanFilters[track.id]?.predict(deltaTime)
            }

            // Update active track coordinates with prediction models
            val predictedTracks = activeTracks.map { track ->
                val kf = kalmanFilters[track.id]
                if (kf != null) {
                    track.copy(
                        centerX = kf.getX(),
                        centerY = kf.getY(),
                        velocityX = kf.getVx(),
                        velocityY = kf.getVy()
                    )
                } else {
                    track
                }
            }

            // 2. Association step: Match current detections with predicted tracks
            val matches = matcher.match(detections, predictedTracks)

            // Keep track of which track IDs were updated in this frame
            val matchedTrackIds = mutableSetOf<Int>()
            val updatedTracks = mutableListOf<Track>()

            for ((det, matchedTrack) in matches) {
                if (matchedTrack != null) {
                    matchedTrackIds.add(matchedTrack.id)

                    // Correct step: Update Kalman filter with new measurements
                    val kf = kalmanFilters[matchedTrack.id]
                    if (kf != null) {
                        kf.update(det.centerX, det.centerY)
                        val prevX = matchedTrack.centerX
                        val prevY = matchedTrack.centerY
                        
                        // Calculate instantaneous velocities
                        val vx = kf.getX() - prevX
                        val vy = kf.getY() - prevY

                        val updatedTrack = Track(
                            id = matchedTrack.id,
                            classId = det.classId,
                            centerX = kf.getX(),
                            centerY = kf.getY(),
                            velocityX = vx,
                            velocityY = vy,
                            age = matchedTrack.age + 1,
                            missedFrames = 0,
                            confidence = det.confidence,
                            width = det.width,
                            height = det.height
                        )

                        if (det.classId == 0) { // PERSON
                            android.util.Log.d("PERSON_WARNING", 
                                "className=PERSON confidence=${det.confidence} trackId=${updatedTrack.id} " +
                                "trackAge=${updatedTrack.age} source=TrackManager_Matched")
                        }
                        
                        updatedTracks.add(updatedTrack)
                    }
                } else {
                    // Track Creation: No match found
                    val newId = nextTrackId++
                    val kf = KalmanFilter2D(det.centerX, det.centerY)
                    kalmanFilters[newId] = kf

                    val newTrack = Track(
                        id = newId,
                        classId = det.classId,
                        centerX = det.centerX,
                        centerY = det.centerY,
                        velocityX = 0.0f,
                        velocityY = 0.0f,
                        age = 1,
                        missedFrames = 0,
                        confidence = det.confidence,
                        width = det.width,
                        height = det.height
                    )

                    if (det.classId == 0) { // PERSON
                        android.util.Log.d("PERSON_WARNING", 
                            "className=PERSON confidence=${det.confidence} trackId=${newTrack.id} " +
                            "trackAge=${newTrack.age} source=TrackManager_New")
                    }

                    updatedTracks.add(newTrack)
                    newTracksCreated++
                }
            }

            // 3. Update missed frames count for tracks that were NOT matched
            val unmatchedTracks = predictedTracks.filter { !matchedTrackIds.contains(it.id) }
            for (track in unmatchedTracks) {
                val updatedMissed = track.missedFrames + 1
                if (updatedMissed <= 10) {
                    // Keep the track but increment missedFrames
                    val updatedTrack = track.copy(
                        missedFrames = updatedMissed,
                        age = track.age + 1
                    )

                    if (updatedTrack.classId == 0) { // PERSON
                        android.util.Log.d("PERSON_WARNING", 
                            "className=PERSON confidence=${updatedTrack.confidence} trackId=${updatedTrack.id} " +
                            "trackAge=${updatedTrack.age} source=TrackManager_Unmatched_MissedCount=$updatedMissed")
                    }

                    updatedTracks.add(updatedTrack)
                } else {
                    // Track Deletion: missedFrames > 10
                    kalmanFilters.remove(track.id)
                    removedTracks++
                }
            }

            // Update the manager's active tracks list
            activeTracks.clear()
            activeTracks.addAll(updatedTracks)

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                activeTracks.toList(),
                TrackMetadata(
                    activeTracks = activeTracks.size,
                    newTracksCreated = newTracksCreated,
                    removedTracks = removedTracks,
                    processingTimeMs = duration,
                    trackingSuccessful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                TrackMetadata(
                    activeTracks = activeTracks.size,
                    newTracksCreated = newTracksCreated,
                    removedTracks = removedTracks,
                    processingTimeMs = duration,
                    trackingSuccessful = false,
                    errorMessage = e.localizedMessage ?: "Unknown tracking error."
                )
            )
        }
    }
}
