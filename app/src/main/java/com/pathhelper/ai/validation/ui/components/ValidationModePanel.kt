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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.pathhelper.ai.validation.session.ValidationSessionManager
import com.pathhelper.ai.validation.session.ValidationTestType

// ─── Palette (shared with ValidationDashboard) ────────────────────────────────
private val BgPanel     = Color(0xCC0D1117)
private val BgHeader    = Color(0xEE161B22)
private val AccentCyan  = Color(0xFF00E5FF)
private val AccentGreen = Color(0xFF69FF47)
private val AccentRed   = Color(0xFFFF5252)
private val AccentOrange= Color(0xFFFF9100)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSec     = Color(0xFF8B949E)

/**
 * Debug-only collapsible Validation Mode panel.
 *
 * Shows a menu of available [ValidationTestType] options.
 * Tapping "Start" begins a [ValidationSessionManager] session.
 * Tapping "Stop" ends it and triggers CSV export automatically.
 *
 * Hidden in release builds.
 */
@Composable
fun ValidationModePanel(
    sessionManager: ValidationSessionManager,
    metricsProvider: () -> com.pathhelper.ai.validation.models.ValidationMetrics,
    modifier: Modifier = Modifier,
    forceVisible: Boolean = false
) {
    if (!BuildConfig.DEBUG && !forceVisible) return

    val isRunning by sessionManager.isRunning.collectAsState()
    val activeTest by sessionManager.activeTestType.collectAsState()
    val sampleCount by sessionManager.sampleCount.collectAsState()

    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
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
            Column {
                Text(
                    text = "🧪  Validation Mode",
                    color = AccentOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (isRunning) {
                    Text(
                        text = "● REC  ${activeTest?.label ?: ""}  [$sampleCount samples]",
                        color = AccentRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = if (expanded) "▲ hide" else "▼ show",
                color = TextSec,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── Content ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(BgPanel)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRunning) {
                    // ── Active session view ──────────────────────────────────────
                    Text(
                        text = "Active: ${activeTest?.label}",
                        color = AccentGreen,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Samples collected: $sampleCount",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { sessionManager.stopSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⏹  Stop & Export CSV",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // ── Test menu ────────────────────────────────────────────────
                    Text(
                        text = "Select a test to start recording:",
                        color = TextSec,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(2.dp))

                    ValidationTestType.values().forEach { testType ->
                        TestMenuButton(
                            testType = testType,
                            onClick = { sessionManager.startSession(testType, metricsProvider) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TestMenuButton(
    testType: ValidationTestType,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶  ${testType.label}",
                color = Color(0xFFE6EDF3),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (testType.targetDurationSeconds > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${testType.targetDurationSeconds / 60} min",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
