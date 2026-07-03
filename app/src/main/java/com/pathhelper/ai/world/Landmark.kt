package com.pathhelper.ai.world

import com.pathhelper.ai.navigation.HorizontalZone
/**
* Represents the data structures or state of Landmark.
*/
data
class Landmark(
    val id: String,
    val type: LandmarkType,
    val distanceMeters: Float?,
    val horizontalZone: HorizontalZone,
    val confidence: Float,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long
)
