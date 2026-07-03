package com.pathhelper.ai.onnx

import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.TtcRiskLevel
import com.pathhelper.ai.navigation.ThreatLevel
/**
* Represents the data structures or state of Detection Render Data.
*/
data
class DetectionRenderData(
    val trackId: Int,
    val classId: Int,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val horizontalZone: HorizontalZone? = null,
    val distanceMeters: Float? = null,
    val ttcSeconds: Float? = null,
    val riskLevel: TtcRiskLevel = TtcRiskLevel.SAFE,
    val priorityScore: Float = 0f,
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val threatRank: Int = Int.MAX_VALUE
)
