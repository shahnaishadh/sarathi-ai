package com.pathhelper.ai.voice

import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.GuidanceDecision
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.ThreatLevel
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationState
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.RoomTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VoiceGuidanceEngineTest {

    private val dummyContext = NavigationContext(
        primaryLandmark = null,
        secondaryLandmark = null,
        approachingLandmark = null,
        activeContext = "Open Path Forward",
        observations = emptyList()
    )

    private val dummyState = LandmarkNavigationState(
        destination = RoomTarget("1"),
        progress = NavigationProgress.SEARCHING,
        currentStep = null,
        completedSteps = emptyList(),
        remainingDistanceMeters = null
    )

    private fun createNavigatingEngine(): VoiceGuidanceEngine {
        val engine = VoiceGuidanceEngine()
        val stateField = VoiceGuidanceEngine::class.java.getDeclaredField("sarthiState")
        stateField.isAccessible = true
        stateField.set(engine, SarthiState.NAVIGATING)
        return engine
    }

    @Test
    fun testCriticalDistanceBand() {
        val engine = createNavigatingEngine()
        val decision = GuidanceDecision(
            action = GuidanceAction.STOP,
            reason = "Obstacle detected",
            selectedCorridor = null,
            highestThreatId = 1,
            highestThreatClassName = "Chair",
            highestThreatLevel = ThreatLevel.CRITICAL,
            confidence = 1.0f,
            highestThreatDistance = 0.7f
        )

        val result = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result.first)
        assertEquals("Chair very close ahead.", result.first?.text)
    }

    @Test
    fun testWarningDistanceBand() {
        val engine = createNavigatingEngine()
        val decision = GuidanceDecision(
            action = GuidanceAction.MOVE_SLIGHTLY_LEFT,
            reason = "Avoid obstacle via left",
            selectedCorridor = HorizontalZone.LEFT,
            highestThreatId = 1,
            highestThreatClassName = "Chair",
            highestThreatLevel = ThreatLevel.MEDIUM,
            confidence = 0.9f,
            highestThreatDistance = 2.0f
        )

        val result = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result.first)
        assertEquals("Chair ahead about 2 meters. Move slightly left", result.first?.text)
    }

    @Test
    fun testInformationalDistanceBand() {
        val engine = createNavigatingEngine()
        val decision = GuidanceDecision(
            action = GuidanceAction.KEEP_CENTER,
            reason = "Path clear",
            selectedCorridor = HorizontalZone.CENTER,
            highestThreatId = 1,
            highestThreatClassName = "Chair",
            highestThreatLevel = ThreatLevel.LOW,
            confidence = 0.9f,
            highestThreatDistance = 4.0f
        )

        val result = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result.first)
        assertEquals("Chair ahead about 4 meters.", result.first?.text)
    }

    @Test
    fun testMultiStepInstructionFormatting() {
        val engine = createNavigatingEngine()
        val decision = GuidanceDecision(
            action = GuidanceAction.MOVE_SLIGHTLY_LEFT,
            reason = "Avoid obstacle via left",
            selectedCorridor = HorizontalZone.LEFT,
            highestThreatId = 1,
            highestThreatClassName = "Chair",
            highestThreatLevel = ThreatLevel.MEDIUM,
            confidence = 0.9f,
            secondaryAction = GuidanceAction.KEEP_CENTER,
            secondaryReason = "then continue straight",
            highestThreatDistance = 2.0f
        )

        val result = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result.first)
        assertEquals("Chair ahead about 2 meters. Move slightly left then continue straight", result.first?.text)
    }

    @Test
    fun testStateBasedSuppression_StationaryObstacle() {
        val engine = createNavigatingEngine()
        val decision = GuidanceDecision(
            action = GuidanceAction.MOVE_RIGHT,
            reason = "Avoid obstacle via right",
            selectedCorridor = HorizontalZone.RIGHT,
            highestThreatId = 1,
            highestThreatClassName = "Person",
            highestThreatLevel = ThreatLevel.HIGH,
            confidence = 0.9f,
            highestThreatDistance = 3.0f
        )

        // First call: should announce
        val result1 = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result1.first)
        assertEquals("Person ahead about 3 meters. Move right", result1.first?.text)

        // Second call: same stationary obstacle state => should suppress (return null)
        val result2 = engine.process(
            decision = decision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        org.junit.Assert.assertNull(result2.first)

        // Third call: same threat ID but distance changes significantly (from 3.0m to 1.0m) => should announce
        val closeDecision = decision.copy(
            highestThreatDistance = 1.0f,
            highestThreatLevel = ThreatLevel.CRITICAL
        )
        val result3 = engine.process(
            decision = closeDecision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result3.first)
        assertEquals("Person ahead about 1 meters. Move right", result3.first?.text)

        // Fourth call: new threat ID => should announce
        val newThreatDecision = decision.copy(
            highestThreatId = 2,
            highestThreatDistance = 3.0f
        )
        val result4 = engine.process(
            decision = newThreatDecision,
            memoryEvents = emptyList(),
            navigationContext = dummyContext,
            routeEvents = emptyList(),
            navigationState = dummyState
        )
        assertNotNull(result4.first)
        assertEquals("Person ahead about 3 meters. Move right", result4.first?.text)
    }
}
