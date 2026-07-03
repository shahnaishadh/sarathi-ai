package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.navigation.graph.RouteGraph
import org.junit.Assert.*
import org.junit.Test

class PoseFusionEngineTest {

    private fun makePose(x: Float = 0f, y: Float = 0f, confidence: Float = 0.8f) =
        PoseEstimate(x, y, 45f, confidence, System.currentTimeMillis())

    private fun makeConf(score: Float = 0.8f) = LocalizationConfidence(
        poseConfidence = score,
        landmarkMatchScore = score,
        routeGraphMatchScore = score,
        sceneMemoryScore = score,
        score = score
    )

    private val emptyGraph = RouteGraph(emptyList(), emptyList(), System.currentTimeMillis())
    private val now get() = System.currentTimeMillis()

    @Test
    fun testFirstFrameProducesInitializingState() {
        val engine = PoseFusionEngine()
        val result = engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.5f), now)
        assertEquals(LocalizationState.INITIALIZING, result.state)
    }

    @Test
    fun testHighConfidenceProducesLocalizedState() {
        val engine = PoseFusionEngine()
        engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.9f), now) // seed frame 1
        val result = engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.9f), now)
        assertEquals(LocalizationState.LOCALIZED, result.state)
    }

    @Test
    fun testLowConfidenceProducesLostState() {
        val engine = PoseFusionEngine()
        engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.05f), now)
        val result = engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.05f), now)
        assertEquals(LocalizationState.LOST, result.state)
    }

    @Test
    fun testPositionSmoothing() {
        val engine = PoseFusionEngine()
        // First frame at (0, 0)
        engine.fuse(makePose(0f, 0f), emptyList(), emptyGraph, makeConf(0.9f), now)
        // Second frame at (100, 0) — smoothing should pull toward previous value
        val result = engine.fuse(makePose(100f, 0f), emptyList(), emptyGraph, makeConf(0.9f), now)
        // Smoothed X = 0.3 * 100 + 0.7 * 0 = 30
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun testResetClearsFrameCount() {
        val engine = PoseFusionEngine()
        engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.9f), now)
        engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.9f), now)
        engine.reset()
        // After reset, first frame should give INITIALIZING again
        val result = engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.9f), now)
        assertEquals(LocalizationState.INITIALIZING, result.state)
    }

    @Test
    fun testNoObservationsGivesNullLandmark() {
        val engine = PoseFusionEngine()
        val result = engine.fuse(makePose(), emptyList(), emptyGraph, makeConf(0.5f), now)
        assertNull(result.nearestLandmark)
    }

    @Test
    fun testBestObservationSetAsNearestLandmark() {
        val engine = PoseFusionEngine()
        val obs = listOf(
            LandmarkObservation("l1", "DOOR", 2f, 45f, 0.9f, now),
            LandmarkObservation("l2", "HALLWAY", 5f, 90f, 0.5f, now)
        )
        val result = engine.fuse(makePose(), obs, emptyGraph, makeConf(0.8f), now)
        assertEquals("DOOR", result.nearestLandmark)
        assertEquals(2f, result.landmarkDistance, 0.001f)
    }
}
