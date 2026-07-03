package com.pathhelper.ai.navigation.outdoor.gps

import android.location.Location
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.NavigationStep
import com.pathhelper.ai.voice.SpeechCommand
import com.pathhelper.ai.haptics.HapticCommand
/**
* Represents the data structures or state of Gps Navigation State.
*/
data
class GpsNavigationState(
    val destination: NavigationTarget,
    val progress: NavigationProgress,
    val currentStep: NavigationStep?,
    val routePoints: List<Location>,
    val currentPointIndex: Int,
    val distanceToTargetMeters: Float?,
    val bearingToTargetDegrees: Float?,
    val currentHeadingDegrees: Float?,
    val currentInstruction: String?,
    val speechCommand: SpeechCommand?,
    val hapticCommand: HapticCommand?
)
