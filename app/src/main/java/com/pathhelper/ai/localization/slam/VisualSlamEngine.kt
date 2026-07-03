package com.pathhelper.ai.localization.slam

import android.graphics.Bitmap
import android.os.SystemClock
/**
* Coordinates Visual Slam Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Visual Slam Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class VisualSlamEngine {
    private val extractor = FeatureExtractor()
    private val voEngine = VisualOdometryEngine()
    private val poseEstimator = PoseEstimator()
    private val localMapping = LocalMappingEngine()

    private var prevFrame: SlamFrame? = null
    private var frameIdSeq = 0L

    fun reset() {
        prevFrame = null
        frameIdSeq = 0L
        poseEstimator.reset()
        localMapping.clear()
    }

    fun process(bitmap: Bitmap?): Pair<PoseEstimate, Pair<SlamMap, SlamMetadata>> {
        val startTime = SystemClock.elapsedRealtime()

        val features = extractor.extract(bitmap)
        val currFrame = SlamFrame(
            id = frameIdSeq++,
            timestamp = System.currentTimeMillis(),
            features = features
        )

        val (matchedCount, delta) = voEngine.matchAndEstimate(prevFrame, currFrame)
        val (dx, dy) = delta

        val totalPrevFeatures = prevFrame?.features?.size ?: 0
        val matchedRatio = if (totalPrevFeatures > 0) matchedCount.toFloat() / totalPrevFeatures else 1.0f

        val pose = poseEstimator.updatePose(dx, dy, matchedRatio, currFrame.timestamp)
        val mapSize = localMapping.updateMap(features, matchedCount)

        prevFrame = currFrame

        val duration = SystemClock.elapsedRealtime() - startTime
        val meta = SlamMetadata(
            featureCount = features.size,
            matchedFeatures = matchedCount,
            trackingConfidence = matchedRatio,
            poseConfidence = pose.confidence,
            localMapPoints = mapSize,
            processingTimeMs = duration,
            successful = true
        )

        return Pair(pose, Pair(localMapping.getMap(), meta))
    }
}
