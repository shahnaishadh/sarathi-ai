package com.pathhelper.ai.camera

/**
 * Represents performance metrics for camera frame analysis.
 *
 * This
data class captures the throughput and processing speed of the vision pipeline, 
 * which is used for real-time monitoring and adaptive processing adjustments within 
 * the Sarathi architecture.
 */
data
class AnalysisStats(
    val framesReceived: Long,
    val fps: Double
)
