package com.pathhelper.ai.validation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.validation.models.ValidationMetrics
import java.util.Locale

// ─── Color palette ────────────────────────────────────────────────────────────
private val BgCard         = Color(0xCC0D1117)
private val BgHeader       = Color(0xEE161B22)
private val AccentCyan     = Color(0xFF00E5FF)
private val AccentGreen    = Color(0xFF69FF47)
private val AccentYellow   = Color(0xFFFFD740)
private val AccentRed      = Color(0xFFFF5252)
private val TextPrimary    = Color(0xFFE6EDF3)
private val TextSecondary  = Color(0xFF8B949E)
private val DividerColor   = Color(0xFF30363D)

// ─── Entry point ──────────────────────────────────────────────────────────────

/**
 * Collapsible debug-only validation dashboard.
 *
 * Default state: expanded in DEBUG builds, collapsed in RELEASE.
 * Pass [forceVisible] = true to override the debug guard (tests / screenshots).
 */
@Composable
fun ValidationDashboard(
    metrics: ValidationMetrics,
    modifier: Modifier = Modifier,
    forceVisible: Boolean = false
) {
    if (!BuildConfig.DEBUG && !forceVisible) return

    var expanded by rememberSaveable { mutableStateOf(BuildConfig.DEBUG) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // ── Header / toggle ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(BgHeader)
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "▣  Validation Dashboard",
                color = AccentCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (expanded) "▲ hide" else "▼ show",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── Cards ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(BgCard)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LocalizationCard(metrics)
                PerformanceCard(metrics)
                SystemHealthCard(metrics)
                LightingCard(metrics)
                GuidanceCard(metrics)
            }
        }
    }
}

// ─── Shared card shell ────────────────────────────────────────────────────────

@Composable
private fun DashCard(
    title: String,
    accentColor: Color = AccentCyan,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF161B22))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Individual cards ─────────────────────────────────────────────────────────

@Composable
fun LocalizationCard(metrics: ValidationMetrics) {
    val confColor = when {
        metrics.localizationConfidence >= 0.8f -> AccentGreen
        metrics.localizationConfidence >= 0.5f -> AccentYellow
        else                                    -> AccentRed
    }
    DashCard("◈  LOCALIZATION", AccentCyan) {
        MetricRow("State",       metrics.localizationState.ifBlank { "—" })
        MetricRow("Room",        metrics.currentRoom.ifBlank { "—" })
        MetricRow("Nearest",     metrics.nearestLandmark.ifBlank { "—" })
        MetricRow("Loc Conf",    "%.1f%%".format(metrics.localizationConfidence * 100), confColor)
        MetricRow("Pose Conf",   "%.1f%%".format(metrics.poseConfidence * 100), confColor)
        MetricRow("Pose X",      "%.2f m".format(metrics.poseX))
        MetricRow("Pose Y",      "%.2f m".format(metrics.poseY))
        MetricRow("Heading",     "%.1f°".format(metrics.heading))
    }
}

@Composable
fun PerformanceCard(metrics: ValidationMetrics) {
    val fpsColor = when {
        metrics.fps >= 15f -> AccentGreen
        metrics.fps >= 10f -> AccentYellow
        else               -> AccentRed
    }
    fun latColor(ms: Long) = when {
        ms < 50  -> AccentGreen
        ms < 100 -> AccentYellow
        else     -> AccentRed
    }
    DashCard("⚡  PERFORMANCE", AccentYellow) {
        MetricRow("FPS",              "%.1f".format(metrics.fps), fpsColor)
        MetricRow("YOLO Latency",     "${metrics.yoloLatencyMs} ms", latColor(metrics.yoloLatencyMs))
        MetricRow("Track Latency",    "${metrics.trackingLatencyMs} ms", latColor(metrics.trackingLatencyMs))
        MetricRow("SLAM Latency",     "${metrics.slamLatencyMs} ms", latColor(metrics.slamLatencyMs))
        MetricRow("Loc Latency",      "${metrics.localizationLatencyMs} ms", latColor(metrics.localizationLatencyMs))
        MetricRow("Guid Latency",     "${metrics.guidanceLatencyMs} ms", latColor(metrics.guidanceLatencyMs))
        MetricRow("Tracked Objects",  "${metrics.trackedObjects}")
        MetricRow("Nav Mode",         metrics.navigationMode.ifBlank { "—" })
    }
}

@Composable
fun SystemHealthCard(metrics: ValidationMetrics) {
    val battColor = when {
        metrics.batteryPercent >= 50f -> AccentGreen
        metrics.batteryPercent >= 20f -> AccentYellow
        else                          -> AccentRed
    }
    val tempColor = when {
        metrics.batteryTemperature < 40f -> AccentGreen
        metrics.batteryTemperature < 45f -> AccentYellow
        else                             -> AccentRed
    }
    DashCard("🔋  SYSTEM HEALTH", AccentGreen) {
        MetricRow("Battery",     "%.0f%%".format(metrics.batteryPercent), battColor)
        MetricRow("Temp",        "%.1f °C".format(metrics.batteryTemperature), tempColor)
        MetricRow("Used Mem",    "%.1f MB".format(metrics.usedMemoryMb))
        MetricRow("Peak Mem",    "%.1f MB".format(metrics.peakMemoryMb))
    }
}

@Composable
fun LightingCard(metrics: ValidationMetrics) {
    val stateColor = when (metrics.lightingState) {
        "BRIGHT" -> AccentGreen
        "NORMAL" -> TextPrimary
        "DIM"    -> AccentYellow
        "DARK"   -> AccentRed
        else     -> TextSecondary
    }
    DashCard("💡  LIGHTING", AccentYellow) {
        MetricRow("Lighting State",   metrics.lightingState.ifBlank { "—" }, stateColor)
        MetricRow("Brightness Score", "%.1f".format(metrics.brightnessScore))
        MetricRow("Torch State",      if (metrics.torchEnabled) "ON" else "OFF",
            if (metrics.torchEnabled) AccentYellow else TextSecondary)
        MetricRow("Auto Torch",       if (metrics.torchEnabled) "Enabled" else "Disabled")
    }
}

@Composable
fun GuidanceCard(metrics: ValidationMetrics) {
    val suppColor = when {
        metrics.suppressionRate > 0.5f -> AccentRed
        metrics.suppressionRate > 0.3f -> AccentYellow
        else                           -> AccentGreen
    }
    DashCard("🔊  GUIDANCE", AccentCyan) {
        MetricRow("Generated",         "${metrics.announcementsGenerated}")
        MetricRow("Spoken",            "${metrics.announcementsSpoken}")
        MetricRow("Suppressed",        "${metrics.announcementsSuppressed}")
        MetricRow("Suppression Rate",  "%.1f%%".format(metrics.suppressionRate * 100), suppColor)
    }
}
