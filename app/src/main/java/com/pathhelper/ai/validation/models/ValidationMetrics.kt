package com.pathhelper.ai.validation.models

import java.time.Instant

/**
 * Immutable
data class representing a snapshot of validation metrics.
 * All fields are read‑only to ensure thread‑safety when shared via StateFlow.
 */
data
class ValidationMetrics(
    // Timestamp of the snapshot in epoch milliseconds
    val timestampMs: Long = Instant.now().toEpochMilli(),

    // ------------------- Performance -------------------
    val fps: Float = 0f,
    val trackedObjects: Int = 0,
    val navigationMode: String = "",

    // ------------------- Localization -------------------
    val localizationState: String = "",
    val currentRoom: String = "",
    val nearestLandmark: String = "",
    val localizationConfidence: Float = 0f,
    val poseConfidence: Float = 0f,
    val poseX: Float = 0f,
    val poseY: Float = 0f,
    val heading: Float = 0f,

    // ------------------- Latency (ms) -------------------
    val yoloLatencyMs: Long = 0L,
    val trackingLatencyMs: Long = 0L,
    val slamLatencyMs: Long = 0L,
    val localizationLatencyMs: Long = 0L,
    val guidanceLatencyMs: Long = 0L,

    // ------------------- System -------------------
    val batteryPercent: Float = 0f,
    val batteryTemperature: Float = 0f,
    val usedMemoryMb: Float = 0f,
    val peakMemoryMb: Float = 0f,

    // ------------------- Lighting -------------------
    val lightingState: String = "",
    val brightnessScore: Float = 0f,
    val torchEnabled: Boolean = false,

    // ------------------- Guidance -------------------
    val announcementsGenerated: Int = 0,
    val announcementsSpoken: Int = 0,
    val announcementsSuppressed: Int = 0,
    val suppressionRate: Float = 0f
)
