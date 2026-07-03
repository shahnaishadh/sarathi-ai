package com.pathhelper.ai.navigation.hybrid

import com.pathhelper.ai.navigation.common.target.BuildingTarget
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceDecision
import org.junit.Assert.*
import org.junit.Test

class HybridNavigationEngineTest {

    @Test
    fun testHybridEngineProcessFlow() {
        val engine = HybridNavigationEngine()
        val target = BuildingTarget("office_building", 37.7749, -122.4194)

        // 1. Initial State should be OUTDOOR when rejected
        val (state1, _) = engine.process(target, EntranceDecision.ENTRANCE_REJECTED, false)
        assertEquals(NavigationMode.OUTDOOR, state1.currentMode)
        assertEquals("PENDING", state1.transitionStatus)

        // 2. Approach state when more evidence is required
        val (state2, _) = engine.process(target, EntranceDecision.MORE_EVIDENCE_REQUIRED, false)
        assertEquals(NavigationMode.ENTRANCE_APPROACH, state2.currentMode)
        assertEquals("APPROACHING", state2.transitionStatus)

        // 3. Indoor state transition when entrance is confirmed
        val (state3, _) = engine.process(target, EntranceDecision.ENTRANCE_CONFIRMED, false)
        assertEquals(NavigationMode.INDOOR, state3.currentMode)
        assertEquals("COMPLETED", state3.transitionStatus)
    }
}
