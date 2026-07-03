package com.pathhelper.ai.tracking

import com.pathhelper.ai.navigation.TtcRiskLevel
import com.pathhelper.ai.navigation.ThreatLevel
import com.pathhelper.ai.navigation.HorizontalZone
/**
* Represents the data structures or state of Track.
*/
data
class Track(
    val id: Int,
    val classId: Int,
    val centerX: Float,
    val centerY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val age: Int,
    val missedFrames: Int,
    val confidence: Float,
    val width: Float = 0f,
    val height: Float = 0f,
    var distanceMeters: Float = 0f,
    var previousDistanceMeters: Float = 0f,
    var distanceVelocityMetersPerSecond: Float = 0f,
    var ttcSeconds: Float? = null,
    var riskLevel: TtcRiskLevel = TtcRiskLevel.SAFE,
    var priorityScore: Float = 0f,
    var threatLevel: ThreatLevel = ThreatLevel.LOW,
    var threatRank: Int = Int.MAX_VALUE,
    var assignedCorridor: HorizontalZone? = null
)
