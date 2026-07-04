package com.pathhelper.ai.navigation.coordinator

import android.content.Context
import android.location.Location
import android.util.Log
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.BuildingTarget
import com.pathhelper.ai.navigation.common.target.RoomTarget
import com.pathhelper.ai.navigation.common.target.GpsTarget
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationEngine
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationState
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationMetadata
import com.pathhelper.ai.navigation.outdoor.routing.OutdoorRouteEngine
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationEngine
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationState
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationMetadata
import com.pathhelper.ai.navigation.hybrid.HybridNavigationEngine
import com.pathhelper.ai.navigation.hybrid.HybridNavigationState
import com.pathhelper.ai.navigation.hybrid.HybridNavigationMetadata
import com.pathhelper.ai.navigation.hybrid.NavigationMode
import com.pathhelper.ai.navigation.hybrid.entrance.BuildingEntranceDetector
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceDecision
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceConfidence
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceMetadata
import com.pathhelper.ai.tracking.Track
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.navigation.graph.RoutePlan
import com.pathhelper.ai.voice.SpeechCommand
import com.pathhelper.ai.haptics.HapticCommand
/**
* Coordinates Navigation Coordinator operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Coordinator.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class NavigationCoordinator(
    private val context: Context,
    private val gpsNavigationEngine: GpsNavigationEngine,
    private val gpsRouteEngine: OutdoorRouteEngine,
    private val landmarkNavigationEngine: LandmarkNavigationEngine
) {
    private val hybridNavigationEngine = HybridNavigationEngine()
    private val buildingEntranceDetector = BuildingEntranceDetector()
    private var activeTarget: NavigationTarget? = null

    var lastGpsState: GpsNavigationState? = null
    var lastGpsMetadata: GpsNavigationMetadata? = null
    var lastLandmarkState: LandmarkNavigationState? = null
    var lastLandmarkMetadata: LandmarkNavigationMetadata? = null
    var lastHybridState: HybridNavigationState? = null
    var lastHybridMetadata: HybridNavigationMetadata? = null

    var lastEntranceDecision: EntranceDecision? = null
    var lastEntranceConfidence: EntranceConfidence? = null
    var lastEntranceMetadata: EntranceMetadata? = null

    var speechCommand: SpeechCommand? = null
    var hapticCommand: HapticCommand? = null

    fun setTarget(target: NavigationTarget) {
        activeTarget = target
        hybridNavigationEngine.reset()
    }

    fun getActiveTarget(): NavigationTarget? = activeTarget

    fun process(
        lastLocation: Location?,
        tracks: List<Track>,
        sceneMemory: SceneMemory,
        worldModel: WorldModel,
        navigationContext: NavigationContext,
        routeMemory: RouteMemory,
        routePlan: RoutePlan,
        frameId: Long = 0L
    ) {
        val target = activeTarget ?: return

        speechCommand = null
        hapticCommand = null

        val routeState = lastLocation?.let { gpsRouteEngine.updateRouteProgress(it) }
        val activeWaypoint = gpsRouteEngine.getActiveWaypoint()?.location
        val deviationDetected = routeState?.deviationDetected ?: false
        val recalculationsCount = gpsRouteEngine.getRecalculationsCount()

        val targetGps = when (target) {
            is BuildingTarget -> GpsTarget(target.entranceLatitude, target.entranceLongitude)
            is GpsTarget -> target
            else -> GpsTarget(37.7749, -122.4194)
        }

        val (gpsState, gpsMetadata) = gpsNavigationEngine.navigate(
            target = targetGps,
            lastLocation = lastLocation,
            activeWaypoint = activeWaypoint,
            deviationDetected = deviationDetected,
            recalculationsCount = recalculationsCount
        )
        lastGpsState = gpsState
        lastGpsMetadata = gpsMetadata

        val targetGoal = when (target) {
            is RoomTarget -> LandmarkTarget(com.pathhelper.ai.world.LandmarkType.DOOR)
            is LandmarkTarget -> target
            else -> LandmarkTarget(com.pathhelper.ai.world.LandmarkType.DOOR)
        }
        val (lmState, lmMetadata) = landmarkNavigationEngine.navigate(
            targetGoal, worldModel, navigationContext, routeMemory, routePlan
        )
        lastLandmarkState = lmState
        lastLandmarkMetadata = lmMetadata

        val gpsDistance = gpsState.distanceToTargetMeters ?: Float.MAX_VALUE
        val indoorReached = (lmState.progress == NavigationProgress.ROUTE_FOUND || lmState.progress == NavigationProgress.APPROACHING)

        val (decision, confidenceAndMeta) = buildingEntranceDetector.evaluate(
            tracks = tracks,
            sceneMemory = sceneMemory,
            worldModel = worldModel,
            gpsDistance = gpsDistance
        )
        val (confidence, entranceMeta) = confidenceAndMeta
        lastEntranceDecision = decision
        lastEntranceConfidence = confidence
        lastEntranceMetadata = entranceMeta

        val (hybridState, hybridMetadata) = hybridNavigationEngine.process(
            target = target,
            entranceDecision = decision,
            indoorReached = indoorReached
        )
        lastHybridState = hybridState
        lastHybridMetadata = hybridMetadata

        when (hybridState.currentMode) {
            NavigationMode.OUTDOOR -> {
                speechCommand = gpsState.speechCommand
                hapticCommand = gpsState.hapticCommand
            }
            NavigationMode.ENTRANCE_APPROACH -> {
                // Speech handled by VoiceGuidanceEngine to ensure suppression
                hapticCommand = HapticCommand(com.pathhelper.ai.haptics.HapticPattern.CENTER, com.pathhelper.ai.navigation.GuidanceAction.KEEP_CENTER, System.currentTimeMillis(), 50)
            }
            NavigationMode.INDOOR -> {
                // Rely on indoor voice/haptic commands
            }
            NavigationMode.ARRIVED -> {
                // Speech handled by VoiceGuidanceEngine to ensure suppression
                hapticCommand = HapticCommand(com.pathhelper.ai.haptics.HapticPattern.RIGHT, com.pathhelper.ai.navigation.GuidanceAction.KEEP_CENTER, System.currentTimeMillis(), 50)
            }
        }
        Log.i("SARTHI_DEBUG", """
            [NAVIGATION_COORDINATOR]
            time=${System.currentTimeMillis()}
            frameId=$frameId
            activeTarget=${activeTarget?.javaClass?.simpleName ?: "null"}
            hybridMode=${hybridState.currentMode}
        """.trimIndent())
    }
}
