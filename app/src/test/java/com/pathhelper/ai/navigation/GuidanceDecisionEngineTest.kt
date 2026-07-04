package com.pathhelper.ai.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GuidanceDecisionEngineTest {

    @Test
    fun testLeftRightCorrectness_ObstacleOnLeft() {
        val engine = GuidanceDecisionEngine()
        
        // Obstacle on LEFT (leftScore is penalized, rightScore is clear)
        // Center is fully blocked (centerScore = 20f)
        val corridors = listOf(
            SafeCorridor(HorizontalZone.SHARP_LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.LEFT, CorridorState.BLOCKED, 1, ThreatLevel.HIGH, 2.0f, 20f),
            SafeCorridor(HorizontalZone.CENTER, CorridorState.BLOCKED, 1, ThreatLevel.MEDIUM, 2.0f, 20f),
            SafeCorridor(HorizontalZone.RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.SHARP_RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f)
        )

        val result = engine.process(emptyList(), corridors)
        
        // Since center is blocked, and left has obstacle (score 20f), but right is clear (score 100f),
        // we should avoid the obstacle on the left by moving to the RIGHT.
        assertEquals(GuidanceAction.MOVE_RIGHT, result.first.action)
    }

    @Test
    fun testLeftRightCorrectness_ObstacleOnRight() {
        val engine = GuidanceDecisionEngine()
        
        // Obstacle on RIGHT (rightScore is penalized, leftScore is clear)
        // Center is fully blocked (centerScore = 20f)
        val corridors = listOf(
            SafeCorridor(HorizontalZone.SHARP_LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.CENTER, CorridorState.BLOCKED, 1, ThreatLevel.MEDIUM, 2.0f, 20f),
            SafeCorridor(HorizontalZone.RIGHT, CorridorState.BLOCKED, 1, ThreatLevel.HIGH, 2.0f, 20f),
            SafeCorridor(HorizontalZone.SHARP_RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f)
        )

        val result = engine.process(emptyList(), corridors)
        
        // Since center is blocked, and right has obstacle (score 20f), but left is clear (score 100f),
        // we should avoid the obstacle on the right by moving to the LEFT.
        assertEquals(GuidanceAction.MOVE_LEFT, result.first.action)
    }

    @Test
    fun testGuidanceHysteresis() {
        val engine = GuidanceDecisionEngine()

        // Frame 1: Slightly better on right.
        // It should choose MOVE_RIGHT.
        val corridors1 = listOf(
            SafeCorridor(HorizontalZone.SHARP_LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 70f),
            SafeCorridor(HorizontalZone.CENTER, CorridorState.BLOCKED, 1, ThreatLevel.CRITICAL, 2.0f, 20f),
            SafeCorridor(HorizontalZone.RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 75f),
            SafeCorridor(HorizontalZone.SHARP_RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f)
        )
        val res1 = engine.process(emptyList(), corridors1)
        assertEquals(GuidanceAction.MOVE_RIGHT, res1.first.action)

        // Frame 2: Slightly better on left now (e.g. 73f vs 70f), but because of hysteresis,
        // it should stick to MOVE_RIGHT because the difference is within the 15f bias.
        val corridors2 = listOf(
            SafeCorridor(HorizontalZone.SHARP_LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 73f),
            SafeCorridor(HorizontalZone.CENTER, CorridorState.BLOCKED, 1, ThreatLevel.CRITICAL, 2.0f, 20f),
            SafeCorridor(HorizontalZone.RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 70f),
            SafeCorridor(HorizontalZone.SHARP_RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f)
        )
        val res2 = engine.process(emptyList(), corridors2)
        assertEquals(GuidanceAction.MOVE_RIGHT, res2.first.action)

        // Frame 3: Significantly better on left (e.g. 90f vs 70f), which exceeds the hysteresis bias.
        // It should switch to MOVE_LEFT.
        val corridors3 = listOf(
            SafeCorridor(HorizontalZone.SHARP_LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f),
            SafeCorridor(HorizontalZone.LEFT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 90f),
            SafeCorridor(HorizontalZone.CENTER, CorridorState.BLOCKED, 1, ThreatLevel.CRITICAL, 2.0f, 20f),
            SafeCorridor(HorizontalZone.RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 70f),
            SafeCorridor(HorizontalZone.SHARP_RIGHT, CorridorState.SAFE, 0, ThreatLevel.LOW, 2.0f, 100f)
        )
        val res3 = engine.process(emptyList(), corridors3)
        assertEquals(GuidanceAction.MOVE_LEFT, res3.first.action)
    }
}
