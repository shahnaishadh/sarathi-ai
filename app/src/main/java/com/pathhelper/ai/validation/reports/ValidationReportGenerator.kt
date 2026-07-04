package com.pathhelper.ai.validation.reports

import android.content.Context
import android.util.Log
import com.pathhelper.ai.validation.session.ValidationSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a structured plain-text summary report for a [ValidationSession].
 *
 * Explain:
 * * Purpose of the component: Summarizes session summary, performance (FPS/latencies), localization status, system status (memory/battery/temp), lighting conditions, announcement effectiveness, and exit criteria validation.
 * * Role within the Sarthi architecture: Provides post-session diagnostic reports to verify build quality and execution stats.
 * * Major inputs and outputs: Inputs a [ValidationSession] and its recorded metrics; outputs a structured text file in external logs storage.
 */
class ValidationReportGenerator(private val context: Context) {

    companion
object {
        private const val TAG = "PathHelper.ReportGen"
        private const val DIR = "validation/logs"
        private val TIMESTAMP_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val READABLE_FMT  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    /**
     * Generates and saves the report for the given [session].
     *
     * @return The [File] written, or null on error.
     */
    fun generate(session: ValidationSession): File? {
        return try {
            val dir = (context.getExternalFilesDir(null) ?: context.filesDir)
                .resolve(DIR).also { it.mkdirs() }

            val timestamp = TIMESTAMP_FMT.format(Date(session.startTimeMs))
            val filename = "${session.testType.filePrefix}-report-$timestamp.txt"
            val file = File(dir, filename)

            file.writeText(buildReport(session))
            Log.i(TAG, "Report written: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Report generation failed: ${e.localizedMessage}")
            null
        }
    }

    // ─── Private builder ──────────────────────────────────────────────────────

