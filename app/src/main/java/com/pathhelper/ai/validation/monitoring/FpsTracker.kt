package com.pathhelper.ai.validation.monitoring

import android.os.SystemClock

/**
 * Lightweight FPS tracker using a circular buffer of frame timestamps.
 *
 * Design goals:
 * - Zero heap allocations on the hot path ([recordFrame])
 * - Thread-compatible: [recordFrame] and read accessors may be called from any single thread
 * - Provides last FPS, moving average FPS (over last [windowSize] frames), and cumulative average
 *
 * @param windowSize Number of frames in the moving average window. Defaults to 60.
 */
class FpsTracker(private val windowSize: Int = 60) {

    // Circular buffer of elapsedRealtime timestamps in milliseconds
    private val timestamps = LongArray(windowSize)
    private var head = 0
    private var count = 0

    private var totalFrames = 0L
    private var sessionStartMs = 0L
    private var lastFrameMs = 0L

    /** FPS computed between the most recent consecutive pair of frames. */
    var lastFps: Float = 0f
        private set

    /** Moving average FPS over the last [windowSize] frames. */
    var movingAverageFps: Float = 0f
        private set

    /** Cumulative average FPS since the first [recordFrame] call. */
    var averageFps: Float = 0f
        private set

    /**
     * Records a new frame at the current system clock time.
     * Must be called once per frame on the analysis thread.
     * No heap allocations occur inside this method.
     */
    fun recordFrame() {
        val now = SystemClock.elapsedRealtime()

        if (sessionStartMs == 0L) {
            sessionStartMs = now
        }

        // Last FPS — interval between this frame and the previous
        if (lastFrameMs != 0L) {
            val intervalMs = now - lastFrameMs
            lastFps = if (intervalMs > 0) 1000f / intervalMs else 0f
        }
        lastFrameMs = now

        // Circular buffer update (no allocation)
        timestamps[head] = now
        head = (head + 1) % windowSize
        if (count < windowSize) count++

        // Moving average FPS — oldest timestamp in the window vs now
        if (count >= 2) {
            val oldestIndex = if (count < windowSize) 0 else head
            val oldestMs = timestamps[oldestIndex]
            val windowMs = now - oldestMs
            movingAverageFps = if (windowMs > 0) ((count - 1).toFloat() * 1000f) / windowMs else 0f
        }

        // Cumulative average
        totalFrames++
        val sessionMs = now - sessionStartMs
        averageFps = if (sessionMs > 0) (totalFrames.toFloat() * 1000f) / sessionMs else 0f
    }

    /** Resets all counters (e.g., after a camera restart). */
    fun reset() {
        timestamps.fill(0L)
        head = 0
        count = 0
        totalFrames = 0L
        sessionStartMs = 0L
        lastFrameMs = 0L
        lastFps = 0f
        movingAverageFps = 0f
        averageFps = 0f
    }
}
