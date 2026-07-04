package com.pathhelper.ai.voice

import android.os.SystemClock
import android.util.Log
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
    private var lastSpokenThreatId: Int? = null
    private var lastSpokenDistance: Float? = null
    private var lastSpokenThreatLevel: com.pathhelper.ai.navigation.ThreatLevel? = null

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

    private var lastPoseX = 0f
    private var lastPoseY = 0f
    private var stationaryStartTime: Long = 0L
    private var lastInactivityNotificationTime: Long = 0L
    private var isFirstPose = true

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
        hybridState: HybridNavigationState? = null,
        frameId: Long = 0L,
        isSpeaking: Boolean = false,
        poseX: Float = 0f,
        poseY: Float = 0f
    ): Pair<SpeechCommand?, VoiceMetadata> {
        val result = processInternal(decision, memoryEvents, navigationContext, routeEvents, navigationState, hybridState, isSpeaking, poseX, poseY)
        val cmd = result.first
        Log.i("SARTHI_DEBUG", """
            [VOICE_GUIDANCE]
            time=${System.currentTimeMillis()}
            frameId=$frameId
            action=${decision.action}
            spokenText=${cmd?.text?.let { "\"$it\"" } ?: "null"}
            suppressed=${cmd == null}
        """.trimIndent())
        return result
    }

    private fun processInternal(
        decision: GuidanceDecision,
        memoryEvents: List<Pair<MemoryObservation, MemoryEvent>>,
        navigationContext: NavigationContext,
        routeEvents: List<Pair<RouteLandmark, RouteEvent>>,
        navigationState: LandmarkNavigationState,
        hybridState: HybridNavigationState?,
        isSpeaking: Boolean = false,
        poseX: Float = 0f,
        poseY: Float = 0f
    ): Pair<SpeechCommand?, VoiceMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        if (isSpeaking) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(null, VoiceMetadata(generatedCount, suppressedCount + 1, duration, true))
        }

        try {
            // Track active threat state
            Log.i("SARTHI_THREAT_STATE", "trackerId=${decision.highestThreatId ?: "null"} distance=${decision.highestThreatDistance ?: "null"} threatLevel=${decision.highestThreatLevel ?: "null"}")

            // Inactivity Tracking
            if (isFirstPose) {
                lastPoseX = poseX
                lastPoseY = poseY
                stationaryStartTime = currentTime
                isFirstPose = false
            }

            val distanceMoved = kotlin.math.sqrt(
                (poseX - lastPoseX) * (poseX - lastPoseX) +
                (poseY - lastPoseY) * (poseY - lastPoseY)
            )

            if (distanceMoved > 0.3f) {
                stationaryStartTime = currentTime
                lastPoseX = poseX
                lastPoseY = poseY
            }

            val timeStationary = currentTime - stationaryStartTime
            var triggerNotification = false

            if (timeStationary >= 15000L && (currentTime - lastInactivityNotificationTime >= 45000L)) {
                triggerNotification = true
                lastInactivityNotificationTime = currentTime
            }

            Log.i("SARTHI_INACTIVITY", "timeStationary=$timeStationary notificationTriggered=$triggerNotification")

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
                    Log.i("SARTHI_SUPPRESSION", "reason=SUPPRESSED_STOP_COOLDOWN")
                    val duration = SystemClock.elapsedRealtime() - startTime
                    return Pair(null, VoiceMetadata(generatedCount, suppressedCount + 1, duration, true))
                }

                val obstacleName = decision.highestThreatClassName ?: "Obstacle"
                val directionAdvice = decision.secondaryReason ?: ""

                val stopText = if (isStopEntry) {
                    val dist = decision.highestThreatDistance
                    if (dist != null && dist < 1.0f) {
                        val base = "$obstacleName very close ahead."
                        if (directionAdvice.isNotEmpty()) "$base $directionAdvice" else base
                    } else if (obstaclePrefix != null) {
                        if (directionAdvice.isNotEmpty()) "$obstaclePrefix $directionAdvice" else "$obstaclePrefix Stop."
                    } else {
                        val base = "$obstacleName ahead. Stop."
                        if (directionAdvice.isNotEmpty()) "$base $directionAdvice" else base
                    }
                } else {
                    val base = if (decision.highestThreatDistance != null) {
                        val roundedDist = kotlin.math.round(decision.highestThreatDistance).toInt()
                        "$obstacleName ahead about $roundedDist meters."
                    } else {
                        "$obstacleName still ahead."
                    }
                    if (directionAdvice.isNotEmpty()) "$base $directionAdvice" else "$base Stop."
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

                Log.i("SARTHI_VOICE_GUIDANCE", "action=${command.action} timestamp=${System.currentTimeMillis()}")

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
                
                Log.i("SARTHI_STARTUP", "time=${System.currentTimeMillis()} startupComplete=true")
                
                val command = SpeechCommand(
                    text = welcomeText,
                    action = GuidanceAction.KEEP_CENTER,
                    priority = 100, 
                    timestamp = currentTime
                )
                
                Log.i("SARTHI_VOICE_GUIDANCE", "action=${command.action} timestamp=${System.currentTimeMillis()}")

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

            // Inactivity Announcement
            if (triggerNotification) {
                val text = "Looks like you are standing in the same position."
                generatedCount++
                lastSpokenAction = GuidanceAction.KEEP_CENTER
                lastSpokenText = text
                lastSpokenTimestamp = currentTime

                val command = SpeechCommand(
                    text = text,
                    action = GuidanceAction.KEEP_CENTER,
                    priority = 50,
                    timestamp = currentTime
                )

                Log.i("SARTHI_VOICE_GUIDANCE", "action=${command.action} timestamp=${System.currentTimeMillis()}")
                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(command, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            // =====================================================
            // Local Guidance Merging (Obstacle + Direction)
            // =====================================================

            val guidanceText = when (decision.action) {
                GuidanceAction.MOVE_LEFT -> "Turn slight left"
                GuidanceAction.MOVE_SLIGHTLY_LEFT -> "Turn slight left"
                GuidanceAction.MOVE_SHARP_LEFT -> "Sharp left"
                GuidanceAction.MOVE_RIGHT -> "Turn slight right"
                GuidanceAction.MOVE_SLIGHTLY_RIGHT -> "Turn slight right"
                GuidanceAction.MOVE_SHARP_RIGHT -> "Sharp right"
                GuidanceAction.KEEP_CENTER -> "Keep center"
                GuidanceAction.WAIT -> "Path blocked. Please wait"
                else -> ""
            }

            var text = if (decision.action == GuidanceAction.KEEP_CENTER) {
                if (lastSpokenAction != GuidanceAction.KEEP_CENTER) {
                    "Keep center"
                } else {
                    ""
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
                GuidanceAction.MOVE_SHARP_LEFT, GuidanceAction.MOVE_SHARP_RIGHT -> 90
                GuidanceAction.MOVE_LEFT, GuidanceAction.MOVE_RIGHT -> 80
                GuidanceAction.WAIT -> 70
                GuidanceAction.MOVE_SLIGHTLY_LEFT, GuidanceAction.MOVE_SLIGHTLY_RIGHT -> 60
                else -> 40
            }

            if (text.isBlank()) {
                 val duration = SystemClock.elapsedRealtime() - startTime
                 return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            val isSameAction = decision.action == lastSpokenAction
            val isSameText = text == lastSpokenText
            val isSameThreat = decision.highestThreatId != null && decision.highestThreatId == lastSpokenThreatId
            val isSameLevel = decision.highestThreatLevel == lastSpokenThreatLevel
            
            // Distance delta suppression (prevent jitter between e.g. 2m and 3m)
            val distanceDelta = if (decision.highestThreatDistance != null && lastSpokenDistance != null) {
                kotlin.math.abs(decision.highestThreatDistance - lastSpokenDistance!!)
            } else {
                Float.MAX_VALUE
            }
            val isDistanceUnchanged = distanceDelta < 1.5f

            val shouldSuppress = if (decision.action == GuidanceAction.STOP) {
                // Keep safety-critical repeating for STOP action (10-second repeating cooldown)
                val isStopEntry = lastSpokenAction != GuidanceAction.STOP
                val timeSinceLastStop = currentTime - lastSpokenTimestamp
                !(isStopEntry || (timeSinceLastStop > 10000L))
            } else {
                // State-based suppression: suppress if threat state, action, level, and distance are unchanged
                (isSameAction && isSameText && isSameThreat && isSameLevel && isDistanceUnchanged) ||
                // Or if there is no threat, and the guidance action hasn't changed (avoid repeating standard info like KEEP_CENTER)
                (decision.highestThreatId == null && lastSpokenThreatId == null && isSameAction)
            }

            if (shouldSuppress) {
                val reason = when {
                    decision.action == GuidanceAction.STOP -> "SUPPRESSED_STOP_COOLDOWN"
                    isSameThreat && isSameAction && isSameLevel && isDistanceUnchanged -> "SUPPRESSED_SAME_THREAT"
                    isSameAction -> "SUPPRESSED_SAME_GUIDANCE"
                    isDistanceUnchanged -> "SUPPRESSED_DISTANCE_UNCHANGED"
                    else -> "SUPPRESSED_STATE_UNCHANGED"
                }
                Log.i("SARTHI_SUPPRESSION", "reason=$reason")

                suppressedCount++
                val duration = SystemClock.elapsedRealtime() - startTime
                return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            }

            generatedCount++
            lastSpokenAction = decision.action
            lastSpokenText = text
            lastSpokenTimestamp = currentTime
            lastSpokenThreatId = decision.highestThreatId
            lastSpokenDistance = decision.highestThreatDistance
            lastSpokenThreatLevel = decision.highestThreatLevel

            val command = SpeechCommand(
                text = text,
                action = decision.action,
                priority = priority,
                timestamp = currentTime
            )

            Log.i("SARTHI_VOICE_GUIDANCE", "action=${command.action} timestamp=${System.currentTimeMillis()}")

            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(command, VoiceMetadata(generatedCount, suppressedCount, duration, true))
            
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(null, VoiceMetadata(generatedCount, suppressedCount, duration, false, e.localizedMessage))
        }
    }
}
