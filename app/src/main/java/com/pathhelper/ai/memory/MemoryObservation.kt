package com.pathhelper.ai.memory

import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.navigation.HorizontalZone
/**
* Represents the data structures or state of Memory Observation.
*/
data
class MemoryObservation(
    val id: String,
    val trackId: Int?,
    val type: EnvironmentType,
    val firstSeenTimestamp: Long,
    var lastSeenTimestamp: Long,
    var confidence: Float,
    var distanceMeters: Float?,
    val horizontalZone: HorizontalZone,
    val description: String
)
