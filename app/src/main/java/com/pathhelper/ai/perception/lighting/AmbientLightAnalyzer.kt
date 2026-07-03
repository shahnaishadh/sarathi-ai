package com.pathhelper.ai.perception.lighting

import android.graphics.ImageFormat
import android.media.Image

/**
 * Estimates scene brightness from a YUV_420_888 camera frame.
 *
 * Samples the Y-plane (luma) at a fixed stride to keep CPU cost minimal.
 * Returns a score in [0, 100] — higher means brighter.
 *
 * Design:
 * - No heap allocation per-call (uses the existing Image buffer directly).
 * - Max ~200 pixel samples per frame at default stride.
 * - Safe to call on the camera analysis thread.
 */
object AmbientLightAnalyzer {

    /**
     * Analyse a single [Image] and return a brightness score in [0.0, 100.0].
     *
     * Returns 0 if the image format is unsupported or the buffer is empty.
     */
    fun analyze(image: Image): Float {
        if (image.format != ImageFormat.YUV_420_888) return 0f

        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val remaining = buffer.remaining()
        if (remaining == 0) return 0f

        // Sample at a stride to limit cost; targets ~200 samples regardless of resolution
        val step = maxOf(1, remaining / 200)

        var sum = 0L
        var samples = 0
        var i = 0
        while (i < remaining) {
            sum += (buffer.get(i).toInt() and 0xFF)
            samples++
            i += step
        }

        return if (samples > 0) (sum.toFloat() / samples) / 255f * 100f else 0f
    }
}
