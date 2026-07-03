package com.pathhelper.ai.validation.session

import com.pathhelper.ai.validation.models.ValidationMetrics

/**
 * Immutable snapshot of a completed validation session.
 *
 * Created by [ValidationSessionManager] when a session ends.
 * Consumed by [com.pathhelper.ai.validation.reports.CsvExporter] for CSV output.
 *
 * @param testType        Which test was performed.
 * @param startTimeMs     Wall-clock start timestamp (epoch ms).
 * @param endTimeMs       Wall-clock end timestamp (epoch ms).
 * @param samples         All metric snapshots captured during the session.
 * @param notes           Optional human-entered notes appended to the report.
 */
data
class ValidationSession(
    val testType: ValidationTestType,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val samples: List<ValidationMetrics>,
    val notes: String = ""
) {
    /** Duration in whole seconds. */
    val durationSeconds: Long get() = (endTimeMs - startTimeMs) / 1_000L

    /** Average FPS over the entire session. */
    val averageFps: Float
        get() = if (samples.isEmpty()) 0f else samples.map { it.fps }.average().toFloat()

    /** Peak memory (MB) recorded during the session. */
    val peakMemoryMb: Float
        get() = samples.maxOfOrNull { it.peakMemoryMb } ?: 0f

    /** Minimum localization confidence recorded during the session. */
    val minLocalizationConfidence: Float
        get() = samples.minOfOrNull { it.localizationConfidence } ?: 0f

    /** Total announcements generated across the session. */
    val totalGenerated: Int
        get() = samples.lastOrNull()?.announcementsGenerated ?: 0

    /** Total announcements suppressed across the session. */
    val totalSuppressed: Int
        get() = samples.lastOrNull()?.announcementsSuppressed ?: 0

    /** Overall suppression rate across the session. */
    val overallSuppressionRate: Float
        get() = if (totalGenerated > 0) totalSuppressed.toFloat() / totalGenerated else 0f
}
