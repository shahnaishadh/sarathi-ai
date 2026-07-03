package com.pathhelper.ai.navigation.landmark

import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.NavigationStep
/**
* Represents the data structures or state of Landmark Navigation State.
*/
data
class LandmarkNavigationState(
    val destination: NavigationTarget,
    val progress: NavigationProgress,
    val currentStep: NavigationStep?,
    val completedSteps: List<NavigationStep>,
    val remainingDistanceMeters: Float?
)
