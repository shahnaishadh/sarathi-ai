package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.graph.RouteGraph
import com.pathhelper.ai.world.WorldModel
import org.junit.Assert.*
import org.junit.Test

class IndoorLocalizationEngineTest {

    private fun makePose(x: Float = 1f, y: Float = 1f, confidence: Float = 0.8f) =
        PoseEstimate(x, y, 45f, confidence, System.currentTimeMillis())

    private val emptyScene = SceneMemory(emptyList())
    private val emptyWorld = WorldModel(emptyList(), emptyList())
    private val emptyGraph = RouteGraph(emptyList(), emptyList(), System.currentTimeMillis())

    @Test
    fun testProcessReturnsTriple() {
        val engine = IndoorLocalizationEngine()
        val (pos, conf, meta) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertNotNull(pos)
        assertNotNull(conf)
        assertNotNull(meta)
    }

    @Test
    fun testMetadataIsSuccessfulOnEmptyInputs() {
        val engine = IndoorLocalizationEngine()
        val (_, _, meta) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertTrue(meta.successful)
    }

    @Test
    fun testZeroLandmarksYieldsZeroMatches() {
        val engine = IndoorLocalizationEngine()
        val (_, _, meta) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertEquals(0, meta.matchedLandmarks)
        assertEquals(0, meta.candidateCount)
    }

    @Test
    fun testConfidenceScoreInRange() {
        val engine = IndoorLocalizationEngine()
        val (_, conf, _) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertTrue(conf.score in 0f..1f)
    }

    @Test
    fun testProcessingTimeIsNonNegative() {
        val engine = IndoorLocalizationEngine()
        val (_, _, meta) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertTrue(meta.processingTimeMs >= 0L)
    }

    @Test
    fun testPositionStateInitializingOnFirstFrame() {
        val engine = IndoorLocalizationEngine()
        val (pos, _, _) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        // First frame → INITIALIZING state from PoseFusionEngine
        assertEquals(LocalizationState.INITIALIZING, pos.state)
    }

    @Test
    fun testResetAllowsNewInitialization() {
        val engine = IndoorLocalizationEngine()
        engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        engine.reset()
        val (pos, _, _) = engine.process(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertEquals(LocalizationState.INITIALIZING, pos.state)
    }

    @Test
    fun testTimestampMatchesPose() {
        val engine = IndoorLocalizationEngine()
        val ts = System.currentTimeMillis()
        val pose = PoseEstimate(0f, 0f, 0f, 0f, ts)
        val (pos, _, _) = engine.process(pose, emptyScene, emptyWorld, emptyGraph)
        assertEquals(ts, pos.timestamp)
    }
}
