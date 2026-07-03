package com.pathhelper.ai.localization.slam

import org.junit.Assert.*
import org.junit.Test

class FeatureExtractorTest {

    @Test
    fun testExtractReturnsFeatureListOnNullBitmap() {
        val extractor = FeatureExtractor()
        // Null bitmap triggers the JVM stub fallback
        val features = extractor.extract(null)

        // Stub fallback should return 50 synthetic features
        assertEquals(50, features.size)
    }

    @Test
    fun testFeatureHasValidDescriptor() {
        val extractor = FeatureExtractor()
        val features = extractor.extract(null)

        assertTrue(features.isNotEmpty())
        val first = features.first()
        assertEquals(16, first.descriptor.size)
    }

    @Test
    fun testFeaturePointCoordinatesArePositive() {
        val extractor = FeatureExtractor()
        val features = extractor.extract(null)

        for (feat in features) {
            assertTrue(feat.point.x >= 0f)
            assertTrue(feat.point.y >= 0f)
        }
    }

    @Test
    fun testFeatureDescriptorValuesAreNormalized() {
        val extractor = FeatureExtractor()
        val features = extractor.extract(null)

        for (feat in features) {
            for (value in feat.descriptor) {
                assertTrue(value in 0.0f..1.5f) // Descriptor elements should be in a sane range
            }
        }
    }

    @Test
    fun testEachCallReturnsSameFallbackCount() {
        val extractor = FeatureExtractor()
        val f1 = extractor.extract(null)
        val f2 = extractor.extract(null)
        assertEquals(f1.size, f2.size)
    }
}
