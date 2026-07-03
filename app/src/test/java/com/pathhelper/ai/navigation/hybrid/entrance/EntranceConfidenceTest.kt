package com.pathhelper.ai.navigation.hybrid.entrance

import org.junit.Assert.*
import org.junit.Test

class EntranceConfidenceTest {

    @Test
    fun testEntranceConfidenceScore() {
        val confidence = EntranceConfidence(
            score = 0.85f,
            doorConfidence = 0.9f,
            gpsProximityScore = 0.8f,
            sceneMemoryEvidence = 0.7f,
            worldModelEvidence = 1.0f,
            historicalSuccessScore = 1.0f
        )
        assertEquals(0.85f, confidence.score, 0.001f)
        assertEquals(0.9f, confidence.doorConfidence, 0.001f)
        assertEquals(0.8f, confidence.gpsProximityScore, 0.001f)
        assertEquals(0.7f, confidence.sceneMemoryEvidence, 0.001f)
        assertEquals(1.0f, confidence.worldModelEvidence, 0.001f)
        assertEquals(1.0f, confidence.historicalSuccessScore, 0.001f)
    }
}
