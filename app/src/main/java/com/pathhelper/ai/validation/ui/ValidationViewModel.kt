package com.pathhelper.ai.validation.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pathhelper.ai.localization.pose.LocalizationConfidence
import com.pathhelper.ai.localization.pose.LocalizedPosition
import com.pathhelper.ai.localization.slam.SlamMetadata
import com.pathhelper.ai.validation.logging.ValidationLogger
import com.pathhelper.ai.validation.models.ValidationMetrics
import com.pathhelper.ai.validation.monitoring.FpsTracker
import com.pathhelper.ai.validation.monitoring.MemoryMonitor
import com.pathhelper.ai.validation.monitoring.SystemHealthMonitor
import com.pathhelper.ai.validation.reports.CsvExporter
import com.pathhelper.ai.validation.reports.ValidationReportGenerator
import com.pathhelper.ai.validation.session.ValidationSessionManager
import com.pathhelper.ai.voice.VoiceMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Aggregates all validation monitors and exposes a single [StateFlow<ValidationMetrics>].
 *
 * CameraScreen observes [metrics] only — no business logic lives in Compose.
 *
 * Domain-specific fields (localization, SLAM, voice, lighting, announcements)
 * are pushed by the camera pipeline via the update* methods, keeping them
 * decoupled from the monitor flows.
 */
class ValidationViewModel(context: Context) : ViewModel() {

    // ─── Internal mutable fields updated by the camera pipeline ──────────────

    @Volatile private var locState: String = ""
    @Volatile private var currentRoom: String = ""
    @Volatile private var nearestLandmark: String = ""
    @Volatile private var locConfidence: Float = 0f
    @Volatile private var poseConfidence: Float = 0f
    @Volatile private var poseX: Float = 0f
    @Volatile private var poseY: Float = 0f
    @Volatile private var heading: Float = 0f

    @Volatile private var yoloLatencyMs: Long = 0L
    @Volatile private var trackingLatencyMs: Long = 0L
    @Volatile private var slamLatencyMs: Long = 0L
    @Volatile private var localizationLatencyMs: Long = 0L
    @Volatile private var guidanceLatencyMs: Long = 0L

    @Volatile private var trackedObjects: Int = 0
    @Volatile private var navigationMode: String = ""

    @Volatile private var lightingState: String = ""
    @Volatile private var brightnessScore: Float = 0f
    @Volatile private var torchEnabled: Boolean = false

    @Volatile private var announcementsGenerated: Int = 0
    @Volatile private var announcementsSpoken: Int = 0
    @Volatile private var announcementsSuppressed: Int = 0

    // ─── FPS tracker (one per ViewModel instance) ─────────────────────────────
    val fpsTracker = FpsTracker(windowSize = 30)

    // ─── Validation automation ────────────────────────────────────────────────
    val csvExporter = CsvExporter(context)
    val reportGenerator = ValidationReportGenerator(context)
    val sessionManager = ValidationSessionManager(
        csvExporter = csvExporter,
        sampleIntervalMs = 2_000L
    )

    // ─── Exposed state ────────────────────────────────────────────────────────
    private val _metrics = MutableStateFlow(ValidationMetrics())
    val metrics: StateFlow<ValidationMetrics> = _metrics.asStateFlow()

    init {
        MemoryMonitor.start()
        SystemHealthMonitor.start(context)

        // Combine monitor flows to drive periodic metric snapshots
        viewModelScope.launch {
            combine(
                MemoryMonitor.usedMemoryMb,
                MemoryMonitor.peakMemoryMb,
                SystemHealthMonitor.batteryPercent,
                SystemHealthMonitor.batteryTemperatureC
            ) { usedMem, peakMem, battPct, battTemp ->
                buildSnapshot(usedMem, peakMem, battPct, battTemp)
            }.collect { snapshot ->
                _metrics.value = snapshot
                ValidationLogger.log(snapshot)
            }
        }
    }

    // ─── Called once per camera frame ─────────────────────────────────────────