    private fun buildReport(s: ValidationSession): String {
        val samples = s.samples
        if (samples.isEmpty()) return "No samples collected."

        val avgFps         = samples.map { it.fps }.average()
        val minFps         = samples.minOf { it.fps }
        val avgYolo        = samples.map { it.yoloLatencyMs }.average()
        val avgTrack       = samples.map { it.trackingLatencyMs }.average()
        val avgSlam        = samples.map { it.slamLatencyMs }.average()
        val avgLoc         = samples.map { it.localizationLatencyMs }.average()
        val avgGuid        = samples.map { it.guidanceLatencyMs }.average()
        val avgCycle       = samples.map { it.yoloLatencyMs + it.trackingLatencyMs + it.slamLatencyMs + it.localizationLatencyMs }.average()

        val minLocConf     = samples.minOf { it.localizationConfidence }
        val avgLocConf     = samples.map { it.localizationConfidence }.average()
        val rooms          = samples.map { it.currentRoom }.distinct()
        val landmarks      = samples.map { it.nearestLandmark }.distinct()

        val maxMem         = samples.maxOf { it.peakMemoryMb }
        val startMem       = samples.first().usedMemoryMb
        val endMem         = samples.last().usedMemoryMb
        val memDelta       = endMem - startMem
        val minBatt        = samples.minOf { it.batteryPercent }
        val maxBatt        = samples.maxOf { it.batteryPercent }
        val battDrain      = maxBatt - minBatt
        val maxTemp        = samples.maxOf { it.batteryTemperature }

        val lightStates    = samples.map { it.lightingState }.distinct()
        val torchOnCount   = samples.count { it.torchEnabled }
        val torchOffCount  = samples.size - torchOnCount

        val generated      = s.totalGenerated
        val suppressed     = s.totalSuppressed
        val spoken         = samples.lastOrNull()?.announcementsSpoken ?: 0
        val suppRate       = if (generated > 0) suppressed * 100f / generated else 0f

        // Pass / Fail
        val fpsPass        = minFps >= 10f
        val cyclePass      = avgCycle < 100.0
        val locConfPass    = minLocConf >= 0.5f
        val memLeakPass    = memDelta < 100f
        val battPass       = battDrain <= 15f
        val tempPass       = maxTemp <= 45f

        return buildString {
            appendLine("═══════════════════════════════════════════════════════")
            appendLine(" Sarthi AI – Validation Report")
            appendLine("═══════════════════════════════════════════════════════")
            appendLine()
            appendLine("── Session Summary ──────────────────────────────────")
            appendLine("  Test:        ${s.testType.label}")
            appendLine("  Start:       ${READABLE_FMT.format(Date(s.startTimeMs))}")
            appendLine("  End:         ${READABLE_FMT.format(Date(s.endTimeMs))}")
            appendLine("  Duration:    ${s.durationSeconds} s")
            appendLine("  Samples:     ${samples.size}")
            appendLine()
            appendLine("── Performance ──────────────────────────────────────")
            appendLine("  Avg FPS:            ${"%.1f".format(avgFps)}")
            appendLine("  Min FPS:            ${"%.1f".format(minFps)}  ${pass(fpsPass)}")
            appendLine("  Avg YOLO:           ${"%.1f".format(avgYolo)} ms")
            appendLine("  Avg Tracking:       ${"%.1f".format(avgTrack)} ms")
            appendLine("  Avg SLAM:           ${"%.1f".format(avgSlam)} ms")
            appendLine("  Avg Localization:   ${"%.1f".format(avgLoc)} ms")
            appendLine("  Avg Guidance:       ${"%.1f".format(avgGuid)} ms")
            appendLine("  Avg Cycle:          ${"%.1f".format(avgCycle)} ms  ${pass(cyclePass)}")
            appendLine()
            appendLine("── Localization ─────────────────────────────────────")
            appendLine("  Avg Confidence:  ${"%.1f".format(avgLocConf * 100)}%")
            appendLine("  Min Confidence:  ${"%.1f".format(minLocConf * 100)}%  ${pass(locConfPass)}")
            appendLine("  Rooms visited:   ${rooms.joinToString()}")
            appendLine("  Landmarks seen:  ${landmarks.joinToString()}")
            appendLine()
            appendLine("── System ────────────────────────────────────────────")
            appendLine("  Start Memory:   ${"%.1f".format(startMem)} MB")
            appendLine("  End Memory:     ${"%.1f".format(endMem)} MB")
            appendLine("  Peak Memory:    ${"%.1f".format(maxMem)} MB")
            appendLine("  Memory Delta:   ${"%.1f".format(memDelta)} MB  ${pass(memLeakPass)}")
            appendLine("  Battery Drain:  ${"%.1f".format(battDrain)}%  ${pass(battPass)}")
            appendLine("  Max Temp:       ${"%.1f".format(maxTemp)} °C  ${pass(tempPass)}")
            appendLine()
            appendLine("── Lighting & Torch ─────────────────────────────────")
            appendLine("  Lighting States:  ${lightStates.joinToString()}")
            appendLine("  Torch ON frames:  $torchOnCount / ${samples.size}")
            appendLine()
            appendLine("── Announcement Manager ─────────────────────────────")
            appendLine("  Generated:   $generated")
            appendLine("  Spoken:      $spoken")
            appendLine("  Suppressed:  $suppressed")
            appendLine("  Suppression: ${"%.1f".format(suppRate)}%")
            appendLine()
            appendLine("── Exit Criteria Evaluation ──────────────────────────")
            appendLine("  FPS ≥ 10:             ${pass(fpsPass)}")
            appendLine("  Cycle < 100 ms:       ${pass(cyclePass)}")
            appendLine("  Min Conf ≥ 0.5:       ${pass(locConfPass)}")
            appendLine("  No memory leak:       ${pass(memLeakPass)}")
            appendLine("  Battery ≤ 15%:        ${pass(battPass)}")
            appendLine("  Temp ≤ 45 °C:         ${pass(tempPass)}")
            appendLine()
            appendLine("═══════════════════════════════════════════════════════")
        }
    }

    private fun pass(ok: Boolean) = if (ok) "✓ PASS" else "✗ FAIL"
}
