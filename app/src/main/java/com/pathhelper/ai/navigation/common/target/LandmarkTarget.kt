package com.pathhelper.ai.navigation.common.target

import com.pathhelper.ai.world.LandmarkType
/**
* Represents the data structures or state of Landmark Target.
*/
data
class LandmarkTarget(
    val landmarkType: LandmarkType
) : NavigationTarget
