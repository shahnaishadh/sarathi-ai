package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.graph.RouteGraph
import com.pathhelper.ai.world.WorldModel

/**
 * LocalizationProvider is the sole abstraction boundary between the
 * Localization Domain and Navigation Domain.
 *
 * NavigationCoordinator → LocalizationProvider → LocalizedPosition  ✅
 * NavigationCoordinator → IndoorLocalizationEngine                  ❌ forbidden
 */
class LocalizationProvider {

    private val engine = IndoorLocalizationEngine()

    private var lastPosition: LocalizedPosition? = null
    private var lastConfidence: LocalizationConfidence = LocalizationConfidence.ZERO
    private var lastMetadata: LocalizationMetadata? = null

    fun update(
        pose: PoseEstimate,
        sceneMemory: SceneMemory,
        worldModel: WorldModel,
        routeGraph: RouteGraph
    ) {
        val (position, confidence, metadata) = engine.process(pose, sceneMemory, worldModel, routeGraph)
        lastPosition = position
        lastConfidence = confidence
        lastMetadata = metadata
    }

    fun getPosition(): LocalizedPosition? = lastPosition
    fun getConfidence(): LocalizationConfidence = lastConfidence
    fun getMetadata(): LocalizationMetadata? = lastMetadata

    fun reset() {
        engine.reset()
        lastPosition = null
        lastConfidence = LocalizationConfidence.ZERO
        lastMetadata = null
    }
}
