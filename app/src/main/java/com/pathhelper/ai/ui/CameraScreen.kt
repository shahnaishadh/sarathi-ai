package com.pathhelper.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pathhelper.ai.camera.AnalysisStats
import com.pathhelper.ai.camera.BitmapMetadata
import com.pathhelper.ai.camera.CameraPreview
import com.pathhelper.ai.camera.FrameAnalyzer
import com.pathhelper.ai.camera.FrameMetadata
import com.pathhelper.ai.onnx.DetectionMetadata
import com.pathhelper.ai.onnx.DetectionRenderData
import com.pathhelper.ai.onnx.InferenceMetadata
import com.pathhelper.ai.onnx.ModelLoader
import com.pathhelper.ai.onnx.ModelMetadata
import com.pathhelper.ai.onnx.NmsMetadata
import com.pathhelper.ai.onnx.TensorMetadata
import com.pathhelper.ai.tracking.Track
import com.pathhelper.ai.tracking.TrackMetadata
import com.pathhelper.ai.navigation.RelativePosition
import com.pathhelper.ai.navigation.RelativePositionMetadata
import com.pathhelper.ai.navigation.DistanceEstimate
import com.pathhelper.ai.navigation.DistanceMetadata
import com.pathhelper.ai.navigation.TtcEstimate
import com.pathhelper.ai.navigation.TtcMetadata
import com.pathhelper.ai.navigation.TtcRiskLevel
import com.pathhelper.ai.navigation.ThreatPriority
import com.pathhelper.ai.navigation.ThreatMetadata
import com.pathhelper.ai.navigation.ThreatLevel
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.CorridorState
import com.pathhelper.ai.navigation.SafeCorridor
import com.pathhelper.ai.navigation.CorridorMetadata
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.GuidanceDecision
import com.pathhelper.ai.navigation.GuidanceMetadata
import com.pathhelper.ai.voice.SpeechCommand
import com.pathhelper.ai.voice.VoiceMetadata
import com.pathhelper.ai.voice.TextToSpeechManager
import com.pathhelper.ai.haptics.HapticCommand
import com.pathhelper.ai.haptics.HapticMetadata
import com.pathhelper.ai.haptics.HapticManager
import com.pathhelper.ai.haptics.HapticPattern
import com.pathhelper.ai.environment.EnvironmentObservation
import com.pathhelper.ai.environment.EnvironmentMetadata
import com.pathhelper.ai.memory.MemoryMetadata
import com.pathhelper.ai.world.WorldModelMetadata
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.context.NavigationContextMetadata
import com.pathhelper.ai.route.RouteMetadata
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
import com.pathhelper.ai.navigation.common.target.GpsTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.landmark.LandmarkNavigationMetadata
import com.pathhelper.ai.navigation.graph.RouteGraphMetadata
import com.pathhelper.ai.navigation.graph.RoutePlan
import com.pathhelper.ai.navigation.persistence.MapMetadata
import com.pathhelper.ai.navigation.outdoor.gps.GpsNavigationMetadata
import com.pathhelper.ai.navigation.hybrid.HybridNavigationState
import com.pathhelper.ai.navigation.hybrid.HybridNavigationMetadata
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceDecision
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceConfidence
import com.pathhelper.ai.navigation.hybrid.entrance.EntranceMetadata
import com.pathhelper.ai.navigation.common.target.BuildingTarget
import com.pathhelper.ai.navigation.common.target.RoomTarget
import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.localization.slam.SlamMap
import com.pathhelper.ai.localization.slam.SlamMetadata
import com.pathhelper.ai.localization.pose.LocalizedPosition
import com.pathhelper.ai.localization.pose.LocalizationConfidence
import com.pathhelper.ai.localization.pose.LocalizationMetadata
import com.pathhelper.ai.localization.pose.LocalizationState
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathhelper.ai.guidance.announcement.AnnouncementManager
import com.pathhelper.ai.guidance.announcement.AnnouncementPriority
import com.pathhelper.ai.perception.lighting.AmbientLightAnalyzer
import com.pathhelper.ai.perception.lighting.TorchController
import com.pathhelper.ai.validation.ui.ValidationViewModel
import com.pathhelper.ai.validation.ui.components.ValidationDashboard
import com.pathhelper.ai.validation.ui.components.ValidationModePanel
import java.util.Locale

