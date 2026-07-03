package com.pathhelper.ai.localization.slam

import org.junit.Assert.*
import org.junit.Test

class PoseEstimatorTest {

    @Test
    fun testInitialPoseIsZero() {
        val estimator = PoseEstimator()
        val pose = estimator.updatePose(0f, 0f, 0f, 1000L)
        assertEquals(0f, pose.positionX, 0.001f)
        assertEquals(0f, pose.positionY, 0.001f)
    }

    @Test
    fun testPositionAccumulates() {
        val estimator = PoseEstimator()
        estimator.updatePose(100f, 0f, 1.0f, 1000L)
        val pose2 = estimator.updatePose(100f, 0f, 1.0f, 2000L)
        // Each update adds dx * 0.01f so 100 * 0.01 = 1.0 per step
        assertEquals(2.0f, pose2.positionX, 0.01f)
        assertEquals(0.0f, pose2.positionY, 0.01f)
    }

    @Test
    fun testConfidenceIsClamped() {
        val estimator = PoseEstimator()
        val pose = estimator.updatePose(50f, 0f, 1.5f, 1000L) // matchedRatio > 1
        assertTrue(pose.confidence <= 1.0f)
        assertTrue(pose.confidence >= 0.0f)
    }

    @Test
    fun testHeadingUpdatesOnMotion() {
        val estimator = PoseEstimator()
        val pose = estimator.updatePose(100f, 0f, 0.8f, 1000L)
        // Moving right → heading should be near 0 degrees
        assertEquals(0f, pose.headingDegrees, 1.0f)
    }

    @Test
    fun testResetClearsState() {
        val estimator = PoseEstimator()
        estimator.updatePose(500f, 300f, 1.0f, 1000L)
        estimator.reset()
        val pose = estimator.updatePose(0f, 0f, 0f, 2000L)
        assertEquals(0f, pose.positionX, 0.001f)
        assertEquals(0f, pose.positionY, 0.001f)
    }

    @Test
    fun testTimestampPreserved() {
        val estimator = PoseEstimator()
        val ts = 123456789L
        val pose = estimator.updatePose(0f, 0f, 0f, ts)
        assertEquals(ts, pose.timestamp)
    }
}
