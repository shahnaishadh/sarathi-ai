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
        val stateField = VoiceGuidanceEngine::class.java.getDeclaredField("sarathiState")
        stateField.isAccessible = true
        stateField.set(engine, SarathiState.NAVIGATING)
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
}
