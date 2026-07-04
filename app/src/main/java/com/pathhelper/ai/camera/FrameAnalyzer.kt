package com.pathhelper.ai.camera

import android.content.Context
import android.location.Location
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pathhelper.ai.onnx.DetectionMetadata
import com.pathhelper.ai.onnx.InferenceMetadata
import com.pathhelper.ai.onnx.LetterboxResult
import com.pathhelper.ai.onnx.NmsMetadata
import com.pathhelper.ai.onnx.TensorMetadata
import com.pathhelper.ai.onnx.YoloDetectionParser
import com.pathhelper.ai.onnx.YoloInferenceEngine
import com.pathhelper.ai.onnx.YoloNmsProcessor
import com.pathhelper.ai.onnx.YoloPreprocessor
import com.pathhelper.ai.tracking.Track
import com.pathhelper.ai.tracking.TrackManager
import com.pathhelper.ai.tracking.TrackMetadata
import com.pathhelper.ai.navigation.RelativePosition
import com.pathhelper.ai.navigation.RelativePositionEngine
import com.pathhelper.ai.navigation.RelativePositionMetadata
import com.pathhelper.ai.navigation.DistanceEstimate
import com.pathhelper.ai.navigation.DistanceEstimationEngine
import com.pathhelper.ai.navigation.DistanceMetadata
import com.pathhelper.ai.navigation.TtcEstimate
import com.pathhelper.ai.navigation.TtcMetadata
import com.pathhelper.ai.navigation.TtcEngine
import com.pathhelper.ai.navigation.NavigationFusionEngine
import com.pathhelper.ai.navigation.ThreatPriority
import com.pathhelper.ai.navigation.ThreatMetadata
import com.pathhelper.ai.navigation.ObstaclePrioritizationEngine
import com.pathhelper.ai.navigation.SafeCorridor
import com.pathhelper.ai.navigation.CorridorMetadata
import com.pathhelper.ai.navigation.SafeCorridorEngine
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.CorridorState
import com.pathhelper.ai.navigation.ThreatLevel
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.GuidanceDecision
import com.pathhelper.ai.navigation.GuidanceMetadata
import com.pathhelper.ai.navigation.GuidanceDecisionEngine
import com.pathhelper.ai.voice.SpeechCommand
import com.pathhelper.ai.voice.VoiceMetadata
import com.pathhelper.ai.voice.VoiceGuidanceEngine
import com.pathhelper.ai.haptics.HapticCommand
import com.pathhelper.ai.haptics.HapticMetadata
import com.pathhelper.ai.haptics.HapticGuidanceEngine
import com.pathhelper.ai.haptics.HapticPattern
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.environment.EnvironmentObservation
import com.pathhelper.ai.environment.EnvironmentMetadata
import com.pathhelper.ai.environment.EnvironmentUnderstandingEngine
import com.pathhelper.ai.memory.MemoryObservation
import com.pathhelper.ai.memory.MemoryEvent
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.memory.MemoryMetadata
import com.pathhelper.ai.memory.SceneMemoryEngine
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.WorldModelMetadata
import com.pathhelper.ai.world.WorldModelEngine
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.context.NavigationContextMetadata
import com.pathhelper.ai.context.NavigationContextEngine
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.route.RouteMetadata
import com.pathhelper.ai.route.RouteMemoryEngine
import com.pathhelper.ai.route.RouteEvent
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import com.pathhelper.ai.navigation.common.target.LocationTarget
import com.pathhelper.ai.navigation.common.target.GpsTarget
import com.pathhelper.ai.navigation.common.target.BuildingTarget
import com.pathhelper.ai.navigation.common.target.RoomTarget
import com.pathhelper.ai.navigation.common.target.NavigationStep
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationState
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationMetadata
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationEngine
import com.pathhelper.ai.navigation.graph.RouteGraph
import com.pathhelper.ai.navigation.graph.RouteGraphMetadata
import com.pathhelper.ai.navigation.graph.RoutePlan
import com.pathhelper.ai.navigation.graph.RouteGraphBuilder
import com.pathhelper.ai.navigation.graph.RoutePlanner
import com.pathhelper.ai.navigation.persistence.PersistentMap
import com.pathhelper.ai.navigation.persistence.MapMetadata
import com.pathhelper.ai.navigation.persistence.MapRepository
import com.pathhelper.ai.navigation.persistence.MapLearningEngine
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationState
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationMetadata
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationEngine
import com.pathhelper.ai.navigation.outdoor.gps.AndroidLocationProvider
import com.pathhelper.ai.navigation.common.analytics.GpsAnalytics
import com.pathhelper.ai.navigation.outdoor.routing.OpenStreetMapRouteProvider
import com.pathhelper.ai.navigation.outdoor.routing.OutdoorRouteEngine
import com.pathhelper.ai.navigation.hybrid.HybridNavigationState
import com.pathhelper.ai.navigation.hybrid.HybridNavigationMetadata
import com.pathhelper.ai.navigation.hybrid.HybridNavigationEngine
import com.pathhelper.ai.navigation.hybrid.NavigationMode
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceDecision
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceConfidence
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceMetadata
import com.pathhelper.ai.navigation.coordinator.NavigationCoordinator
import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.localization.slam.SlamMap
import com.pathhelper.ai.localization.slam.SlamMetadata
import com.pathhelper.ai.localization.slam.VisualSlamEngine
import com.pathhelper.ai.localization.pose.LocalizedPosition
import com.pathhelper.ai.localization.pose.LocalizationConfidence
import com.pathhelper.ai.localization.pose.LocalizationMetadata
import com.pathhelper.ai.localization.pose.LocalizationState
import com.pathhelper.ai.localization.pose.LocalizationProvider

