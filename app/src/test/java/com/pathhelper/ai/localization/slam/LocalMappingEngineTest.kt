package com.pathhelper.ai.localization.slam

import org.junit.Assert.*
import org.junit.Test

class LocalMappingEngineTest {

    private fun makeFeature(x: Float, y: Float, seed: Float): VisualFeature {
        return VisualFeature(
            point = FeaturePoint(x, y, 100f, 0),
            descriptor = FloatArray(16) { i -> (seed + i) / 255.0f }
        )
    }

    @Test
    fun testInitialMapIsEmpty() {
        val engine = LocalMappingEngine()
        assertEquals(0, engine.getMap().points.size)
    }

    @Test
    fun testPointsAddedWhenMatchedBelowThreshold() {
        val engine = LocalMappingEngine()
        val features = listOf(makeFeature(100f, 100f, 50f), makeFeature(200f, 200f, 80f))
        // matchedFeatures < 20 triggers adding new map points
        val count = engine.updateMap(features, 5)
        assertEquals(features.size, count)
    }

    @Test
    fun testMapDoesNotExceed500Points() {
        val engine = LocalMappingEngine()
        // Add 600 features in batches of 10
        repeat(60) {
            val features = (0 until 10).map { i -> makeFeature(it * 10f + i, 0f, it * 5f) }
            engine.updateMap(features, 0) // 0 matches → always add
        }
        assertTrue(engine.getMap().points.size <= 500)
    }

    @Test
    fun testObservationCountIncreasesWhenSufficientMatches() {
        val engine = LocalMappingEngine()
        val features = listOf(makeFeature(100f, 100f, 50f))
        // First insertion
        engine.updateMap(features, 0)
        val initCount = engine.getMap().points.first().observedCount

        // Subsequent update with high match count (>= 20) only increments observed count
        engine.updateMap(features, 25)
        val updatedCount = engine.getMap().points.first().observedCount
        assertEquals(initCount + 1, updatedCount)
    }

    @Test
    fun testClearResetsMap() {
        val engine = LocalMappingEngine()
        engine.updateMap(listOf(makeFeature(10f, 10f, 50f)), 0)
        engine.clear()
        assertEquals(0, engine.getMap().points.size)
    }

    @Test
    fun testGetMapReturnsSnapshot() {
        val engine = LocalMappingEngine()
        val features = listOf(makeFeature(50f, 60f, 30f))
        engine.updateMap(features, 0)
        val map1 = engine.getMap()
        engine.updateMap(listOf(makeFeature(70f, 80f, 40f)), 0)
        val map2 = engine.getMap()
        // Snapshots are independent lists
        assertNotEquals(map1.points.size, map2.points.size)
    }
}