    /**
     * Must be called from the frame analysis callback after every processed frame.
     * Refreshes FPS and rebuilds the metrics snapshot with current field values.
     */
    fun onFrameProcessed() {
        fpsTracker.recordFrame()
        // Publish an updated snapshot immediately on every frame
        _metrics.value = buildSnapshot(
            usedMem = MemoryMonitor.usedMemoryMb.value,
            peakMem = MemoryMonitor.peakMemoryMb.value,
            battPct = SystemHealthMonitor.batteryPercent.value,
            battTemp = SystemHealthMonitor.batteryTemperatureC.value
        )
        ValidationLogger.log(_metrics.value)
    }

    /** Stops any active session and generates the report. Call from onCleared if needed. */
    override fun onCleared() {
        super.onCleared()
        if (sessionManager.isRunning.value) {
            sessionManager.stopSession()
        }
        sessionManager.lastSession?.let { session ->
            viewModelScope.launch(Dispatchers.IO) {
                reportGenerator.generate(session)
            }
        }
    }

    // ─── Domain update methods (called from FrameAnalyzer callback) ───────────

    fun updateLocalization(
        position: LocalizedPosition,
        confidence: LocalizationConfidence
    ) {
        locState = position.state.name
        currentRoom = position.currentRoom ?: "Unknown"
        nearestLandmark = position.nearestLandmark ?: "N/A"
        locConfidence = position.confidence
        poseConfidence = confidence.poseConfidence
    }

    fun updatePose(x: Float, y: Float, headingDeg: Float) {
        poseX = x
        poseY = y
        heading = headingDeg
    }

    fun updateLatencies(
        yolo: Long,
        tracking: Long,
        slam: Long,
        localization: Long,
        guidance: Long
    ) {
        yoloLatencyMs = yolo
        trackingLatencyMs = tracking
        slamLatencyMs = slam
        localizationLatencyMs = localization
        guidanceLatencyMs = guidance
    }

    fun updatePerformance(trackedObjs: Int, navMode: String) {
        trackedObjects = trackedObjs
        navigationMode = navMode
    }

    fun updateLighting(state: String, brightness: Float, torch: Boolean) {
        lightingState = state
        brightnessScore = brightness
        torchEnabled = torch
    }

    fun updateAnnouncements(generated: Int, spoken: Int, suppressed: Int) {
        announcementsGenerated = generated
        announcementsSpoken = spoken
        announcementsSuppressed = suppressed
    }

    // ─── Snapshot builder ─────────────────────────────────────────────────────

    private fun buildSnapshot(
        usedMem: Float,
        peakMem: Float,
        battPct: Float,
        battTemp: Float
    ) = ValidationMetrics(
        timestampMs = System.currentTimeMillis(),
        fps = fpsTracker.movingAverageFps,
        trackedObjects = trackedObjects,
        navigationMode = navigationMode,
        localizationState = locState,
        currentRoom = currentRoom,
        nearestLandmark = nearestLandmark,
        localizationConfidence = locConfidence,
        poseConfidence = poseConfidence,
        poseX = poseX,
        poseY = poseY,
        heading = heading,
        yoloLatencyMs = yoloLatencyMs,
        trackingLatencyMs = trackingLatencyMs,
        slamLatencyMs = slamLatencyMs,
        localizationLatencyMs = localizationLatencyMs,
        guidanceLatencyMs = guidanceLatencyMs,
        batteryPercent = battPct,
        batteryTemperature = battTemp,
        usedMemoryMb = usedMem,
        peakMemoryMb = peakMem,
        lightingState = lightingState,
        brightnessScore = brightnessScore,
        torchEnabled = torchEnabled,
        announcementsGenerated = announcementsGenerated,
        announcementsSpoken = announcementsSpoken,
        announcementsSuppressed = announcementsSuppressed,
        suppressionRate = if (announcementsGenerated > 0)
            announcementsSuppressed.toFloat() / announcementsGenerated
        else 0f
    )

    // ─── Factory ──────────────────────────────────────────────────────────────
    /**
     * Coordinates Factory operations and logic.
     *
     * Explain:
     * * Purpose of the component: Manages state and calculations for Factory.
     * * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
     * * Major inputs and outputs: Refer to member methods for input/output definitions.
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ValidationViewModel(context.applicationContext) as T
    }
}