private val classNames = listOf(
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard",
    "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
    "scissors", "teddy bear", "hair drier", "toothbrush"
)

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var cameraStatus by remember { mutableStateOf("Inactive") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ONNX Model Loading States
    var modelMetadata by remember { mutableStateOf<ModelMetadata?>(null) }
    var modelStatus by remember { mutableStateOf("Loading") }
    var modelError by remember { mutableStateOf<String?>(null) }

    // Camera Frame Analysis States
    var analysisStats by remember { mutableStateOf(AnalysisStats(0L, 0.0)) }
    var frameMetadata by remember { mutableStateOf<FrameMetadata?>(null) }
    var bitmapMetadata by remember { mutableStateOf<BitmapMetadata?>(null) }
    var bitmapWidth by remember { mutableStateOf(1) }
    var bitmapHeight by remember { mutableStateOf(1) }

    // YOLO Preprocessing States
    var tensorMetadata by remember { mutableStateOf<TensorMetadata?>(null) }
    var letterboxScale by remember { mutableStateOf(1.0f) }
    var letterboxPadX by remember { mutableStateOf(0) }
    var letterboxPadY by remember { mutableStateOf(0) }

    // YOLO Inference States
    var inferenceMetadata by remember { mutableStateOf<InferenceMetadata?>(null) }

    // YOLO Detection Parsing States
    var detectionMetadata by remember { mutableStateOf<DetectionMetadata?>(null) }

    // YOLO Detection Filtering States
    var nmsMetadata by remember { mutableStateOf<NmsMetadata?>(null) }

    // YOLO Tracking States
    var finalTracksList by remember { mutableStateOf<List<Track>>(emptyList()) }
    var trackMetadata by remember { mutableStateOf<TrackMetadata?>(null) }

    // Spatial Awareness States
    var relativePositionsList by remember { mutableStateOf<List<RelativePosition>>(emptyList()) }
    var relativePositionMetadata by remember { mutableStateOf<RelativePositionMetadata?>(null) }

    // Distance Estimation States
    var distanceEstimatesList by remember { mutableStateOf<List<DistanceEstimate>>(emptyList()) }
    var distanceMetadata by remember { mutableStateOf<DistanceMetadata?>(null) }

    // Time-To-Collision States
    var ttcEstimatesList by remember { mutableStateOf<List<TtcEstimate>>(emptyList()) }
    var ttcMetadata by remember { mutableStateOf<TtcMetadata?>(null) }

    // Threat Prioritization States
    var threatPrioritiesList by remember { mutableStateOf<List<ThreatPriority>>(emptyList()) }
    var threatMetadata by remember { mutableStateOf<ThreatMetadata?>(null) }

    // Safe Corridor States
    var safeCorridorsList by remember { mutableStateOf<List<SafeCorridor>>(emptyList()) }
    var corridorMetadata by remember { mutableStateOf<CorridorMetadata?>(null) }

    // Guidance Decision States
    var guidanceDecision by remember { mutableStateOf<GuidanceDecision?>(null) }
    var guidanceMetadata by remember { mutableStateOf<GuidanceMetadata?>(null) }

    // Environment Understanding States
    var observationsCount by remember { mutableStateOf(0) }
    var topObservation by remember { mutableStateOf("N/A") }
    var observationConfidence by remember { mutableStateOf(0.0f) }
    var environmentMetadata by remember { mutableStateOf<EnvironmentMetadata?>(null) }

    // Scene Memory States
    var activeMemoriesCount by remember { mutableStateOf(0) }
    var newMemoriesCount by remember { mutableStateOf(0) }
    var updatedMemoriesCount by remember { mutableStateOf(0) }
    var expiredMemoriesCount by remember { mutableStateOf(0) }
    var memoryMetadata by remember { mutableStateOf<MemoryMetadata?>(null) }

    // World Model States
    var landmarksCount by remember { mutableStateOf(0) }
    var relationsCount by remember { mutableStateOf(0) }
    var nearestLandmark by remember { mutableStateOf("N/A") }
    var navigationContext by remember { mutableStateOf("N/A") }
    var worldModelMetadata by remember { mutableStateOf<WorldModelMetadata?>(null) }

    // Navigation Context States
    var primaryLandmarkText by remember { mutableStateOf("N/A") }
    var secondaryLandmarkText by remember { mutableStateOf("N/A") }
    var approachingLandmarkText by remember { mutableStateOf("N/A") }
    var activeContextText by remember { mutableStateOf("N/A") }
    var highestPriorityText by remember { mutableStateOf("N/A") }
    var navigationContextMetadata by remember { mutableStateOf<NavigationContextMetadata?>(null) }

    // Route Memory States
    var trackedLandmarksCount by remember { mutableStateOf(0) }
    var passedLandmarksCount by remember { mutableStateOf(0) }
    var revisitedLandmarksCount by remember { mutableStateOf(0) }
    var currentLandmarkText by remember { mutableStateOf("N/A") }
    var progressText by remember { mutableStateOf("N/A") }
    var routeMetadata by remember { mutableStateOf<RouteMetadata?>(null) }

    // Route Graph States
    var routeGraphMetadata by remember { mutableStateOf<RouteGraphMetadata?>(null) }
    var routePlan by remember { mutableStateOf<RoutePlan?>(null) }

    // Persistent Maps States
    var mapMetadata by remember { mutableStateOf<MapMetadata?>(null) }

    // Landmark Navigation States
    var destinationText by remember { mutableStateOf("N/A") }
    var statusText by remember { mutableStateOf("N/A") }
    var distanceText by remember { mutableStateOf("N/A") }
    var currentStepText by remember { mutableStateOf("N/A") }
    var landmarkNavigationMetadata by remember { mutableStateOf<LandmarkNavigationMetadata?>(null) }

    // GPS Navigation States
    var gpsDestinationText by remember { mutableStateOf("N/A") }
    var gpsStatusText by remember { mutableStateOf("N/A") }
    var gpsDistanceText by remember { mutableStateOf("N/A") }
    var gpsStepText by remember { mutableStateOf("N/A") }
    var gpsMetadata by remember { mutableStateOf<GpsNavigationMetadata?>(null) }

    // Hybrid Navigation States
    var hybridModeText by remember { mutableStateOf("OUTDOOR") }
    var hybridStatusText by remember { mutableStateOf("PENDING") }
    var hybridCompletionText by remember { mutableStateOf("0%") }
    var hybridInstructionText by remember { mutableStateOf("N/A") }
    var hybridMetadata by remember { mutableStateOf<HybridNavigationMetadata?>(null) }

    // Entrance Detection States
    var entranceDecisionText by remember { mutableStateOf("MORE_EVIDENCE_REQUIRED") }
    var entranceCandidatesCount by remember { mutableStateOf(0) }
    var entranceConfidenceScore by remember { mutableStateOf("0%") }
    var entranceGpsProximity by remember { mutableStateOf("0%") }
    var entranceDoorConfidence by remember { mutableStateOf("0%") }
    var entranceMetadata by remember { mutableStateOf<EntranceMetadata?>(null) }

    // Visual SLAM States
    var slamPositionText by remember { mutableStateOf("0.00, 0.00") }
    var slamHeadingText by remember { mutableStateOf("0.0°") }
    var slamFeatureCount by remember { mutableStateOf(0) }
    var slamMatchedFeatures by remember { mutableStateOf(0) }
    var slamLocalMapPoints by remember { mutableStateOf(0) }
    var slamTrackingConfidence by remember { mutableStateOf("0%") }
    var slamPoseConfidence by remember { mutableStateOf("0%") }
    var slamMetadata by remember { mutableStateOf<SlamMetadata?>(null) }

    // Indoor Localization States
    var locCurrentRoom by remember { mutableStateOf("N/A") }
    var locNearestLandmark by remember { mutableStateOf("N/A") }
    var locLandmarkDistance by remember { mutableStateOf("N/A") }
    var locLandmarkMatchScore by remember { mutableStateOf("0%") }
    var locConfidenceText by remember { mutableStateOf("0%") }
    var locPositionDrift by remember { mutableStateOf("0%") }
    var locPoseConfidenceText by remember { mutableStateOf("0%") }
    var localizationMeta by remember { mutableStateOf<LocalizationMetadata?>(null) }
    var localizationConfidenceData by remember { mutableStateOf<LocalizationConfidence?>(null) }
    var localizationPosition by remember { mutableStateOf<LocalizedPosition?>(null) }

    // Voice Guidance States
    var lastCommand by remember { mutableStateOf("N/A") }
    var suppressedCommandsCount by remember { mutableStateOf(0) }
    var generatedCommandsCount by remember { mutableStateOf(0) }
    var voiceMetadata by remember { mutableStateOf<VoiceMetadata?>(null) }

    // Haptic Guidance States
    var lastHapticPattern by remember { mutableStateOf("N/A") }
    var generatedHapticCommandsCount by remember { mutableStateOf(0) }
    var suppressedHapticCommandsCount by remember { mutableStateOf(0) }
    var hapticMetadata by remember { mutableStateOf<HapticMetadata?>(null) }

    // Overlay Render Telemetry
    var overlayRenderTimeMs by remember { mutableStateOf(0L) }

    val modelLoader = remember { ModelLoader(context) }
    val ttsManager = remember { TextToSpeechManager(context) }
    val hapticManager = remember { HapticManager(context) }

    // ── Validation ViewModel ────────────────────────────────────────────────
    val validationViewModel: ValidationViewModel = viewModel(
        factory = ValidationViewModel.Factory(context)
    )
    val validationMetrics by validationViewModel.metrics.collectAsState()

    // ── Announcement Manager (wraps TTS with suppression logic) ────────────
    val announcementManager = remember {
        AnnouncementManager { text -> ttsManager.speak(
            com.pathhelper.ai.voice.SpeechCommand(
                text = text,
                action = GuidanceAction.KEEP_CENTER,
                priority = 50,
                timestamp = System.currentTimeMillis()
            )
        )}
    }

    // ── Torch Controller ───────────────────────────────────────────────────
    val torchController = remember {
        val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE)
            as CameraManager
        val cameraId = try { cameraManager.cameraIdList.firstOrNull() ?: "0" }
                       catch (e: Exception) { "0" }
        TorchController(
            cameraManager = cameraManager,
            cameraId = cameraId,
            speak = { text ->
                announcementManager.announce(
                    text = text,
                    priority = AnnouncementPriority.INFO,
                    currentPosX = 0f,
                    currentPosY = 0f
                )
            },
            onStateChanged = { state, brightness, torch ->
                validationViewModel.updateLighting(state.name, brightness, torch)
            }
        )
    }

    val frameAnalyzer = remember {
        FrameAnalyzer(context) { stats, metadata, bMeta, tMeta, letterbox, iMeta, dMeta, nMeta, tracks, trackingStats, spatialPositions, spatialStats, distanceEstimates, distanceStats, ttcEstimates, ttcStats, threatPriorities, threatStats, safeCorridors, safeCorridorStats, decision, decisionStats, environmentObservations, environmentStats, _, sceneMemoryStats, worldModel, worldModelStats, navContext, navContextStats, routeMemory, routeMemoryStats, _, graphStats, plan, _, mapStats, navState, navStats, gpsState, gpsStats, hybridState, hybridStats, entranceDecision, entranceConfidence, entranceMetadataParam, slamPose, _, slamMetadataParam, locPosition, locConfidence, locMetadataParam, speechCommand, voiceStats, hapticCommand, hapticStats ->
            
            analysisStats = stats
            frameMetadata = metadata
            bitmapMetadata = bMeta
            tensorMetadata = tMeta
            inferenceMetadata = iMeta
            detectionMetadata = dMeta
            nmsMetadata = nMeta
            finalTracksList = tracks
            trackMetadata = trackingStats
            relativePositionsList = spatialPositions
            relativePositionMetadata = spatialStats
            distanceEstimatesList = distanceEstimates
            distanceMetadata = distanceStats
            ttcEstimatesList = ttcEstimates
            ttcMetadata = ttcStats
            threatPrioritiesList = threatPriorities
            threatMetadata = threatStats
            safeCorridorsList = safeCorridors
            corridorMetadata = safeCorridorStats
            guidanceDecision = decision
            guidanceMetadata = decisionStats

            speechCommand?.let { cmd ->
                val priority = when {
                    cmd.action == GuidanceAction.STOP -> AnnouncementPriority.CRITICAL
                    cmd.priority >= 80 -> AnnouncementPriority.CRITICAL
                    cmd.priority >= 50 -> AnnouncementPriority.WARNING
                    else -> AnnouncementPriority.INFO
                }
                announcementManager.announce(cmd.text, priority, 0f, 0f)
                lastCommand = cmd.text
            }
            voiceMetadata = voiceStats
            suppressedCommandsCount = voiceStats.suppressedCommands
            generatedCommandsCount = voiceStats.generatedCommands

            hapticCommand?.let { cmd ->
                hapticManager.play(cmd)
                lastHapticPattern = cmd.pattern.name
            }
            hapticMetadata = hapticStats
            suppressedHapticCommandsCount = hapticStats.suppressedCommands
            generatedHapticCommandsCount = hapticStats.generatedCommands

            environmentMetadata = environmentStats
            observationsCount = environmentStats.observations
            val topObs = environmentObservations.maxByOrNull { it.confidence }
            topObservation = topObs?.type?.name ?: "N/A"
            observationConfidence = topObs?.confidence ?: 0f

            memoryMetadata = sceneMemoryStats
            activeMemoriesCount = sceneMemoryStats.activeObservations
            newMemoriesCount = sceneMemoryStats.newObservations
            updatedMemoriesCount = sceneMemoryStats.updatedObservations
            expiredMemoriesCount = sceneMemoryStats.expiredObservations

            worldModelMetadata = worldModelStats
            landmarksCount = worldModelStats.landmarks
            relationsCount = worldModelStats.relations
            nearestLandmark = worldModel.landmarks.filter { it.distanceMeters != null }.minByOrNull { it.distanceMeters!! }?.type?.name ?: "N/A"

            navigationContext = when {
                worldModel.landmarks.any { it.type == LandmarkType.ENTRANCE } -> "ENTRANCE"
                worldModel.landmarks.any { it.type == LandmarkType.HALLWAY } -> "HALLWAY"
                worldModel.landmarks.any { it.type == LandmarkType.CROSSWALK } -> "CROSSWALK"
                worldModel.landmarks.any { it.type == LandmarkType.DOOR } -> "DOOR"
                else -> "N/A"
            }

            navigationContextMetadata = navContextStats
            primaryLandmarkText = navContext.primaryLandmark?.landmarkType?.name ?: "N/A"
            secondaryLandmarkText = navContext.secondaryLandmark?.landmarkType?.name ?: "N/A"
            approachingLandmarkText = navContext.approachingLandmark?.landmarkType?.name ?: "N/A"
            activeContextText = navContext.activeContext
            highestPriorityText = navContext.primaryLandmark?.priority?.name ?: "N/A"

            routeMetadata = routeMemoryStats
            trackedLandmarksCount = routeMemoryStats.trackedLandmarks
            passedLandmarksCount = routeMemoryStats.passedLandmarks
            revisitedLandmarksCount = routeMemoryStats.revisitedLandmarks
            currentLandmarkText = navContext.primaryLandmark?.landmarkType?.name ?: "N/A"
            progressText = routeMemory.landmarks.find { lm -> lm.landmarkId == navContext.primaryLandmark?.landmarkId }?.event?.name ?: "N/A"

            routeGraphMetadata = graphStats
            routePlan = plan

            mapMetadata = mapStats

            landmarkNavigationMetadata = navStats
            destinationText = (navState.destination as? LandmarkTarget)?.landmarkType?.name ?: "N/A"
            statusText = navState.progress.name
            distanceText = navState.remainingDistanceMeters?.let { d -> String.format(Locale.US, "%.1fm", d) } ?: "N/A"
            currentStepText = navState.currentStep?.instruction ?: "N/A"

            gpsMetadata = gpsStats
            gpsDestinationText = when (val d = gpsState.destination) {
                is GpsTarget -> String.format(Locale.US, "%.5f, %.5f", d.latitude, d.longitude)
                is BuildingTarget -> String.format(Locale.US, "%.5f, %.5f", d.entranceLatitude, d.entranceLongitude)
                else -> "N/A"
            }
            gpsStatusText = gpsState.progress.name
            gpsDistanceText = gpsState.distanceToTargetMeters?.let { d -> String.format(Locale.US, "%.1fm", d) } ?: "N/A"
            gpsStepText = gpsState.currentInstruction ?: "N/A"

            hybridMetadata = hybridStats
            hybridModeText = hybridState.currentMode.name
            hybridStatusText = hybridState.transitionStatus
            hybridCompletionText = String.format(Locale.US, "%.0f%%", hybridState.completionPercentage)
            hybridInstructionText = hybridState.currentInstruction

            entranceMetadata = entranceMetadataParam
            entranceDecisionText = entranceDecision?.name ?: "N/A"
            entranceCandidatesCount = entranceMetadataParam?.candidateCount ?: 0
            entranceConfidenceScore = String.format(Locale.US, "%.0f%%", (entranceConfidence?.score ?: 0f) * 100)
            entranceGpsProximity = String.format(Locale.US, "%.0f%%", (entranceConfidence?.gpsProximityScore ?: 0f) * 100)
            entranceDoorConfidence = String.format(Locale.US, "%.0f%%", (entranceConfidence?.doorConfidence ?: 0f) * 100)

            slamMetadata = slamMetadataParam
            slamPositionText = String.format(Locale.US, "%.2f, %.2f", slamPose.positionX, slamPose.positionY)
            slamHeadingText = String.format(Locale.US, "%.1f°", slamPose.headingDegrees)
            slamFeatureCount = slamMetadataParam?.featureCount ?: 0
            slamMatchedFeatures = slamMetadataParam?.matchedFeatures ?: 0
            slamLocalMapPoints = slamMetadataParam?.localMapPoints ?: 0
            slamTrackingConfidence = String.format(Locale.US, "%.0f%%", (slamMetadataParam?.trackingConfidence ?: 0f) * 100)
            slamPoseConfidence = String.format(Locale.US, "%.0f%%", (slamMetadataParam?.poseConfidence ?: 0f) * 100)

            localizationMeta = locMetadataParam
            localizationPosition = locPosition
            localizationConfidenceData = locConfidence
            locCurrentRoom = locPosition.currentRoom ?: "N/A"
            locNearestLandmark = locPosition.nearestLandmark ?: "N/A"
            locLandmarkDistance = String.format(Locale.US, "%.1fm", locPosition.landmarkDistance)
            locConfidenceText = String.format(Locale.US, "%.0f%%", locPosition.confidence * 100)
            locPoseConfidenceText = String.format(Locale.US, "%.0f%%", (locConfidence.poseConfidence * 100))
            locPositionDrift = String.format(Locale.US, "%.1f%%", (1f - locConfidence.routeGraphMatchScore) * 100f)
            locLandmarkMatchScore = String.format(Locale.US, "%.0f%%", locConfidence.landmarkMatchScore * 100)

            letterboxScale = letterbox?.scale ?: 1f
            letterboxPadX = letterbox?.padX ?: 0
            letterboxPadY = letterbox?.padY ?: 0
            bitmapWidth = bMeta.width
            bitmapHeight = bMeta.height

            // ── Validation ViewModel updates ────────────────────────────────
            validationViewModel.updateLocalization(locPosition, locConfidence)
            validationViewModel.updatePose(
                x = slamPose.positionX,
                y = slamPose.positionY,
                headingDeg = slamPose.headingDegrees
            )
            validationViewModel.updateLatencies(
                yolo = iMeta.inferenceTimeMs,
                tracking = trackingStats.processingTimeMs,
                slam = slamMetadataParam?.processingTimeMs ?: 0L,
                localization = locMetadataParam?.processingTimeMs ?: 0L,
                guidance = decisionStats.decisionTimeMs
            )
            validationViewModel.updatePerformance(
                trackedObjs = tracks.size,
                navMode = hybridState.currentMode.name
            )
            run {
                val ann = announcementManager.snapshot()
                validationViewModel.updateAnnouncements(
                    generated = ann.generated,
                    spoken = ann.spoken,
                    suppressed = ann.suppressed
                )
            }
            validationViewModel.onFrameProcessed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            errorMessage = "Camera permission is required to use PathHelper AI."
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val metadata = modelLoader.loadModel()
            modelMetadata = metadata
            if (metadata.isLoaded) {
                modelStatus = "Loaded"
                modelError = null
            } else {
                modelStatus = "Error"
                modelError = metadata.errorMessage
            }
        } catch (e: Exception) {
            modelError = e.localizedMessage ?: "Unknown loading error"
            modelStatus = "Error"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            torchController.release()
            modelLoader.close()
            ttsManager.shutdown()
            hapticManager.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val density = LocalDensity.current
            val canvasWidthPx = with(density) { maxWidth.toPx() }
            val canvasHeightPx = with(density) { maxHeight.toPx() }

            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = frameAnalyzer,
                    onStatusChanged = { status ->
                        cameraStatus = status
                        errorMessage = null
                    },
                    onError = { err ->
                        errorMessage = err
                        cameraStatus = "Error"
                    }
                )

                val renderDataList = remember(
                    finalTracksList,
                    relativePositionsList,
                    distanceEstimatesList,
                    ttcEstimatesList,
                    threatPrioritiesList,
                    bitmapWidth,
                    bitmapHeight,
                    letterboxScale,
                    letterboxPadX,
                    letterboxPadY,
                    canvasWidthPx,
                    canvasHeightPx
                ) {
                    val renderStartTime = SystemClock.elapsedRealtime()
                    val list = finalTracksList.map { track ->
                        val origCenterX = (track.centerX - letterboxPadX) / letterboxScale
                        val origCenterY = (track.centerY - letterboxPadY) / letterboxScale
                        val origW = track.width / letterboxScale
                        val origH = track.height / letterboxScale

                        val scaleX = canvasWidthPx / bitmapWidth
                        val scaleY = canvasHeightPx / bitmapHeight

                        val screenCenterX = origCenterX * scaleX
                        val screenCenterY = origCenterY * scaleY
                        val screenW = origW * scaleX
                        val screenH = origH * scaleY

                        val positionInfo = relativePositionsList.find { it.trackId == track.id }
                        val zone = positionInfo?.horizontalZone

                        val distInfo = distanceEstimatesList.find { it.trackId == track.id }
                        val dist = distInfo?.distanceMeters

                        val ttcInfo = ttcEstimatesList.find { it.trackId == track.id }
                        val ttcVal = ttcInfo?.ttcSeconds
                        val riskVal = ttcInfo?.riskLevel ?: TtcRiskLevel.SAFE

                        val threatInfo = threatPrioritiesList.find { it.trackId == track.id }
                        val scoreVal = threatInfo?.priorityScore ?: 0f
                        val levelVal = threatInfo?.threatLevel ?: ThreatLevel.LOW
                        val rankVal = threatInfo?.rank ?: Int.MAX_VALUE

                        DetectionRenderData(
                            trackId = track.id,
                            classId = track.classId,
                            confidence = track.confidence,
                            left = screenCenterX - screenW / 2f,
                            top = screenCenterY - screenH / 2f,
                            right = screenCenterX + screenW / 2f,
                            bottom = screenCenterY + screenH / 2f,
                            horizontalZone = zone,
                            distanceMeters = dist,
                            ttcSeconds = ttcVal,
                            riskLevel = riskVal,
                            priorityScore = scoreVal,
                            threatLevel = levelVal,
                            threatRank = rankVal
                        )
                    }
                    overlayRenderTimeMs = SystemClock.elapsedRealtime() - renderStartTime
                    list
                }

                DetectionOverlay(detections = renderDataList)

                if (safeCorridorsList.isNotEmpty()) {
                    val bestCorridor = safeCorridorsList.sortedWith(
                        compareByDescending<SafeCorridor> { it.score }
                            .thenBy {
                                when (it.horizontalZone) {
                                    HorizontalZone.CENTER -> 0
                                    HorizontalZone.LEFT -> 1
                                    HorizontalZone.RIGHT -> 2
                                }
                            }
                    ).firstOrNull()?.horizontalZone ?: HorizontalZone.CENTER

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color(0x99000000), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        safeCorridorsList.forEach { corr ->
                            val stateColor = when (corr.state) {
                                CorridorState.SAFE -> Color.Green
                                CorridorState.CAUTION -> Color.Yellow
                                CorridorState.BLOCKED -> Color.Red
                            }
                            Text(
                                text = "${corr.horizontalZone.name} ${corr.state.name}",
                                color = stateColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "BEST: $bestCorridor",
                            color = Color.Cyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                guidanceDecision?.let { dec ->
                    val actionColor = when (dec.action) {
                        GuidanceAction.STOP -> Color.Red
                        GuidanceAction.WAIT -> Color.Yellow
                        GuidanceAction.KEEP_CENTER -> Color.Green
                        GuidanceAction.MOVE_LEFT -> Color.Cyan
                        GuidanceAction.MOVE_SLIGHTLY_LEFT -> Color.Cyan
                        GuidanceAction.MOVE_RIGHT -> Color.Cyan
                        GuidanceAction.MOVE_SLIGHTLY_RIGHT -> Color.Cyan
                    }
                    val actionText = when (dec.action) {
                        GuidanceAction.MOVE_LEFT -> "MOVE LEFT"
                        GuidanceAction.MOVE_SLIGHTLY_LEFT -> "SLIGHTLY LEFT"
                        GuidanceAction.MOVE_RIGHT -> "MOVE RIGHT"
                        GuidanceAction.MOVE_SLIGHTLY_RIGHT -> "SLIGHTLY RIGHT"
                        GuidanceAction.KEEP_CENTER -> "KEEP CENTER"
                        GuidanceAction.STOP -> "STOP"
                        GuidanceAction.WAIT -> "WAIT"
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color(0xDD000000), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ACTION",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = actionText,
                            color = actionColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Camera permission is required to use PathHelper AI.",
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // ── Validation Dashboard (debug-only, collapsible) ──────────────────
        ValidationDashboard(
            metrics = validationMetrics,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Validation Mode Panel (debug-only, test runner) ────────────────
        ValidationModePanel(
            sessionManager = validationViewModel.sessionManager,
            metricsProvider = { validationViewModel.metrics.value },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1B1F)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "PathHelper AI Shell",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    PerformanceTelemetry(analysisStats, frameMetadata, bitmapMetadata)
                    InferenceTelemetry(inferenceMetadata, tensorMetadata)
                    DetectionTelemetry(detectionMetadata)
                    FilteringTelemetry(nmsMetadata)
                    TrackingTelemetry(trackMetadata)
                    SpatialTelemetry(relativePositionMetadata, relativePositionsList, distanceEstimatesList)
                    DistanceTelemetry(distanceMetadata)
                    TtcTelemetry(ttcMetadata)
                    ThreatTelemetry(threatMetadata)
                    CorridorTelemetry(corridorMetadata, safeCorridorsList)
                    GuidanceTelemetry(guidanceMetadata, guidanceDecision)
                    EnvironmentTelemetry(environmentMetadata, topObservation, observationConfidence, observationsCount)
                    MemoryTelemetry(memoryMetadata, activeMemoriesCount, newMemoriesCount, updatedMemoriesCount, expiredMemoriesCount)
                    WorldModelTelemetry(worldModelMetadata, landmarksCount, relationsCount, nearestLandmark, navigationContext)
                    ContextTelemetry(navigationContextMetadata, primaryLandmarkText, secondaryLandmarkText, approachingLandmarkText, activeContextText, highestPriorityText)
                    RouteTelemetry(routeMetadata, trackedLandmarksCount, passedLandmarksCount, revisitedLandmarksCount, currentLandmarkText, progressText)
                    GraphTelemetry(routeGraphMetadata, routePlan)
                    MapTelemetry(mapMetadata)
                    LandmarkNavTelemetry(landmarkNavigationMetadata, destinationText, statusText, distanceText, currentStepText)
                    GpsTelemetry(gpsMetadata, gpsDestinationText, gpsStatusText, gpsDistanceText, gpsStepText)
                    HybridTelemetry(hybridMetadata, hybridModeText, hybridStatusText, hybridCompletionText, hybridInstructionText)
                    EntranceTelemetry(entranceMetadata, entranceDecisionText, entranceCandidatesCount, entranceConfidenceScore, entranceGpsProximity, entranceDoorConfidence)
                    SlamTelemetry(slamMetadata, slamPositionText, slamHeadingText, slamFeatureCount, slamMatchedFeatures, slamLocalMapPoints, slamTrackingConfidence, slamPoseConfidence)
                    LocalizationTelemetry(localizationMeta, localizationPosition, locCurrentRoom, locNearestLandmark, locLandmarkDistance, locConfidenceText, locPoseConfidenceText, locPositionDrift, locLandmarkMatchScore, localizationConfidenceData)
                    VoiceTelemetry(voiceMetadata, lastCommand, suppressedCommandsCount, generatedCommandsCount)
                    HapticTelemetry(hapticMetadata, lastHapticPattern, generatedHapticCommandsCount, suppressedHapticCommandsCount)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "DEBUG OVERLAY",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceTelemetry(stats: AnalysisStats, frame: FrameMetadata?, bitmap: BitmapMetadata?) {
    Text(
        text = "Performance",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Frames: ${stats.framesReceived}",
        color = Color.Green,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace
    )
    Text(
        text = "FPS: ${String.format(Locale.US, "%.1f", stats.fps)}",
        color = if (stats.fps > 25) Color.Green else Color.Yellow,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace
    )
    frame?.let {
        Text(
            text = "Camera: ${it.width}x${it.height} (${it.rotationDegrees}°)",
            color = Color.Cyan,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
    bitmap?.let {
        Text(
            text = "Bitmap: ${it.width}x${it.height} (${it.conversionTimeMs}ms)",
            color = Color.Cyan,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Luminance: ${String.format(Locale.US, "%.1f", it.luminance)}",
            color = if (it.luminance < 20f) Color.Yellow else Color.Green,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        it.errorMessage?.let { err ->
            Text(text = "Error: $err", color = Color.Red, fontSize = 12.sp)
        }
    }
}

@Composable
fun InferenceTelemetry(iMeta: InferenceMetadata?, tMeta: TensorMetadata?) {
    tMeta?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "YOLO Preprocessing", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Tensor: ${it.shape.contentToString()}", color = Color.Cyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Time: ${it.preprocessingTimeMs} ms", color = if (it.preprocessingTimeMs < 10) Color.Green else Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
    iMeta?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "YOLO Inference", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Output: ${it.outputName}", color = Color.Cyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Time: ${it.inferenceTimeMs} ms", color = if (it.inferenceTimeMs < 30) Color.Green else Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun DetectionTelemetry(dMeta: DetectionMetadata?) {
    dMeta?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "YOLO Parsing", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Candidates: ${it.totalCandidates}", color = Color.Cyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Max Conf: ${String.format(Locale.US, "%.2f", it.maxConfidence)}", color = Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Time: ${it.parsingTimeMs} ms", color = if (it.parsingTimeMs < 5) Color.Green else Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun FilteringTelemetry(nMeta: NmsMetadata?) {
    nMeta?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "NMS Filtering", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Detections: ${it.finalDetectionCount}", color = Color.Green, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Time: ${it.nmsTimeMs} ms", color = if (it.nmsTimeMs < 3) Color.Green else Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun TrackingTelemetry(tStats: TrackMetadata?) {
    tStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Object Tracking", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Active Tracks: ${it.activeTracks}", color = Color.Green, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Time: ${it.processingTimeMs} ms", color = if (it.processingTimeMs < 2) Color.Green else Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SpatialTelemetry(sStats: RelativePositionMetadata?, list: List<RelativePosition>, dists: List<DistanceEstimate>) {
    sStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Spatial Awareness", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Objects: ${it.processedObjects}", color = Color.Cyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        list.take(3).forEach { pos ->
            val d = dists.find { it.trackId == pos.trackId }?.distanceMeters ?: 0f
            Text(text = "#${pos.trackId} ${pos.horizontalZone.name} (${String.format(Locale.US, "%.1fm", d)})", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun DistanceTelemetry(dStats: DistanceMetadata?) {
    dStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Distance Estimation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Avg Dist: ${String.format(Locale.US, "%.1fm", it.averageDistanceMeters)}", color = Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun TtcTelemetry(tStats: TtcMetadata?) {
    tStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Time-To-Collision", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Critical Risks: ${it.criticalRiskCount}", color = if (it.criticalRiskCount > 0) Color.Red else Color.Green, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ThreatTelemetry(tStats: ThreatMetadata?) {
    tStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Obstacle Prioritization", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Highest Score: ${String.format(Locale.US, "%.1f", it.highestPriorityScore)}", color = Color.Yellow, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CorridorTelemetry(cStats: CorridorMetadata?, list: List<SafeCorridor>) {
    cStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Safe Corridors", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        list.forEach { corr ->
            val color = when(corr.state) { CorridorState.SAFE -> Color.Green; CorridorState.CAUTION -> Color.Yellow; else -> Color.Red }
            Text(text = "${corr.horizontalZone.name}: ${corr.state.name}", color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun GuidanceTelemetry(gStats: GuidanceMetadata?, decision: GuidanceDecision?) {
    gStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Guidance Decision", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        decision?.let { dec ->
            Text(text = "Action: ${dec.action.name}", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = "Reason: ${dec.reason}", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun EnvironmentTelemetry(eStats: EnvironmentMetadata?, top: String, conf: Float, count: Int) {
    eStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Environment", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Top: $top (${String.format(Locale.US, "%.0f%%", conf * 100)})", color = Color.Green, fontSize = 14.sp)
    }
}

@Composable
fun MemoryTelemetry(mStats: MemoryMetadata?, active: Int, new: Int, updated: Int, expired: Int) {
    mStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Scene Memory", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Active: $active, N/U/E: $new/$updated/$expired", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun WorldModelTelemetry(wStats: WorldModelMetadata?, landmarks: Int, relations: Int, nearest: String, context: String) {
    wStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "World Model", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Nearest: $nearest, Context: $context", color = Color.Yellow, fontSize = 14.sp)
    }
}

@Composable
fun ContextTelemetry(cStats: NavigationContextMetadata?, primary: String, secondary: String, approaching: String, active: String, priority: String) {
    cStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Navigation Context", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Active: $active", color = Color.Green, fontSize = 14.sp)
        Text(text = "Primary: $primary", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun RouteTelemetry(rStats: RouteMetadata?, tracked: Int, passed: Int, revisited: Int, current: String, progress: String) {
    rStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Route Memory", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Current: $current, Status: $progress", color = Color.Yellow, fontSize = 14.sp)
    }
}

@Composable
fun GraphTelemetry(gStats: RouteGraphMetadata?, plan: RoutePlan?) {
    gStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Route Graph", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Nodes/Edges: ${it.nodeCount}/${it.edgeCount}", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun MapTelemetry(mStats: MapMetadata?) {
    mStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Persistent Maps", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Map ID: ${it.mapId}, Ver: ${it.version}", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun LandmarkNavTelemetry(nStats: LandmarkNavigationMetadata?, dest: String, status: String, dist: String, step: String) {
    nStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Landmark Navigation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Status: $status, Next: $step", color = Color.Green, fontSize = 14.sp)
    }
}

@Composable
fun GpsTelemetry(gStats: GpsNavigationMetadata?, dest: String, status: String, dist: String, step: String) {
    gStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "GPS Navigation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Dist: $dist, Step: $step", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun HybridTelemetry(hStats: HybridNavigationMetadata?, mode: String, status: String, completion: String, instruction: String) {
    hStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Hybrid Navigation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Mode: $mode, Completion: $completion", color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun EntranceTelemetry(eStats: EntranceMetadata?, decision: String, candidates: Int, confidence: String, gps: String, door: String) {
    eStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Entrance Detection", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Decision: $decision, Conf: $confidence", color = Color.Green, fontSize = 14.sp)
    }
}

@Composable
fun SlamTelemetry(sStats: SlamMetadata?, pos: String, heading: String, feat: Int, matched: Int, map: Int, trackConf: String, poseConf: String) {
    sStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Visual SLAM", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Pos: $pos, Track: $trackConf", color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun LocalizationTelemetry(lStats: LocalizationMetadata?, pos: LocalizedPosition?, room: String, nearest: String, dist: String, conf: String, pose: String, drift: String, score: String, confData: LocalizationConfidence?) {
    lStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Indoor Localization", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Room: $room, Conf: $conf", color = Color.Green, fontSize = 14.sp)
    }
}

@Composable
fun VoiceTelemetry(vStats: VoiceMetadata?, last: String, suppressed: Int, generated: Int) {
    vStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Voice Guidance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Last: $last", color = Color.Yellow, fontSize = 14.sp)
    }
}

@Composable
fun HapticTelemetry(hStats: HapticMetadata?, last: String, generated: Int, suppressed: Int) {
    hStats?.let {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Haptic Guidance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "Last: $last", color = Color.Yellow, fontSize = 14.sp)
    }
}
