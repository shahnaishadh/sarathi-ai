package com.pathhelper.ai.navigation.hybrid

import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Entrance Transition operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Entrance Transition.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class EntranceTransition {
    fun evaluate(
        currentMode: NavigationMode,
        gpsDistance: Float,
        tracks: List<Track>
    ): NavigationMode {
        return when (currentMode) {
            NavigationMode.OUTDOOR -> {
                if (gpsDistance < 15.0f) {
                    NavigationMode.ENTRANCE_APPROACH
                } else {
                    NavigationMode.OUTDOOR
                }
            }
            NavigationMode.ENTRANCE_APPROACH -> {
                val doorDetected = tracks.any { track ->
                    val normX = track.centerX / 640.0f
                    val zone = when {
                        normX < 0.3333f -> HorizontalZone.LEFT
                        normX < 0.6666f -> HorizontalZone.CENTER
                        else -> HorizontalZone.RIGHT
                    }
                    val aspectVertical = if (track.width > 0) track.height / track.width else 0.0f
                    aspectVertical >= 1.4f && zone == HorizontalZone.CENTER && track.distanceMeters < 5.0f
                }
                if (doorDetected) {
                    NavigationMode.INDOOR
                } else {
                    NavigationMode.ENTRANCE_APPROACH
                }
            }
            else -> currentMode
        }
    }
}
