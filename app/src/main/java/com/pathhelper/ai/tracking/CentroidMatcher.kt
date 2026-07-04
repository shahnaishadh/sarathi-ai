package com.pathhelper.ai.tracking

import kotlin.math.sqrt
import com.pathhelper.ai.onnx.Detection
/**
* Coordinates Centroid Matcher operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Centroid Matcher.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class CentroidMatcher {
    private val matchDistanceThreshold = 80.0f

    fun match(
        detections: List<Detection>,
        tracks: List<Track>
    ): List<Pair<Detection, Track?>> {
        val matched = mutableListOf<Pair<Detection, Track?>>()
        val assignedTrackIds = mutableSetOf<Int>()

        for (det in detections) {
            var bestTrack: Track? = null
            var minDistance = Float.MAX_VALUE

            for (track in tracks) {
                if (track.classId != det.classId) continue
                if (assignedTrackIds.contains(track.id)) continue

                val dx = det.centerX - track.centerX
                val dy = det.centerY - track.centerY
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < matchDistanceThreshold && dist < minDistance) {
                    minDistance = dist
                    bestTrack = track
                }
            }

            if (bestTrack != null) {
                assignedTrackIds.add(bestTrack.id)
                matched.add(Pair(det, bestTrack))
            } else {
                matched.add(Pair(det, null))
            }
        }

        return matched
    }
}
