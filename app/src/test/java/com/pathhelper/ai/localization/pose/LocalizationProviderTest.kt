package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.graph.RouteGraph
import com.pathhelper.ai.world.WorldModel
import org.junit.Assert.*
import org.junit.Test

class LocalizationProviderTest {

    private fun makePose(
        x: Float = 1f,
        y: Float = 1f,
        heading: Float = 45f,
        confidence: Float = 0.8f,
        timestamp: Long = System.currentTimeMillis()
    ) = PoseEstimate(x, y, heading, confidence, timestamp)

    private val emptyScene = SceneMemory(emptyList())
    private val emptyWorld = WorldModel(emptyList(), emptyList())
    private val emptyGraph = RouteGraph(emptyList(), emptyList(), System.currentTimeMillis())

    // -------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------

    @Test
    fun testInitialPositionIsNull() {
        val provider = LocalizationProvider()
        // Before any update, position must be absent
        assertNull(provider.getPosition())
    }

    @Test
    fun testInitialConfidenceIsZero() {
        val provider = LocalizationProvider()
        val conf = provider.getConfidence()
        // ZERO sentinel is the contract before first update
        assertEquals(LocalizationConfidence.ZERO, conf)
    }

    @Test
    fun testInitialMetadataIsNull() {
        val provider = LocalizationProvider()
        assertNull(provider.getMetadata())
    }

    // -------------------------------------------------------------------
    // After first update
    // -------------------------------------------------------------------

    @Test
    fun testAfterFirstUpdatePositionIsNotNull() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        assertNotNull(provider.getPosition())
    }

    @Test
    fun testFirstUpdateYieldsInitializingState() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        val pos = provider.getPosition()!!
        // PoseFusionEngine emits INITIALIZING on frame 1
        assertEquals(LocalizationState.INITIALIZING, pos.state)
    }

    @Test
    fun testConfidenceScoreInValidRange() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        val conf = provider.getConfidence()
        assertTrue(
            "Confidence score must be in [0, 1] but was ${conf.score}",
            conf.score in 0f..1f
        )
    }

    @Test
    fun testMetadataSuccessfulAfterValidUpdate() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        val meta = provider.getMetadata()
        assertNotNull(meta)
        assertTrue("Metadata.successful must be true for a valid update", meta!!.successful)
    }

    @Test
    fun testTimestampPropagatedFromPose() {
        val provider = LocalizationProvider()
        val fixedTs = 1_700_000_000_000L
        provider.update(makePose(timestamp = fixedTs), emptyScene, emptyWorld, emptyGraph)
        val pos = provider.getPosition()!!
        assertEquals(
            "LocalizedPosition.timestamp must match the SLAM pose timestamp",
            fixedTs,
            pos.timestamp
        )
    }

    // -------------------------------------------------------------------
    // Multiple consecutive updates
    // -------------------------------------------------------------------

    @Test
    fun testMultipleUpdatesDoNotCrash() {
        val provider = LocalizationProvider()
        repeat(5) {
            provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        }
        val conf = provider.getConfidence()
        assertTrue(conf.score in 0f..1f)
    }

    @Test
    fun testSecondUpdateAdvancesBeyondInitializing() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        val pos = provider.getPosition()!!
        // After the 2nd frame, PoseFusionEngine may return SEARCHING or LOST
        // (no landmark data supplied), but NEVER INITIALIZING again
        assertNotEquals(LocalizationState.INITIALIZING, pos.state)
    }

    // -------------------------------------------------------------------
    // Reset behaviour
    // -------------------------------------------------------------------

    @Test
    fun testResetClearsPosition() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        provider.reset()
        assertNull(provider.getPosition())
    }

    @Test
    fun testResetClearsMetadata() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        provider.reset()
        assertNull(provider.getMetadata())
    }

    @Test
    fun testResetRestoresZeroConfidence() {
        val provider = LocalizationProvider()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        provider.reset()
        assertEquals(LocalizationConfidence.ZERO, provider.getConfidence())
    }

    @Test
    fun testFirstUpdateAfterResetIsInitializing() {
        val provider = LocalizationProvider()
        // Warm-up
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        // Reset and re-initialize
        provider.reset()
        provider.update(makePose(), emptyScene, emptyWorld, emptyGraph)
        val pos = provider.getPosition()!!
        assertEquals(
            "After reset, first update must restart in INITIALIZING state",
            LocalizationState.INITIALIZING,
            pos.state
        )
    }
}
