package com.pathhelper.ai.perception

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pathhelper.ai.camera.BitmapConverter

/**
 * Simplified analyzer for Segmentation Benchmarking.
 */
class SegmentationAnalyzer(
    context: Context,
    private val segmentationEngine: SegmentationEngine,
    private val onResult: (SegmentationResult?, Long) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val bitmapConverter = BitmapConverter()

    override fun analyze(image: ImageProxy) {
        val (bitmap, _) = bitmapConverter.imageProxyToBitmap(image)
        
        if (bitmap != null) {
            val (result, latency) = segmentationEngine.process(bitmap)
            onResult(result, latency)
            bitmap.recycle()
        }
        
        image.close()
    }
}
