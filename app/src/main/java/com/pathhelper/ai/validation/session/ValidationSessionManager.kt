package com.pathhelper.ai.validation.session

import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.validation.models.ValidationMetrics
import com.pathhelper.ai.validation.reports.CsvExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle of a physical validation session.
 *
 * ## Usage
 * ```
 * sessionManager.startSession(ValidationTestType.HALLWAY_WALK, metricsProvider)
 * // … tester walks …
 * sessionManager.stopSession(context)   // exports CSV automatically
 * ```
 *
 * @param csvExporter  Used to write the CSV file when a session ends.
 * @param sampleIntervalMs  How often metrics are sampled (default 2 s).
 */
class ValidationSessionManager(
    private val csvExporter: CsvExporter,
    private val sampleIntervalMs: Long = 2_000L
) {

    companion
object {
        private const val TAG = "PathHelper.SessionMgr"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var samplerJob: Job? = null

    // Mutable session state
    private var currentTestType: ValidationTestType? = null
    private var sessionStartMs = 0L
    private val collectedSamples = mutableListOf<ValidationMetrics>()

    // ── Public observable state ────────────────────────────────────────────────

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeTestType = MutableStateFlow<ValidationTestType?>(null)
    val activeTestType: StateFlow<ValidationTestType?> = _activeTestType.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    /** Last completed session (available after [stopSession]). */
    var lastSession: ValidationSession? = null
        private set

    // ── Session control ────────────────────────────────────────────────────────

    /**
     * Starts a new session.
     *
     * @param testType         The test being performed.
     * @param metricsProvider  Lambda called on the sampling thread to obtain the current metrics.
     *                         Use `{ validationViewModel.metrics.value }` in practice.
     */
    fun startSession(
        testType: ValidationTestType,
        metricsProvider: () -> ValidationMetrics
    ) {
        if (_isRunning.value) {
            Log.w(TAG, "Session already running – ignoring startSession(${testType.label})")
            return
        }

        currentTestType = testType
        sessionStartMs = System.currentTimeMillis()
        collectedSamples.clear()

        _isRunning.value = true
        _activeTestType.value = testType
        _sampleCount.value = 0

        if (BuildConfig.DEBUG) Log.d(TAG, "Session STARTED: ${testType.label}")

        samplerJob = scope.launch {
            while (_isRunning.value) {
                val snapshot = metricsProvider()
                synchronized(collectedSamples) { collectedSamples.add(snapshot) }
                _sampleCount.value = collectedSamples.size
                if (BuildConfig.DEBUG) Log.v(TAG, "Sample #${_sampleCount.value} fps=${snapshot.fps}")
                delay(sampleIntervalMs)
            }
        }

        // Auto-stop if testType has a fixed duration
        val target = testType.targetDurationSeconds
        if (target > 0) {
            scope.launch {
                delay(target * 1_000L)
                if (_isRunning.value) {
                    Log.d(TAG, "Auto-stopping session after ${target}s")
                    stopSession()
                }
            }
        }
    }

    /**
     * Stops the active session, assembles a [ValidationSession], and exports a CSV.
     *
     * Safe to call even if no session is running.
     */
    fun stopSession() {
        if (!_isRunning.value) return

        samplerJob?.cancel()
        samplerJob = null
        _isRunning.value = false

        val endMs = System.currentTimeMillis()
        val samples = synchronized(collectedSamples) { collectedSamples.toList() }

        val session = ValidationSession(
            testType = currentTestType ?: ValidationTestType.STATIC_STABILITY,
            startTimeMs = sessionStartMs,
            endTimeMs = endMs,
            samples = samples
        )
        lastSession = session

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Session ENDED: ${session.testType.label} " +
                    "duration=${session.durationSeconds}s samples=${samples.size}")
        }

        // Export CSV on IO thread
        scope.launch(Dispatchers.IO) {
            csvExporter.export(session)
        }

        _activeTestType.value = null
    }
}
