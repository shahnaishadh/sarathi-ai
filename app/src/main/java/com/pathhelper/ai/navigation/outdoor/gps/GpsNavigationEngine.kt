package com.pathhelper.ai.navigation.outdoor.gps

import android.location.Location
import android.os.SystemClock
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.GpsTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.NavigationStep
import com.pathhelper.ai.voice.SpeechCommand
import com.pathhelper.ai.haptics.HapticCommand
import com.pathhelper.ai.haptics.HapticPattern
/**
* Coordinates Gps Navigation Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Gps Navigation Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class GpsNavigationEngine {
    private var lastAnnouncedInstruction: String? = null
    private var lastAnnouncedProgress: NavigationProgress? = null

    fun navigate(
        target: NavigationTarget,
        lastLocation: Location?,
        activeWaypoint: Location?,
        deviationDetected: Boolean,
        recalculationsCount: Int
    ): Pair<GpsNavigationState, GpsNavigationMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        if (target !is GpsTarget) {
            val state = GpsNavigationState(target, NavigationProgress.SEARCHING, null, emptyList(), 0, null, null, null, "Searching for GPS Target...", null, null)
            val meta = GpsNavigationMetadata(lastLocation != null, lastLocation?.accuracy, 0, recalculationsCount, SystemClock.elapsedRealtime() - startTime, true)
            return Pair(state, meta)
        }

        if (lastLocation == null) {
            val state = GpsNavigationState(target, NavigationProgress.SEARCHING, null, emptyList(), 0, null, null, null, "Waiting for Location Fix...", null, null)
            val meta = GpsNavigationMetadata(false, null, 0, recalculationsCount, SystemClock.elapsedRealtime() - startTime, true)
            return Pair(state, meta)
        }

        if (activeWaypoint == null) {
            val state = GpsNavigationState(target, NavigationProgress.SEARCHING, null, emptyList(), 0, null, null, null, "Calculating Route...", null, null)
            val meta = GpsNavigationMetadata(true, lastLocation.accuracy, 0, recalculationsCount, SystemClock.elapsedRealtime() - startTime, true)
            return Pair(state, meta)
        }

        val distanceToFinal = calculateDistance(lastLocation.latitude, lastLocation.longitude, target.latitude, target.longitude)
        val bearingToNext = calculateBearing(lastLocation.latitude, lastLocation.longitude, activeWaypoint.latitude, activeWaypoint.longitude)
        val currentHeading = lastLocation.bearing

        val progress = when {
            distanceToFinal < 3.0f -> NavigationProgress.ARRIVED
            distanceToFinal < 15.0f -> NavigationProgress.APPROACHING
            deviationDetected -> NavigationProgress.LOST
            else -> NavigationProgress.ROUTE_FOUND
        }

        val turn = getTurnInstruction(currentHeading, bearingToNext)
        val stepText = when (progress) {
            NavigationProgress.ARRIVED -> "Arrived at destination."
            NavigationProgress.APPROACHING -> "Approaching destination. $turn."
            NavigationProgress.LOST -> "Recalculating route..."
            else -> "$turn, proceed ${Math.round(distanceToFinal)} meters."
        }

        var speechCommand: SpeechCommand? = null
        var hapticCommand: HapticCommand? = null

        if (progress != lastAnnouncedProgress || stepText != lastAnnouncedInstruction) {
            speechCommand = SpeechCommand(stepText, GuidanceAction.KEEP_CENTER, 50, currentTime)
            val pattern = when (progress) {
                NavigationProgress.ARRIVED -> HapticPattern.RIGHT
                NavigationProgress.LOST -> HapticPattern.WAIT
                else -> HapticPattern.CENTER
            }
            hapticCommand = HapticCommand(pattern, GuidanceAction.KEEP_CENTER, currentTime, 50)
            lastAnnouncedProgress = progress
            lastAnnouncedInstruction = stepText
        }

        val step = NavigationStep(stepText, 1.0f, currentTime)
        val state = GpsNavigationState(
            destination = target,
            progress = progress,
            currentStep = step,
            routePoints = listOf(activeWaypoint),
            currentPointIndex = 0,
            distanceToTargetMeters = distanceToFinal,
            bearingToTargetDegrees = bearingToNext,
            currentHeadingDegrees = currentHeading,
            currentInstruction = stepText,
            speechCommand = speechCommand,
            hapticCommand = hapticCommand
        )

        val extras = lastLocation.extras
        val satellites = if (extras?.containsKey("satellites") == true) extras.getInt("satellites") else 0

        val meta = GpsNavigationMetadata(
            hasLocation = true,
            accuracy = lastLocation.accuracy,
            satellitesCount = satellites,
            recalculationsCount = recalculationsCount,
            processingTimeMs = SystemClock.elapsedRealtime() - startTime,
            successful = true
        )

        return Pair(state, meta)
    }

    private fun getTurnInstruction(heading: Float, targetBearing: Float): String {
        var diff = targetBearing - heading
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f

        return when {
            diff < -30f -> "Sharp left"
            diff > 30f -> "Sharp right"
            diff < -5f -> "Turn slight left"
            diff > 5f -> "Turn slight right"
            else -> "Go straight"
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val y = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)

        val bearingRad = Math.atan2(y, x)
        var bearingDeg = Math.toDegrees(bearingRad).toFloat()
        return (bearingDeg + 360f) % 360f
    }
}
