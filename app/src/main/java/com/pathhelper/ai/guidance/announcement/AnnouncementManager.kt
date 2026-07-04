package com.pathhelper.ai.guidance.announcement

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.navigation.GuidanceAction
import com.pathhelper.ai.navigation.ThreatLevel
import kotlin.math.sqrt

/**
 * Central announcement manager for voice guidance.
 *
 * ## Suppression Rules (by priority)
 *
 * | Priority | Cooldown | Duplicate Window | Movement Threshold |
 * |----------|----------|------------------|--------------------|
 * | CRITICAL | none     | none             | none               |
 * | WARNING  | 3 s      | 5 s              | 0.5 m              |
 * | INFO     | 10 s     | 5 s              | 0.5 m              |
 *
 * ## Design Decisions
 * - Thread-safe via @Synchronized where state is mutated.
 * - No heap allocation in the hot path (uses primitive vars).
 * - speak() is a lambda so the manager has no TTS dependency.
 */
class AnnouncementManager(
    private val speak: (String, GuidanceAction, Int) -> Unit
) {

    companion
object {
        private const val TAG = "PathHelper.Announcement"

        private const val DUPLICATE_WINDOW_MS = 5_000L
        private const val WARNING_COOLDOWN_MS = 5_000L
        private const val INFO_COOLDOWN_MS    = 20_000L
        private const val MOVEMENT_THRESHOLD_M = 0.5f
    }

    private val state = AnnouncementState()

    // Last spoken tracking (per-priority cooldown)
    private var lastWarningText = ""
    private var lastWarningTimeMs = 0L
    private var lastInfoText = ""
    private var lastInfoTimeMs = 0L

    // Last position at time of last spoken (any priority)
    private var lastSpokenX = 0f
    private var lastSpokenY = 0f
    private var lastSpokenText = ""
    private var lastSpokenTimeMs = 0L

    @Synchronized
    fun announce(
        text: String,
        priority: AnnouncementPriority,
        currentPosX: Float = 0f,
        currentPosY: Float = 0f,
        frameId: Long = 0L,
        trackerId: Int? = null,
        action: GuidanceAction? = null,
        distance: Float? = null,
        threatLevel: ThreatLevel? = null,
        rawPriority: Int = 50
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "QUEUED: \"$text\" priority=$priority")
        val now = SystemClock.elapsedRealtime()
        state.generated++

        // ── Duplicate suppression ─────────────────────────────────────────────
        if (text == lastSpokenText && (now - lastSpokenTimeMs) < DUPLICATE_WINDOW_MS) {
            suppress(text, "duplicate")
            Log.i("SARTHI_DEBUG", """
                [ANNOUNCEMENT_MANAGER]
                time=${System.currentTimeMillis()}
                frameId=$frameId
                trackerId=${trackerId ?: "null"}
                action=${action ?: "null"}
                distance=${distance ?: "null"}
                threatLevel=${threatLevel ?: "null"}
                suppressed=true
                text="$text"
                reason=duplicate
            """.trimIndent())
            return
        }

        if (priority == AnnouncementPriority.CRITICAL) {
            doSpeak(text, now, currentPosX, currentPosY, action ?: GuidanceAction.STOP, rawPriority)
            Log.i("SARTHI_DEBUG", """
                [ANNOUNCEMENT_MANAGER]
                time=${System.currentTimeMillis()}
                frameId=$frameId
                trackerId=${trackerId ?: "null"}
                action=${action ?: "null"}
                distance=${distance ?: "null"}
                threatLevel=${threatLevel ?: "null"}
                suppressed=false
                text="$text"
            """.trimIndent())
            return
        }

        // ── Movement threshold ────────────────────────────────────────────────
        val moved = distance(currentPosX, currentPosY, lastSpokenX, lastSpokenY)
        val belowMovementThreshold = moved < MOVEMENT_THRESHOLD_M

        // ── Priority-specific cooldowns ───────────────────────────────────────
        when (priority) {
            AnnouncementPriority.WARNING -> {
                val elapsed = now - lastWarningTimeMs
                if (belowMovementThreshold && elapsed < WARNING_COOLDOWN_MS) {
                    suppress(text, "WARNING cooldown ${elapsed}ms < ${WARNING_COOLDOWN_MS}ms, moved ${moved}m")
                    Log.i("SARTHI_DEBUG", """
                        [ANNOUNCEMENT_MANAGER]
                        time=${System.currentTimeMillis()}
                        frameId=$frameId
                        trackerId=${trackerId ?: "null"}
                        action=${action ?: "null"}
                        distance=${distance ?: "null"}
                        threatLevel=${threatLevel ?: "null"}
                        suppressed=true
                        text="$text"
                        reason=WARNING cooldown
                    """.trimIndent())
                    return
                }
                lastWarningText = text
                lastWarningTimeMs = now
            }
            AnnouncementPriority.INFO -> {
                val elapsed = now - lastInfoTimeMs
                if (belowMovementThreshold && elapsed < INFO_COOLDOWN_MS) {
                    suppress(text, "INFO cooldown ${elapsed}ms < ${INFO_COOLDOWN_MS}ms, moved ${moved}m")
                    Log.i("SARTHI_DEBUG", """
                        [ANNOUNCEMENT_MANAGER]
                        time=${System.currentTimeMillis()}
                        frameId=$frameId
                        trackerId=${trackerId ?: "null"}
                        action=${action ?: "null"}
                        distance=${distance ?: "null"}
                        threatLevel=${threatLevel ?: "null"}
                        suppressed=true
                        text="$text"
                        reason=INFO cooldown
                    """.trimIndent())
                    return
                }
                lastInfoText = text
                lastInfoTimeMs = now
            }
            else -> { /* CRITICAL handled above */ }
        }

        doSpeak(text, now, currentPosX, currentPosY, action ?: GuidanceAction.KEEP_CENTER, rawPriority)
        Log.i("SARTHI_DEBUG", """
            [ANNOUNCEMENT_MANAGER]
            time=${System.currentTimeMillis()}
            frameId=$frameId
            trackerId=${trackerId ?: "null"}
            action=${action ?: "null"}
            distance=${distance ?: "null"}
            threatLevel=${threatLevel ?: "null"}
            suppressed=false
            text="$text"
        """.trimIndent())
    }

    /** Returns an immutable snapshot of current counters. */
    @Synchronized
    fun snapshot(): AnnouncementState = state.copy()

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun doSpeak(text: String, nowMs: Long, x: Float, y: Float, action: GuidanceAction, rawPriority: Int) {
        speak(text, action, rawPriority)
        state.spoken++
        lastSpokenText = text
        lastSpokenTimeMs = nowMs
        lastSpokenX = x
        lastSpokenY = y
        if (BuildConfig.DEBUG) Log.d(TAG, "SPOKEN: \"$text\"")
    }

    private fun suppress(text: String, reason: String) {
        state.suppressed++
        if (BuildConfig.DEBUG) Log.d(TAG, "SUPPRESSED: \"$text\" reason=$reason")
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
