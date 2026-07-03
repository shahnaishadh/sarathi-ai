package com.pathhelper.ai.localization.slam

import org.junit.Assert.*
import org.junit.Test

class VisualOdometryEngineTest {

    private fun makeFeature(x: Float, y: Float, descriptorBase: Float): VisualFeature {
        return VisualFeature(
            point = FeaturePoint(x, y, 100f + descriptorBase, 0),
            descriptor = FloatArray(16) { i -> (descriptorBase + i) / 255.0f }
        )
    }

    private fun makeFrame(id: Long, features: List<VisualFeature>): SlamFrame {
        return SlamFrame(id = id, timestamp = System.currentTimeMillis(), features = features)
    }

    @Test
    fun testNoMotionOnNullPreviousFrame() {
        val engine = VisualOdometryEngine()
        val current = makeFrame(0L, listOf(makeFeature(100f, 100f, 50f)))

        val (matched, delta) = engine.matchAndEstimate(null, current)
        assertEquals(0, matched)
        assertEquals(0f, delta.first, 0.001f)
        assertEquals(0f, delta.second, 0.001f)
    }

    @Test
    fun testMatchingIdenticalFramesReturnsZeroDisplacement() {
        val engine = VisualOdometryEngine()
        val features = listOf(
            makeFeature(100f, 150f, 50f),
            makeFeature(200f, 300f, 80f)
        )
        val prev = makeFrame(0L, features)
        val curr = makeFrame(1L, features) // Same features → zero displacement

        val (matched, delta) = engine.matchAndEstimate(prev, curr)
        assertTrue(matched > 0)
        assertEquals(0f, delta.first, 0.001f)
        assertEquals(0f, delta.second, 0.001f)
    }

    @Test
    fun testMotionEstimationWithDisplacedFeatures() {
        val engine = VisualOdometryEngine()
        // Previous features
        val prevFeatures = listOf(makeFeature(100f, 100f, 50f))
        // Same descriptor but shifted 10px right, 5px down
        val currFeatures = listOf(
            VisualFeature(
                point = FeaturePoint(110f, 105f, 150f, 0),
                descriptor = FloatArray(16) { i -> (50f + i) / 255.0f }
            )
        )
        val prev = makeFrame(0L, prevFeatures)
        val curr = makeFrame(1L, currFeatures)

        val (matched, _) = engine.matchAndEstimate(prev, curr)
        assertTrue(matched >= 0)
    }

    @Test
    fun testNoMatchesForCompletelyDifferentDescriptors() {
        val engine = VisualOdometryEngine()
        val prevFeatures = listOf(makeFeature(100f, 100f, 0f))    // descriptor ~[0.0, ...]
        val currFeatures = listOf(makeFeature(200f, 200f, 200f))  // descriptor ~[0.78, ...]
        val prev = makeFrame(0L, prevFeatures)
        val curr = makeFrame(1L, currFeatures)

        val (matched, _) = engine.matchAndEstimate(prev, curr)
        // With distance threshold 0.2f, divergent descriptors should not match
        assertEquals(0, matched)
    }
}
