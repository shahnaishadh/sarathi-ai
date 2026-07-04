package com.pathhelper.ai.voice

import android.os.SystemClock
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.GuidanceDecision
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.memory.MemoryEvent
import com.pathhelper.ai.memory.MemoryObservation
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.route.RouteLandmark
import com.pathhelper.ai.route.RouteEvent
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationState
import com.pathhelper.ai.navigation.hybrid.HybridNavigationState
import com.pathhelper.ai.navigation.hybrid.NavigationMode
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/**
* Represents the different states or configurations of Sarthi State.
*/
enum class SarthiState {
    INITIALIZING,
    WELCOME,
    READY,
    NAVIGATING
}
/**
* Coordinates Voice Guidance Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Voice Guidance Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class VoiceGuidanceEngine {
    private var lastSpokenAction: GuidanceAction? = null
    private var lastSpokenText: String? = null
    private var lastSpokenTimestamp: Long = 0L

    private var lastNavigationProgress: NavigationProgress? = null
    private var lastStepInstruction: String? = null
    private var lastDestination: NavigationTarget? = null
    private var lastHybridMode: NavigationMode? = null

    private var generatedCount = 0
    private var suppressedCount = 0

    // Sarthi Assistive UX States
    private var sarthiState = SarthiState.INITIALIZING
    private var welcomeStartedAt = 0L
    private val WELCOME_HOLD_DURATION = 8000L // 8 seconds for the full welcome sequence

    /**
     * Manages announcement delivery, routing state changes, and obstacle threat levels.
     * Evaluates active system decisions, navigation context, and route updates to structure
     * timely speech commands for the user.
     */
    fun process(
        decision: GuidanceDecision,
        memoryEvents: List<Pair<MemoryObservation, MemoryEvent>>,
        navigationContext: NavigationContext,
        routeEvents: List<Pair<RouteLandmark, RouteEvent>>,
        navigationState: LandmarkNavigationState,
        hybridState: HybridNavigationState? = null
    ): Pair<SpeechCommand?, VoiceMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        try {
            val obstaclePrefix = if (decision.highestThreatClassName != null) {
                val distance = decision.highestThreatDistance
                if (distance != null) {
                    if (distance < 1.0f) {
                        "${decision.highestThreatClassName} very close ahead."
                    } else {
                        val roundedDist = kotlin.math.round(distance).toInt()
                        "${decision.highestThreatClassName} ahead about $roundedDist meters."
                    }
                } else {
                    "${decision.highestThreatClassName} ahead."
                }
            } else {
                null
            }

            // =====================================================
            // Emergency STOP State Machine (Safety First)
            // =====================================================

            if (decision.action == GuidanceAction.STOP) {
                val isStopEntry = lastSpokenAction != GuidanceAction.STOP
                val timeSinceLastStop = currentTime - lastSpokenTimestamp
                
                val shouldSpeak = isStopEntry || (timeSinceLastStop > 10000L)

                if (!shouldSpeak) {
                    val duration = SystemClock.elapsedRealtime() - startTime
                    return Pair(null, VoiceMetadata(generatedCount, suppressedCount + 1, duration, true))
                }

                val obstacleName = decision.highestThreatClassName ?: "Obstacle"
                val stopText = if (isStopEntry) {
                    val dist = decision.highestThreatDistance
                    if (dist != null && dist < 1.0f) {
                        "$obstacleName very close ahead."
                    } else if (obstaclePrefix != null) {
                        "$obstaclePrefix Stop."
                    } else {
                        "$obstacleName ahead. Stop."
                    }
                } else {
                    "$obstacleName still ahead."
                }
                
                generatedCount++
                lastSpokenAction = GuidanceAction.STOP
                lastSpokenText = stopText
                lastSpokenTimestamp = currentTime

                if (sarthiState == SarthiState.WELCOME || sarthiState == SarthiState.READY) {
                    sarthiState = SarthiState.NAVIGATING
                }

                val command = SpeechCommand(
                    text = stopText,
                    action = GuidanceAction.STOP,
                    priority = 100, 
                    timestamp = currentTime
                )

                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(command, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            // =====================================================
            // Sarthi Startup Experience Logic
            // =====================================================

            if (sarthiState == SarthiState.INITIALIZING) {
                sarthiState = SarthiState.WELCOME
                welcomeStartedAt = currentTime
                
                val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
                val currentTimeString = timeFormat.format(Date())
                
                val welcomeText = "Hi, I am Sarthi. The current time is $currentTimeString. " +
                                 "I am ready to assist you. Please point your camera forward and begin walking."
                
                generatedCount++
                lastSpokenText = welcomeText
                lastSpokenTimestamp = currentTime
                
                val command = SpeechCommand(
                    text = welcomeText,
                    action = GuidanceAction.KEEP_CENTER,
                    priority = 100, 
                    timestamp = currentTime
                )
                
                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(command, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            if (sarthiState == SarthiState.WELCOME) {
                if (currentTime - welcomeStartedAt > WELCOME_HOLD_DURATION) {
                    sarthiState = SarthiState.READY
                } else {
                    val duration = SystemClock.elapsedRealtime() - startTime
                    return Pair(null, VoiceMetadata(generatedCount, suppressedCount + 1, duration, true))
                }
            }

            if (sarthiState == SarthiState.READY) {
                val hasActualRoute = navigationState.progress != NavigationProgress.SEARCHING && 
                                    navigationState.progress != NavigationProgress.LOST
                
                if (hasActualRoute || decision.action != GuidanceAction.KEEP_CENTER) {
                    sarthiState = SarthiState.NAVIGATING
                } else {
                    val duration = SystemClock.elapsedRealtime() - startTime
                    return Pair(null, VoiceMetadata(generatedCount, suppressedCount + 1, duration, true))
                }
            }

            // =====================================================
            // Local Guidance Merging (Obstacle + Direction)
            // =====================================================

            val guidanceText = when (decision.action) {
                GuidanceAction.MOVE_LEFT -> "Move left"
                GuidanceAction.MOVE_SLIGHTLY_LEFT -> "Move slightly left"
                GuidanceAction.MOVE_RIGHT -> "Move right"
                GuidanceAction.MOVE_SLIGHTLY_RIGHT -> "Move slightly right"
                GuidanceAction.KEEP_CENTER -> "Keep center"
                GuidanceAction.WAIT -> "Path blocked. Please wait"
                else -> ""
            }

            var text = if (obstaclePrefix != null) {
                if (decision.action == GuidanceAction.KEEP_CENTER || (decision.action == GuidanceAction.STOP && decision.highestThreatDistance != null && decision.highestThreatDistance < 1.0f)) {
                    obstaclePrefix
                } else {
                    "$obstaclePrefix $guidanceText"
                }
            } else {
                guidanceText
            }

            // Sequential Instruction Addition
            if (decision.secondaryAction != null && decision.secondaryReason != null) {
                text = "$text ${decision.secondaryReason}"
            }

            // Path Clear Exit Logic
            if (lastSpokenAction == GuidanceAction.STOP && decision.action != GuidanceAction.STOP) {
                text = "Path clear. $guidanceText"
            }

            val priority = when (decision.action) {
                GuidanceAction.MOVE_LEFT, GuidanceAction.MOVE_RIGHT -> 80
                GuidanceAction.MOVE_SLIGHTLY_LEFT, GuidanceAction.MOVE_SLIGHTLY_RIGHT -> 60
                GuidanceAction.WAIT -> 70
                else -> 40
            }

            if (text.isBlank()) {
                 val duration = SystemClock.elapsedRealtime() - startTime
                 return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            val isSameAction = decision.action == lastSpokenAction
            val isSameText = text == lastSpokenText
            val timeElapsed = currentTime - lastSpokenTimestamp
            
            // Aggressive suppression for steady-state instructions
            val cooldown = if (priority <= 40) 10000L else 3000L
            val shouldSuppress = isSameAction && isSameText && timeElapsed < cooldown

            if (shouldSuppress) {
                suppressedCount++
                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            generatedCount++
            lastSpokenAction = decision.action
            lastSpokenText = text
            lastSpokenTimestamp = currentTime

            val command = SpeechCommand(
                text = text,
                action = decision.action,
                priority = priority,
                timestamp = currentTime
            )

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(command, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, false, e.localizedMessage))
        }
    }
}
