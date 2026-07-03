package com.pathhelper.ai.navigation.hybrid

import com.pathhelper.ai.navigation.common.target.NavigationTarget
/**
* Represents the data structures or state of Hybrid Navigation State.
*/
data
class HybridNavigationState(
    val currentMode: NavigationMode,
    val activeTarget: NavigationTarget,
    val currentInstruction: String,
    val outdoorProgress: Float,
    val indoorProgress: Float,
    val transitionStatus: String,
    val etaSeconds: Long,
    val completionPercentage: Float
)
