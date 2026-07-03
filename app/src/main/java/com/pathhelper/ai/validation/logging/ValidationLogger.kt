package com.pathhelper.ai.validation.logging

import android.util.Log
import com.pathhelper.ai.validation.models.ValidationMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Central validation logger.
 *
 * Writes a concise Logcat line on each call and emits the full snapshot
 * to [metricsFlow] so downstream observers (e.g. a Room DAO) can persist
 * rows for future CSV export.
 *
 * CSV column order (for reference):
 * timestamp, fps, trackedObjects, navigationMode,
 * localizationState, currentRoom, nearestLandmark,
 * localizationConfidence, poseConfidence, poseX, poseY, heading,
 * yoloLatencyMs, trackingLatencyMs, slamLatencyMs, localizationLatencyMs, guidanceLatencyMs,
 * batteryPercent, batteryTemperature, usedMemoryMb, peakMemoryMb,
 * lightingState, brightnessScore, torchEnabled,
 * announcementsGenerated, announcementsSpoken, announcementsSuppressed, suppressionRate
 */
object ValidationLogger {

    private const val TAG = "PathHelper.Validation"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _metricsFlow = MutableSharedFlow<ValidationMetrics>(extraBufferCapacity = 64)
    /** Collect this to persist snapshots to Room or write to a CSV file. */
    val metricsFlow: SharedFlow<ValidationMetrics> = _metricsFlow.asSharedFlow()

    fun log(metrics: ValidationMetrics) {
        // Logcat — single formatted line for quick adb logcat inspection
        Log.d(
            TAG,
            buildString {
                append("ts=${metrics.timestampMs} ")
                append("fps=${"%.1f".format(metrics.fps)} ")
                append("room=${metrics.currentRoom} ")
                append("landmark=${metrics.nearestLandmark} ")
                append("locConf=${"%.2f".format(metrics.localizationConfidence)} ")
                append("poseConf=${"%.2f".format(metrics.poseConfidence)} ")
                append("x=${"%.2f".format(metrics.poseX)} y=${"%.2f".format(metrics.poseY)} ")
                append("yolo=${metrics.yoloLatencyMs}ms ")
                append("track=${metrics.trackingLatencyMs}ms ")
                append("slam=${metrics.slamLatencyMs}ms ")
                append("loc=${metrics.localizationLatencyMs}ms ")
                append("guid=${metrics.guidanceLatencyMs}ms ")
                append("mem=${"%.1f".format(metrics.usedMemoryMb)}MB ")
                append("peak=${"%.1f".format(metrics.peakMemoryMb)}MB ")
                append("batt=${"%.0f".format(metrics.batteryPercent)}% ")
                append("temp=${"%.1f".format(metrics.batteryTemperature)}C ")
                append("light=${metrics.lightingState} ")
                append("torch=${metrics.torchEnabled} ")
                append("gen=${metrics.announcementsGenerated} ")
                append("spoken=${metrics.announcementsSpoken} ")
                append("supp=${metrics.announcementsSuppressed}")
            }
        )

        // Emit for persistence – non-blocking, drops if buffer full to avoid blocking camera
        scope.launch { _metricsFlow.tryEmit(metrics) }
    }

    /**
     * Returns a CSV header line matching the column order used in log().
     * Useful when opening a new export file.
     */
    fun csvHeader(): String =
        "timestamp,fps,trackedObjects,navigationMode," +
        "localizationState,currentRoom,nearestLandmark," +
        "localizationConfidence,poseConfidence,poseX,poseY,heading," +
        "yoloLatencyMs,trackingLatencyMs,slamLatencyMs,localizationLatencyMs,guidanceLatencyMs," +
        "batteryPercent,batteryTemperature,usedMemoryMb,peakMemoryMb," +
        "lightingState,brightnessScore,torchEnabled," +
        "announcementsGenerated,announcementsSpoken,announcementsSuppressed,suppressionRate"

    /** Serialises a [ValidationMetrics] snapshot to a CSV row. */
    fun toCsvRow(m: ValidationMetrics): String =
        "${m.timestampMs},${m.fps},${m.trackedObjects},${m.navigationMode}," +
        "${m.localizationState},${m.currentRoom},${m.nearestLandmark}," +
        "${m.localizationConfidence},${m.poseConfidence},${m.poseX},${m.poseY},${m.heading}," +
        "${m.yoloLatencyMs},${m.trackingLatencyMs},${m.slamLatencyMs},${m.localizationLatencyMs},${m.guidanceLatencyMs}," +
        "${m.batteryPercent},${m.batteryTemperature},${m.usedMemoryMb},${m.peakMemoryMb}," +
        "${m.lightingState},${m.brightnessScore},${m.torchEnabled}," +
        "${m.announcementsGenerated},${m.announcementsSpoken},${m.announcementsSuppressed},${m.suppressionRate}"
}
