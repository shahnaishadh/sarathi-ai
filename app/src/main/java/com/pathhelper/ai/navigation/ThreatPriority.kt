package com.pathhelper.ai.navigation

/**
 * Represents a semantic threat detected in the environment.
 * Preserves
object identity and spatial context throughout the pipeline.
 */
data
class ThreatPriority(
    val trackId: Int,
    val classId: Int,
    val className: String,
    val confidence: Float,
    val horizontalZone: HorizontalZone,
    val distanceMeters: Float,
    val ttcSeconds: Float?,
    val riskLevel: TtcRiskLevel,
    val priorityScore: Float,
    val threatLevel: ThreatLevel,
    val rank: Int
)
