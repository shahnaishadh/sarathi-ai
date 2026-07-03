package com.pathhelper.ai.navigation

import com.pathhelper.ai.perception.SegmentationResult

/**
 * Fuses object-level threats (YOLO) with structural scene data (Semantic Segmentation).
 */
class NavigationFusionEngine {

    fun fuse(
        threats: List<ThreatPriority>,
        structuralCorridors: List<SafeCorridor>,
        segmentation: SegmentationResult? = null
    ): List<SafeCorridor> {
        
        return structuralCorridors.map { corridor ->
            val overlappingThreat = threats.find { it.horizontalZone == corridor.horizontalZone }
            
            // Structural check: If segmentation says there is no floor here, it's BLOCKED
            val isWall = segmentation?.let { /* check wall density in zone */ false } ?: false
            
            if (isWall) {
                corridor.copy(state = CorridorState.BLOCKED, highestThreatLevel = ThreatLevel.CRITICAL)
            } else if (overlappingThreat != null && overlappingThreat.threatLevel == ThreatLevel.CRITICAL) {
                corridor.copy(state = CorridorState.BLOCKED)
            } else {
                corridor
            }
        }
    }
}
