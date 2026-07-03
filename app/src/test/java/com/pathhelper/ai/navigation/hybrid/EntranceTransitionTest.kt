package com.pathhelper.ai.navigation.hybrid

import com.pathhelper.ai.tracking.Track
import org.junit.Assert.*
import org.junit.Test

class EntranceTransitionTest {

    @Test
    fun testEntranceTransitionOutdoorToApproach() {
        val transition = EntranceTransition()
        
        // 1. Distance > 15m should remain OUTDOOR
        val mode1 = transition.evaluate(NavigationMode.OUTDOOR, 20.0f, emptyList())
        assertEquals(NavigationMode.OUTDOOR, mode1)

        // 2. Distance < 15m should switch to ENTRANCE_APPROACH
        val mode2 = transition.evaluate(NavigationMode.OUTDOOR, 12.0f, emptyList())
        assertEquals(NavigationMode.ENTRANCE_APPROACH, mode2)
    }

    @Test
    fun testEntranceTransitionApproachToIndoor() {
        val transition = EntranceTransition()
        val mockDoorTrack = Track(
            id = 1,
            classId = 0,
            centerX = 320f,
            centerY = 240f,
            velocityX = 0f,
            velocityY = 0f,
            age = 5,
            missedFrames = 0,
            confidence = 0.9f,
            width = 100f,
            height = 200f
        )

        // 1. Distance < 15m but no door detected: remain ENTRANCE_APPROACH
        val mode1 = transition.evaluate(NavigationMode.ENTRANCE_APPROACH, 5.0f, emptyList())
        assertEquals(NavigationMode.ENTRANCE_APPROACH, mode1)

        // 2. Distance < 15m and door detected: switch to INDOOR
        val mode2 = transition.evaluate(NavigationMode.ENTRANCE_APPROACH, 5.0f, listOf(mockDoorTrack))
        assertEquals(NavigationMode.INDOOR, mode2)
    }
}
