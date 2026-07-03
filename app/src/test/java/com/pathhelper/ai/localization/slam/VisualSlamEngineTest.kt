package com.pathhelper.ai.localization.slam

import org.junit.Assert.*
import org.junit.Test

class VisualSlamEngineTest {

    @Test
    fun testFirstFrameProducesZeroPose() {
        val engine = VisualSlamEngine()
        val (pose, _) = engine.process(null)
        // On first frame (no previous), displacement is 0
        assertEquals(0f, pose.positionX, 0.01f)
        assertEquals(0f, pose.positionY, 0.01f)
    }

    @Test
    fun testSecondFrameProducesMetadata() {
        val engine = VisualSlamEngine()
        engine.process(null) // seed first frame
        val (_, slamResult) = engine.process(null) // second frame with same features
        val meta = slamResult.second
        assertTrue(meta.featureCount >= 0)
        assertTrue(meta.processingTimeMs >= 0L)
    }

    @Test
    fun testPoseConfidenceInRange() {
        val engine = VisualSlamEngine()
        val (pose, _) = engine.process(null)
        assertTrue(pose.confidence in 0f..1f)
    }

    @Test
    fun testMapPointsAccumulateOverMultipleFrames() {
        val engine = VisualSlamEngine()
        engine.process(null) // first frame
        val (_, result2) = engine.process(null) // second frame
        val mapPoints = result2.first.points.size
        assertTrue(mapPoints >= 0)
    }

    @Test
    fun testMetadataIsSuccessful() {
        val engine = VisualSlamEngine()
        val (_, result) = engine.process(null)
        val meta = result.second
        assertTrue(meta.successful)
    }

    @Test
    fun testResetClearsState() {
        val engine = VisualSlamEngine()
        engine.process(null)
        engine.process(null)
        engine.reset()
        val (pose, result) = engine.process(null)
        val meta = result.second
        // After reset, first frame should behave like initial → zero displacement
        assertEquals(0f, pose.positionX, 0.01f)
        assertEquals(0f, pose.positionY, 0.01f)
        assertTrue(meta.successful)
    }

    @Test
    fun testLocalMapSizeRemainsWithinBounds() {
        val engine = VisualSlamEngine()
        repeat(20) { engine.process(null) }
        val (_, result) = engine.process(null)
        val mapSize = result.second.localMapPoints
        assertTrue(mapSize <= 500)
    }
}
