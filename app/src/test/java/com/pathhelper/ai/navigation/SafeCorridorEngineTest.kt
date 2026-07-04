package com.pathhelper.ai.navigation

import com.pathhelper.ai.tracking.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SafeCorridorEngineTest {

    @Test
    fun testSafeGapDetection_BetweenChairAndTv() {
        val engine = SafeCorridorEngine()

        // Obstacle 1: Chair (on the left, slightly encroaching on center)
        // Center: 180f, Width: 160f => Left: 100f, Right: 260f
        // Normalized left: 0.156f, right: 0.406f.
        val chair = Track(
            id = 1,
            classId = 56, // Chair
            centerX = 180f,
            centerY = 320f,
            velocityX = 0f,
            velocityY = 0f,
            age = 10,
            missedFrames = 0,
            confidence = 0.9f,
            width = 160f,
            height = 160f,
            distanceMeters = 2.0f
        )

        // Obstacle 2: TV (on the right, slightly encroaching on center)
        // Center: 460f, Width: 160f => Left: 380f, Right: 540f
        // Normalized left: 0.594f, right: 0.844f.
        val tv = Track(
            id = 2,
            classId = 62, // TV
            centerX = 460f,
            centerY = 320f,
            velocityX = 0f,
            velocityY = 0f,
            age = 10,
            missedFrames = 0,
            confidence = 0.9f,
            width = 160f,
            height = 160f,
            distanceMeters = 2.0f
        )

        val tracks = listOf(chair, tv)
        val result = engine.process(tracks)
        val corridors = result.first

        // Verify that CENTER corridor is marked as SAFE because of the traversable gap in the middle (width = 0.188f >= 0.12f)
        val centerCorridor = corridors.find { it.horizontalZone == HorizontalZone.CENTER }
        org.junit.Assert.assertNotNull(centerCorridor)
        assertEquals(CorridorState.SAFE, centerCorridor?.state)
    }
}