/**
 * Orchestrates the end-to-end vision and navigation processing pipeline for each camera frame.
 *
 * This class implements the [ImageAnalysis.Analyzer]
interface to receive raw camera frames. 
 * It coordinates a complex sequence of AI-related tasks including:
 * - Image format conversion and preprocessing.
 * - Object detection and tracking using ONNX-based YOLO models.
 * - Spatial awareness and threat prioritization.
 * - Simultaneous Localization and Mapping (SLAM) and indoor localization.
 * - Navigation planning and hybrid outdoor/indoor guidance coordination.
 * - Generation of voice and haptic feedback commands.
 *
 * Role in Sarthi: Acts as the central "brain" of the real-time processing loop, 
 * fusing multiple streams of AI perception data into actionable guidance for the user.
 */
class FrameAnalyzer(
    private val context: Context,
    private val onFrameProcessed: (
        AnalysisStats,
        FrameMetadata,
        BitmapMetadata,
        TensorMetadata,
        LetterboxResult?,
        InferenceMetadata,
        DetectionMetadata,
        NmsMetadata,
        List<Track>,
        TrackMetadata,
        List<RelativePosition>,
        RelativePositionMetadata,
        List<DistanceEstimate>,
        DistanceMetadata,
        List<TtcEstimate>,
        TtcMetadata,
        List<ThreatPriority>,
        ThreatMetadata,
        List<SafeCorridor>,
        CorridorMetadata,
        GuidanceDecision,
        GuidanceMetadata,
        List<EnvironmentObservation>,
        EnvironmentMetadata,
        SceneMemory,
        MemoryMetadata,
        WorldModel,
        WorldModelMetadata,
        NavigationContext,
        NavigationContextMetadata,
        RouteMemory,
        RouteMetadata,
        RouteGraph,
        RouteGraphMetadata,
        RoutePlan,
        PersistentMap?,
        MapMetadata,
        LandmarkNavigationState,
        LandmarkNavigationMetadata,
        GpsNavigationState,
        GpsNavigationMetadata,
        HybridNavigationState,
        HybridNavigationMetadata,
        EntranceDecision?,
        EntranceConfidence?,
        EntranceMetadata?,
        PoseEstimate,
        SlamMap,
        SlamMetadata,
        LocalizedPosition,
        LocalizationConfidence,
        LocalizationMetadata,
        SpeechCommand?,
        VoiceMetadata,
        HapticCommand?,
        HapticMetadata
    ) -> Unit
) : ImageAnalysis.Analyzer {
    private var cameraController: CameraController? = null
    private var frameCount = 0L
    private var lastFpsTimestamp = 0L
    private var lastFrameTimestamp = 0L
    private var fpsFrameCount = 0
    private var currentFps = 0.0
    private val bitmapConverter = BitmapConverter()
    private val yoloPreprocessor = YoloPreprocessor()
    private val yoloInferenceEngine = YoloInferenceEngine()
    private val yoloDetectionParser = YoloDetectionParser()
    private val yoloNmsProcessor = YoloNmsProcessor()
    private val trackManager = TrackManager()
    private val relativePositionEngine = RelativePositionEngine()
    private val distanceEstimationEngine = DistanceEstimationEngine()
    private val ttcEngine = TtcEngine()
    private val obstaclePrioritizationEngine = ObstaclePrioritizationEngine()
    private val safeCorridorEngine = SafeCorridorEngine()
    private val guidanceDecisionEngine = GuidanceDecisionEngine()
    private val voiceGuidanceEngine = VoiceGuidanceEngine()
    private val hapticGuidanceEngine = HapticGuidanceEngine()
    private val navigationFusionEngine = NavigationFusionEngine()
    private val environmentUnderstandingEngine = EnvironmentUnderstandingEngine()
    private val sceneMemoryEngine = SceneMemoryEngine()
    private val worldModelEngine = WorldModelEngine()
    private val navigationContextEngine = NavigationContextEngine()
    private val routeMemoryEngine = RouteMemoryEngine()
    private val landmarkNavigationEngine = LandmarkNavigationEngine()
    private val routeGraphBuilder = RouteGraphBuilder()
    private val routePlanner = RoutePlanner()
    private val mapRepository = MapRepository(context)
    private val mapLearningEngine = MapLearningEngine()
    private val visualSlamEngine = VisualSlamEngine()
    private val localizationProvider = LocalizationProvider()

    // GPS & Routing instances
    private val gpsLocationProvider = AndroidLocationProvider(context)
    private val gpsAnalytics = GpsAnalytics()
    private val osmRouteProvider = OpenStreetMapRouteProvider()
    private val gpsRouteEngine = OutdoorRouteEngine(osmRouteProvider)
    private val gpsNavigationEngine = GpsNavigationEngine()

    private val targetGoal: NavigationTarget = LandmarkTarget(LandmarkType.ELEVATOR)
    private val gpsTarget: NavigationTarget = BuildingTarget("office_building", 37.7749, -122.4194)
    private val activeMapId = "main_indoor_map"
    private var lastPrimaryLandmarkId: String? = null
    private var lastNavigationProgress: NavigationProgress? = null

    // Coordinator instance
    private val navigationCoordinator = NavigationCoordinator(
        context = context,
        gpsNavigationEngine = gpsNavigationEngine,
        gpsRouteEngine = gpsRouteEngine,
        landmarkNavigationEngine = landmarkNavigationEngine
    )

    fun setCameraController(controller: CameraController) {
        this.cameraController = controller
    }

    init {
        navigationCoordinator.setTarget(gpsTarget)

        gpsLocationProvider.startTracking { location ->
            gpsAnalytics.logLocationUpdate(location)
            
            gpsRouteEngine.updateRouteProgress(location)

            if (gpsRouteEngine.isRouteEmpty()) {
                val targetGps = when (val target = navigationCoordinator.getActiveTarget() ?: gpsTarget) {
                    is BuildingTarget -> Location("gps_target").apply {
                        latitude = target.entranceLatitude
                        longitude = target.entranceLongitude
                    }
                    is GpsTarget -> Location("gps_target").apply {
                        latitude = target.latitude
                        longitude = target.longitude
                    }
                    else -> Location("gps_target").apply {
                        latitude = 37.7749
                        longitude = -122.4194
                    }
                }
                gpsRouteEngine.requestRoute(location, targetGps) {}
            }
        }
    }

    /**
     * Primary entry point for camera frame processing.
     *
     * This method executes the multi-stage AI pipeline, including
object detection, 
     * spatial mapping, and navigation logic. It ensures that every frame is processed 
     * within the constraints of mobile real-time performance and provides the results 
     * to the UI through the [onFrameProcessed] callback.
     *
     * @param image The raw camera frame provided by CameraX.
     */
    override fun analyze(image: ImageProxy) {
        val currentTime = SystemClock.elapsedRealtime()
        try {
            frameCount++
            fpsFrameCount++

            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = currentTime
            } else {
                val delta = currentTime - lastFpsTimestamp
                if (delta >= 1000) {
                    currentFps = (fpsFrameCount.toDouble() * 1000.0) / delta
                    fpsFrameCount = 0
                    lastFpsTimestamp = currentTime
                }
            }

            val deltaTimeSeconds = if (lastFrameTimestamp == 0L) {
                0.0333f
            } else {
                (currentTime - lastFrameTimestamp) / 1000.0f
            }
            lastFrameTimestamp = currentTime

            val planes = image.planes
            val yRowStride = if (planes.isNotEmpty()) planes[0].rowStride else 0
            val uRowStride = if (planes.size > 1) planes[1].rowStride else 0
            val vRowStride = if (planes.size > 2) planes[2].rowStride else 0
            val uPixelStride = if (planes.size > 1) planes[1].pixelStride else 0
            val vPixelStride = if (planes.size > 2) planes[2].pixelStride else 0

            val metadata = FrameMetadata(
                width = image.width,
                height = image.height,
                rotationDegrees = image.imageInfo.rotationDegrees,
                imageFormat = image.format,
                yRowStride = yRowStride,
                uRowStride = uRowStride,
                vRowStride = vRowStride,
                uPixelStride = uPixelStride,
                vPixelStride = vPixelStride
            )

            val stats = AnalysisStats(
                framesReceived = frameCount,
                fps = currentFps
            )

            val conversionResult = bitmapConverter.imageProxyToBitmap(image)
            val bitmap = conversionResult.first
            val bitmapMetadata = conversionResult.second

            // Low light detection and flash control
            if (bitmapMetadata.luminance < 20f) {
                cameraController?.setFlash(true)
            } else if (bitmapMetadata.luminance > 40f) {
                cameraController?.setFlash(false)
            }

            var tensorMetadata = TensorMetadata(
                tensorCreated = false,
                shape = longArrayOf(1, 3, 640, 640),
                preprocessingTimeMs = 0L,
                errorMessage = "Bitmap generation failed."
            )
            var lastLetterbox: LetterboxResult? = null
            var inferenceMetadata = InferenceMetadata(
                inferenceSuccessful = false,
                outputName = "unknown",
                outputShape = longArrayOf(),
                inferenceTimeMs = 0L,
                errorMessage = "Preprocessing not completed."
            )
            var detectionMetadata = DetectionMetadata(
                parsingSuccessful = false,
                totalCandidates = 0,
                maxConfidence = 0.0f,
                averageConfidence = 0.0f,
                parsingTimeMs = 0L,
                tensorShape = longArrayOf(),
                errorMessage = "Inference not completed."
            )
            var nmsMetadata = NmsMetadata(
                filteringSuccessful = false,
                candidateCount = 0,
                confidenceFilteredCount = 0,
                finalDetectionCount = 0,
                maxConfidence = 0.0f,
                nmsTimeMs = 0L,
                errorMessage = "Parsing not completed."
            )
            var trackMetadata = TrackMetadata(
                activeTracks = 0,
                newTracksCreated = 0,
                removedTracks = 0,
                processingTimeMs = 0L,
                trackingSuccessful = false,
                errorMessage = "NMS filtering not completed."
            )
            var relativePositionMetadata = RelativePositionMetadata(
                trackingObjects = 0,
                processedObjects = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Tracking not completed."
            )
            var distanceMetadata = DistanceMetadata(
                processedTracks = 0,
                estimatedTracks = 0,
                averageDistanceMeters = 0.0f,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Spatial Awareness not completed."
            )
            var ttcMetadata = TtcMetadata(
                processedTracks = 0,
                validTtcCount = 0,
                criticalRiskCount = 0,
                averageTtcSeconds = 0.0f,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Distance Estimation not completed."
            )
            var threatMetadata = ThreatMetadata(
                processedTracks = 0,
                rankedThreats = 0,
                criticalThreats = 0,
                highestPriorityScore = 0.0f,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Time-To-Collision not completed."
            )
            var corridorMetadata = CorridorMetadata(
                analyzedTracks = 0,
                safeCorridors = 3,
                blockedCorridors = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Obstacle Prioritization not completed."
            )
            var guidanceMetadata = GuidanceMetadata(
                evaluatedThreats = 0,
                evaluatedCorridors = 3,
                decisionTimeMs = 0L,
                successful = false,
                errorMessage = "Safe Corridor not completed."
            )
            var environmentMetadata = EnvironmentMetadata(
                observations = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Corridor calculation not completed."
            )
            var memoryMetadata = MemoryMetadata(
                activeObservations = 0,
                newObservations = 0,
                updatedObservations = 0,
                expiredObservations = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Environment understanding not completed."
            )
            var worldModelMetadata = WorldModelMetadata(
                landmarks = 0,
                relations = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Scene memory update not completed."
            )
            var navigationContextMetadata = NavigationContextMetadata(
                processedLandmarks = 0,
                prioritizedLandmarks = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "World model building not completed."
            )
            var routeMetadata = RouteMetadata(
                trackedLandmarks = 0,
                passedLandmarks = 0,
                revisitedLandmarks = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Navigation context analysis not completed."
            )
            var routeGraphMetadata = RouteGraphMetadata(
                nodeCount = 0,
                edgeCount = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Route memory updating not completed."
            )
            var mapMetadata = MapMetadata(
                mapId = activeMapId,
                nodeCount = 0,
                edgeCount = 0,
                version = 0,
                saveSuccessful = false,
                processingTimeMs = 0L,
                errorMessage = "Map serialization not initialized."
            )
            var landmarkNavigationMetadata = LandmarkNavigationMetadata(
                destinationFound = false,
                generatedSteps = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Route graph planning not completed."
            )
            var voiceMetadata = VoiceMetadata(
                generatedCommands = 0,
                suppressedCommands = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Guidance Decision not completed."
            )
            var hapticMetadata = HapticMetadata(
                generatedCommands = 0,
                suppressedCommands = 0,
                processingTimeMs = 0L,
                successful = false,
                errorMessage = "Guidance Decision not completed."
            )
            var finalTracksList: List<Track> = emptyList()
            var relativePositionsList: List<RelativePosition> = emptyList()
            var distanceEstimatesList: List<DistanceEstimate> = emptyList()
            var ttcEstimatesList: List<TtcEstimate> = emptyList()
            var threatPrioritiesList: List<ThreatPriority> = emptyList()
            var safeCorridorsList: List<SafeCorridor> = emptyList()
            var environmentObservationsList: List<EnvironmentObservation> = emptyList()
            var sceneMemory = SceneMemory(emptyList())
            var worldModel = WorldModel(emptyList(), emptyList())
            var navigationContext = NavigationContext(null, null, null, "Open Path Forward", emptyList())
            var routeMemory = RouteMemory(emptyList())
            var storedMap = mapRepository.getActiveMap() ?: mapRepository.loadMap(activeMapId)
            var routeGraph = RouteGraph(emptyList(), emptyList(), currentTime)
            var routePlan = RoutePlan(targetGoal, null, emptyList(), emptyList(), null)
            var landmarkNavigationState = LandmarkNavigationState(targetGoal, NavigationProgress.SEARCHING, null, emptyList(), null)
            var gpsState = GpsNavigationState(
                destination = gpsTarget,
                progress = NavigationProgress.SEARCHING,
                currentStep = null,
                routePoints = emptyList(),
                currentPointIndex = 0,
                distanceToTargetMeters = null,
                bearingToTargetDegrees = null,
                currentHeadingDegrees = null,
                currentInstruction = "Initializing GPS...",
                speechCommand = null,
                hapticCommand = null
            )
            var gpsMetadata = GpsNavigationMetadata(
                hasLocation = false,
                accuracy = null,
                satellitesCount = 0,
                recalculationsCount = 0,
                processingTimeMs = 0L,
                successful = false
            )
            var slamPose = PoseEstimate(0f, 0f, 0f, 0f, currentTime)
            var slamMap = SlamMap(emptyList())
            var slamMetadata = SlamMetadata(0, 0, 0f, 0f, 0, 0L, false, "Pipeline initialized")
            var localizedPosition = LocalizedPosition(null, null, 0f, 0f, 0f, LocalizationState.INITIALIZING, currentTime)
            @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
            var localizationConfidence: LocalizationConfidence = LocalizationConfidence.ZERO
            var localizationMetadata = LocalizationMetadata(0, 0, 0L, false, "Pipeline initialized")
            var guidanceDecision = GuidanceDecision(
                action = GuidanceAction.KEEP_CENTER,
                reason = "Pipeline initialized",
                selectedCorridor = HorizontalZone.CENTER,
                highestThreatId = null,
                highestThreatClassName = null,
                highestThreatLevel = null,
                confidence = 0.9f
            )
            var speechCommand: SpeechCommand? = null
            var hapticCommand: HapticCommand? = null

            if (bitmap != null) {
                val (poseResult, slamResult) = visualSlamEngine.process(bitmap)
                slamPose = poseResult
                slamMap = slamResult.first
                slamMetadata = slamResult.second

                val preprocessResult = yoloPreprocessor.preprocess(bitmap)
                val tensor = preprocessResult.first
                tensorMetadata = preprocessResult.second
                lastLetterbox = yoloPreprocessor.lastLetterboxResult

                if (tensor != null) {
                    val inferenceResult = yoloInferenceEngine.runInference(tensor)
                    val outputFloats = inferenceResult.first
                    inferenceMetadata = inferenceResult.second

                    if (outputFloats != null) {
                        val parsingResult = yoloDetectionParser.parse(outputFloats, inferenceMetadata.outputShape)
                        val candidateDetections = parsingResult.first
                        detectionMetadata = parsingResult.second

                        val currentDetections = if (candidateDetections.isNotEmpty()) {
                            val nmsResult = yoloNmsProcessor.process(candidateDetections)
                            nmsMetadata = nmsResult.second
                            nmsResult.first
                        } else {
                            emptyList<com.pathhelper.ai.onnx.Detection>()
                        }

                        // BUG FIX: Always update tracker, even if detections are empty, to run Kalman prediction
                        val trackingResult = trackManager.update(currentDetections, deltaTimeSeconds)
                        finalTracksList = trackingResult.first
                        trackMetadata = trackingResult.second

                        if (finalTracksList.isNotEmpty()) {
                            val spatialResult = relativePositionEngine.process(finalTracksList, 640f, 640f)
                            relativePositionsList = spatialResult.first
                            relativePositionMetadata = spatialResult.second

                            if (relativePositionsList.isNotEmpty()) {
                                val scale = lastLetterbox?.scale ?: (640.0f / image.height.toFloat())
                                val distResult = distanceEstimationEngine.estimate(
                                    relativePositionsList,
                                    finalTracksList,
                                    scale,
                                    deltaTimeSeconds
                                )
                                distanceEstimatesList = distResult.first
                                distanceMetadata = distResult.second

                                if (distanceEstimatesList.isNotEmpty()) {
                                    val ttcResult = ttcEngine.process(finalTracksList)
                                    ttcEstimatesList = ttcResult.first
                                    ttcMetadata = ttcResult.second

                                    if (ttcEstimatesList.isNotEmpty()) {
                                        val threatResult = obstaclePrioritizationEngine.process(finalTracksList)
                                        threatPrioritiesList = threatResult.first
                                        threatMetadata = threatResult.second
                                    }
                                }
                            }
                        }
                    }

                    val corridorResult = safeCorridorEngine.process(finalTracksList)
                    val baseCorridors = corridorResult.first
                    corridorMetadata = corridorResult.second

                    // Fuse base corridors with object-level threats
                    safeCorridorsList = navigationFusionEngine.fuse(threatPrioritiesList, baseCorridors)

                    val decisionResult = guidanceDecisionEngine.process(threatPrioritiesList, safeCorridorsList)
                    guidanceDecision = decisionResult.first
                    guidanceMetadata = decisionResult.second

                    val environmentResult = environmentUnderstandingEngine.process(finalTracksList)
                    environmentObservationsList = environmentResult.first
                    environmentMetadata = environmentResult.second

                    val memoryResult = sceneMemoryEngine.update(environmentObservationsList)
                    sceneMemory = memoryResult.first
                    memoryMetadata = memoryResult.second

                    val worldResult = worldModelEngine.build(sceneMemory)
                    worldModel = worldResult.first
                    worldModelMetadata = worldResult.second

                    val contextResult = navigationContextEngine.analyze(worldModel)
                    navigationContext = contextResult.first
                    navigationContextMetadata = contextResult.second

                    val routeResult = routeMemoryEngine.update(worldModel, navigationContext)
                    routeMemory = routeResult.first
                    routeMetadata = routeResult.second

                    val graphResult = routeGraphBuilder.build(worldModel, routeMemory, storedMap)
                    routeGraph = graphResult.first
                    routeGraphMetadata = graphResult.second

                    routePlan = routePlanner.plan(routeGraph, targetGoal, worldModel)

                    val updatedMap = mapLearningEngine.learn(routeGraph, storedMap, activeMapId)
                    storedMap = updatedMap

                    var saveSuccessful = true
                    var saveError: String? = null
                    val saveStartTime = SystemClock.elapsedRealtime()
                    try {
                        if (frameCount % 30 == 0L) {
                            mapRepository.saveMap(updatedMap)
                        } else {
                            mapRepository.setActiveMap(updatedMap)
                        }
                    } catch (e: Exception) {
                        saveSuccessful = false
                        saveError = e.localizedMessage
                    }
                    val saveDuration = SystemClock.elapsedRealtime() - saveStartTime
                    mapMetadata = MapMetadata(
                        mapId = activeMapId,
                        nodeCount = updatedMap.nodes.size,
                        edgeCount = updatedMap.edges.size,
                        version = updatedMap.version,
                        saveSuccessful = saveSuccessful,
                        processingTimeMs = saveDuration,
                        errorMessage = saveError
                    )

                    // Execute Central NavigationCoordinator
                    val lastLocation = gpsLocationProvider.getLastKnownLocation()
                    navigationCoordinator.process(
                        lastLocation = lastLocation,
                        tracks = finalTracksList,
                        sceneMemory = sceneMemory,
                        worldModel = worldModel,
                        navigationContext = navigationContext,
                        routeMemory = routeMemory,
                        routePlan = routePlan
                    )

                    landmarkNavigationState = navigationCoordinator.lastLandmarkState ?: landmarkNavigationState
                    landmarkNavigationMetadata = navigationCoordinator.lastLandmarkMetadata ?: landmarkNavigationMetadata

                    val voiceResult = voiceGuidanceEngine.process(
                        guidanceDecision, 
                        sceneMemoryEngine.lastEvents, 
                        navigationContext, 
                        routeMemoryEngine.lastEvents, 
                        landmarkNavigationState,
                        navigationCoordinator.lastHybridState
                    )
                    speechCommand = voiceResult.first
                    voiceMetadata = voiceResult.second

                    val hapticResult = hapticGuidanceEngine.process(guidanceDecision)
                    hapticCommand = hapticResult.first
                    hapticMetadata = hapticResult.second

                    val hasNewLandmark = sceneMemoryEngine.lastEvents.any { it.second == MemoryEvent.NEW }
                    if (hapticCommand == null && hasNewLandmark) {
                        hapticCommand = HapticCommand(
                            pattern = HapticPattern.CENTER,
                            action = GuidanceAction.KEEP_CENTER,
                            timestamp = currentTime,
                            priority = 40
                        )
                    }

                    val activePrimaryId = navigationContext.primaryLandmark?.landmarkId
                    if (lastPrimaryLandmarkId != activePrimaryId) {
                        lastPrimaryLandmarkId = activePrimaryId
                        if (activePrimaryId != null && hapticCommand == null) {
                            hapticCommand = HapticCommand(
                                pattern = HapticPattern.CENTER,
                                action = GuidanceAction.KEEP_CENTER,
                                timestamp = currentTime,
                                priority = 40
                            )
                        }
                    }

                    if (lastNavigationProgress != landmarkNavigationState.progress) {
                        if (hapticCommand == null) {
                            if (landmarkNavigationState.progress == NavigationProgress.ARRIVED) {
                                hapticCommand = HapticCommand(
                                    pattern = HapticPattern.RIGHT,
                                    action = GuidanceAction.KEEP_CENTER,
                                    timestamp = currentTime,
                                    priority = 50
                                )
                            } else if (landmarkNavigationState.progress == NavigationProgress.LOST) {
                                hapticCommand = HapticCommand(
                                    pattern = HapticPattern.WAIT,
                                    action = GuidanceAction.KEEP_CENTER,
                                    timestamp = currentTime,
                                    priority = 50
                                )
                            } else if (landmarkNavigationState.progress == NavigationProgress.ROUTE_FOUND) {
                                hapticCommand = HapticCommand(
                                    pattern = HapticPattern.CENTER,
                                    action = GuidanceAction.KEEP_CENTER,
                                    timestamp = currentTime,
                                    priority = 50
                                )
                            }
                        }
                        lastNavigationProgress = landmarkNavigationState.progress
                    }

                    if (navigationCoordinator.speechCommand != null) {
                        speechCommand = navigationCoordinator.speechCommand
                    }
                    if (navigationCoordinator.hapticCommand != null) {
                        hapticCommand = navigationCoordinator.hapticCommand
                    }

                    tensor.close()
                }
            } else {
                val corridorResult = safeCorridorEngine.process(emptyList())
                val baseCorridors = corridorResult.first
                corridorMetadata = corridorResult.second

                // Fuse base corridors with object-level threats
                safeCorridorsList = navigationFusionEngine.fuse(emptyList(), baseCorridors)

                val decisionResult = guidanceDecisionEngine.process(emptyList(), safeCorridorsList)
                guidanceDecision = decisionResult.first
                guidanceMetadata = decisionResult.second

                val environmentResult = environmentUnderstandingEngine.process(emptyList())
                environmentObservationsList = environmentResult.first
                environmentMetadata = environmentResult.second

                val memoryResult = sceneMemoryEngine.update(emptyList())
                sceneMemory = memoryResult.first
                memoryMetadata = memoryResult.second

                val worldResult = worldModelEngine.build(sceneMemory)
                worldModel = worldResult.first
                worldModelMetadata = worldResult.second

                val contextResult = navigationContextEngine.analyze(worldModel)
                navigationContext = contextResult.first
                navigationContextMetadata = contextResult.second

                val routeResult = routeMemoryEngine.update(worldModel, navigationContext)
                routeMemory = routeResult.first
                routeMetadata = routeResult.second

                val graphResult = routeGraphBuilder.build(worldModel, routeMemory, storedMap)
                routeGraph = graphResult.first
                routeGraphMetadata = graphResult.second

                routePlan = routePlanner.plan(routeGraph, targetGoal, worldModel)

                val updatedMap = mapLearningEngine.learn(routeGraph, storedMap, activeMapId)
                storedMap = updatedMap

                var saveSuccessful = true
                var saveError: String? = null
                val saveStartTime = SystemClock.elapsedRealtime()
                try {
                    if (frameCount % 30 == 0L) {
                        mapRepository.saveMap(updatedMap)
                    } else {
                        mapRepository.setActiveMap(updatedMap)
                    }
                } catch (e: Exception) {
                    saveSuccessful = false
                    saveError = e.localizedMessage
                }
                val saveDuration = SystemClock.elapsedRealtime() - saveStartTime
                mapMetadata = MapMetadata(
                    mapId = activeMapId,
                    nodeCount = updatedMap.nodes.size,
                    edgeCount = updatedMap.edges.size,
                    version = updatedMap.version,
                    saveSuccessful = saveSuccessful,
                    processingTimeMs = saveDuration,
                    errorMessage = saveError
                )

                // Execute Central NavigationCoordinator in fallback
                val lastLocation = gpsLocationProvider.getLastKnownLocation()
                navigationCoordinator.process(
                    lastLocation = lastLocation,
                    tracks = emptyList(),
                    sceneMemory = sceneMemory,
                    worldModel = worldModel,
                    navigationContext = navigationContext,
                    routeMemory = routeMemory,
                    routePlan = routePlan
                )

                landmarkNavigationState = navigationCoordinator.lastLandmarkState ?: landmarkNavigationState
                landmarkNavigationMetadata = navigationCoordinator.lastLandmarkMetadata ?: landmarkNavigationMetadata

                val voiceResult = voiceGuidanceEngine.process(guidanceDecision, sceneMemoryEngine.lastEvents, navigationContext, routeMemoryEngine.lastEvents, landmarkNavigationState)
                speechCommand = voiceResult.first
                voiceMetadata = voiceResult.second

                val hapticResult = hapticGuidanceEngine.process(guidanceDecision)
                hapticCommand = hapticResult.first
                hapticMetadata = hapticResult.second

                if (navigationCoordinator.speechCommand != null) {
                    speechCommand = navigationCoordinator.speechCommand
                }
                if (navigationCoordinator.hapticCommand != null) {
                    hapticCommand = navigationCoordinator.hapticCommand
                }
            }

            val finalGpsState = navigationCoordinator.lastGpsState ?: gpsState
            val finalGpsMetadata = navigationCoordinator.lastGpsMetadata ?: gpsMetadata
            val finalHybridState = navigationCoordinator.lastHybridState ?: HybridNavigationState(
                currentMode = NavigationMode.OUTDOOR,
                activeTarget = gpsTarget,
                currentInstruction = "Initializing...",
                outdoorProgress = 0f,
                indoorProgress = 0f,
                transitionStatus = "PENDING",
                etaSeconds = 0L,
                completionPercentage = 0f
            )
            val finalHybridMetadata = navigationCoordinator.lastHybridMetadata ?: HybridNavigationMetadata(0L, true)

            // Indoor Localization
            localizationProvider.update(slamPose, sceneMemory, worldModel, routeGraph)
            localizedPosition = localizationProvider.getPosition() ?: localizedPosition
            localizationConfidence = localizationProvider.getConfidence()
            localizationMetadata = localizationProvider.getMetadata() ?: localizationMetadata

            onFrameProcessed(
                stats,
                metadata,
                bitmapMetadata,
                tensorMetadata,
                lastLetterbox,
                inferenceMetadata,
                detectionMetadata,
                nmsMetadata,
                finalTracksList,
                trackMetadata,
                relativePositionsList,
                relativePositionMetadata,
                distanceEstimatesList,
                distanceMetadata,
                ttcEstimatesList,
                ttcMetadata,
                threatPrioritiesList,
                threatMetadata,
                safeCorridorsList,
                corridorMetadata,
                guidanceDecision,
                guidanceMetadata,
                environmentObservationsList,
                environmentMetadata,
                sceneMemory,
                memoryMetadata,
                worldModel,
                worldModelMetadata,
                navigationContext,
                navigationContextMetadata,
                routeMemory,
                routeMetadata,
                routeGraph,
                routeGraphMetadata,
                routePlan,
                storedMap,
                mapMetadata,
                landmarkNavigationState,
                landmarkNavigationMetadata,
                finalGpsState,
                finalGpsMetadata,
                finalHybridState,
                finalHybridMetadata,
                navigationCoordinator.lastEntranceDecision,
                navigationCoordinator.lastEntranceConfidence,
                navigationCoordinator.lastEntranceMetadata,
                slamPose,
                slamMap,
                slamMetadata,
                localizedPosition,
                localizationConfidence,
                localizationMetadata,
                speechCommand,
                voiceMetadata,
                hapticCommand,
                hapticMetadata
            )

            bitmap?.recycle()
        } catch (e: Exception) {
            val fallbackStats = AnalysisStats(frameCount, currentFps)
            val fallbackMetadata = FrameMetadata(0, 0, 0, 0, 0, 0, 0, 0, 0)
            val fallbackBitmapMetadata = BitmapMetadata(0, 0, 0, 0L, false, e.localizedMessage)
            val fallbackTensorMetadata = TensorMetadata(false, longArrayOf(1, 3, 640, 640), 0L, e.localizedMessage)
            val fallbackInferenceMetadata = InferenceMetadata(false, "unknown", longArrayOf(), 0L, e.localizedMessage)
            val fallbackDetectionMetadata = DetectionMetadata(false, 0, 0.0f, 0.0f, 0L, longArrayOf(), e.localizedMessage)
            val fallbackNmsMetadata = NmsMetadata(false, 0, 0, 0, 0.0f, 0L, e.localizedMessage)
            val fallbackTrackMetadata = TrackMetadata(0, 0, 0, 0L, false, e.localizedMessage)
            val fallbackSpatialMetadata = RelativePositionMetadata(0, 0, 0L, false, e.localizedMessage)
            val fallbackDistanceMetadata = DistanceMetadata(0, 0, 0.0f, 0L, false, e.localizedMessage)
            val fallbackTtcMetadata = TtcMetadata(0, 0, 0, 0.0f, 0L, false, e.localizedMessage)
            val fallbackThreatMetadata = ThreatMetadata(0, 0, 0, 0.0f, 0L, false, e.localizedMessage)
            val fallbackCorridorMetadata = CorridorMetadata(0, 3, 0, 0L, false, e.localizedMessage)
            val fallbackCorridors = listOf(HorizontalZone.LEFT, HorizontalZone.CENTER, HorizontalZone.RIGHT).map { zone ->
                SafeCorridor(zone, CorridorState.SAFE, 0, ThreatLevel.LOW, 0f, 100f)
            }
            val fallbackDecision = GuidanceDecision(GuidanceAction.WAIT, "Exception encountered", null, null, null, null, 0.5f)
            val fallbackGuidanceMetadata = GuidanceMetadata(0, 3, 0L, false, e.localizedMessage)
            val fallbackEnvironmentMetadata = EnvironmentMetadata(0, 0L, false, e.localizedMessage)
            val fallbackMemoryMetadata = MemoryMetadata(0, 0, 0, 0, 0L, false, e.localizedMessage)
            val fallbackWorldModelMetadata = WorldModelMetadata(0, 0, 0L, false, e.localizedMessage)
            val fallbackContext = NavigationContext(null, null, null, "Open Path Forward", emptyList())
            val fallbackContextMetadata = NavigationContextMetadata(0, 0, 0L, false, e.localizedMessage)
            val fallbackRoute = RouteMemory(emptyList())
            val fallbackRouteMetadata = RouteMetadata(0, 0, 0, 0L, false, e.localizedMessage)
            val fallbackGraph = RouteGraph(emptyList(), emptyList(), currentTime)
            val fallbackGraphMetadata = RouteGraphMetadata(0, 0, 0L, false, e.localizedMessage)
            val fallbackPlan = RoutePlan(targetGoal, null, emptyList(), emptyList(), null)
            val fallbackMapMetadata = MapMetadata(activeMapId, 0, 0, 0, false, 0L, e.localizedMessage)
            val fallbackNavState = LandmarkNavigationState(targetGoal, NavigationProgress.LOST, null, emptyList(), null)
            val fallbackNavMetadata = LandmarkNavigationMetadata(false, 0, 0L, false, e.localizedMessage)
            val fallbackPose = PoseEstimate(0f, 0f, 0f, 0f, currentTime)
            val fallbackSlamMap = SlamMap(emptyList())
            val fallbackSlamMeta = SlamMetadata(0, 0, 0f, 0f, 0, 0L, false, e.localizedMessage)

            val fallbackGpsLoc = Location("gps_fallback")
            val (fallbackGpsState, fallbackGpsMetadata) = gpsNavigationEngine.navigate(gpsTarget, fallbackGpsLoc, null, false, 0)

            val fallbackHybridState = HybridNavigationState(
                currentMode = NavigationMode.OUTDOOR,
                activeTarget = gpsTarget,
                currentInstruction = "Error: " + e.localizedMessage,
                outdoorProgress = 0f,
                indoorProgress = 0f,
                transitionStatus = "ERROR",
                etaSeconds = 0L,
                completionPercentage = 0f
            )
            val fallbackHybridMetadata = HybridNavigationMetadata(0L, false, e.localizedMessage)

            val fallbackVoiceMetadata = VoiceMetadata(0, 0, 0L, false, e.localizedMessage)
            val fallbackHapticMetadata = HapticMetadata(0, 0, 0L, false, e.localizedMessage)
            onFrameProcessed(
                fallbackStats,
                fallbackMetadata,
                fallbackBitmapMetadata,
                fallbackTensorMetadata,
                null,
                fallbackInferenceMetadata,
                fallbackDetectionMetadata,
                fallbackNmsMetadata,
                emptyList(),
                fallbackTrackMetadata,
                emptyList(),
                fallbackSpatialMetadata,
                emptyList(),
                fallbackDistanceMetadata,
                emptyList(),
                fallbackTtcMetadata,
                emptyList(),
                fallbackThreatMetadata,
                fallbackCorridors,
                fallbackCorridorMetadata,
                fallbackDecision,
                fallbackGuidanceMetadata,
                emptyList(),
                fallbackEnvironmentMetadata,
                SceneMemory(emptyList()),
                fallbackMemoryMetadata,
                WorldModel(emptyList(), emptyList()),
                fallbackWorldModelMetadata,
                fallbackContext,
                fallbackContextMetadata,
                fallbackRoute,
                fallbackRouteMetadata,
                fallbackGraph,
                fallbackGraphMetadata,
                fallbackPlan,
                null,
                fallbackMapMetadata,
                fallbackNavState,
                fallbackNavMetadata,
                fallbackGpsState,
                fallbackGpsMetadata,
                fallbackHybridState,
                fallbackHybridMetadata,
                null,
                null,
                null,
                fallbackPose,
                fallbackSlamMap,
                fallbackSlamMeta,
                LocalizedPosition(null, null, 0f, 0f, 0f, LocalizationState.LOST, currentTime),
                LocalizationConfidence.ZERO,
                LocalizationMetadata(0, 0, 0L, false, e.localizedMessage),
                null,
                fallbackVoiceMetadata,
                null,
                fallbackHapticMetadata
            )
        } finally {
            image.close()
        }
    }
}
