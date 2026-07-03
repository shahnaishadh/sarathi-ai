package com.pathhelper.ai.environment

import com.pathhelper.ai.navigation.HorizontalZone
/**
* Represents the data structures or state of Environment Observation.
*/
data
class EnvironmentObservation(
    val type: EnvironmentType,
    val confidence: Float,
    val horizontalZone: HorizontalZone,
    val distanceMeters: Float?,
    val description: String,
    val trackId: Int? = null
)
