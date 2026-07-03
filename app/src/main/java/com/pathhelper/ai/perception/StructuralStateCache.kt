package com.pathhelper.ai.perception

import android.os.SystemClock
import java.util.BitSet

/**
 * Manages caching and temporal smoothing of semantic segmentation results.
 *
 * Explain:
 * * Purpose of the component: Temporarily stores segmentation frames to apply temporal smoothing (e.g. noise filter).
 * * Role within the Sarathi architecture: Enhances the stability and robustness of structural visual data before passing to navigation engines.
 * * Major inputs and outputs: Inputs a new [SegmentationResult]; outputs a smoothed or consolidated state estimate.
 */
/**
* Represents the data structures or state of Cached Result.
*/
/**
* Represents the data structures or state of Cached Result.
*/
class StructuralStateCache {
/**
* Represents the data structures or state of Cached Result.
*/
data class CachedResult(
        val result: SegmentationResult,
        val timestamp: Long
    )

    private var currentCache: CachedResult? = null
    private val CACHE_EXPIRATION_MS = 1000L
    
    @Synchronized
    fun update(newResult: SegmentationResult) {
        val last = currentCache?.result
        
        if (last == null) {
            currentCache = CachedResult(newResult, SystemClock.elapsedRealtime())
            return
        }

        // Temporal Smoothing: Bitwise AND between current and previous frame
        // This ensures a pixel is only "True" if it was true in both frames (stable)
        val smoothedFloor = last.floorMask.clone() as BitSet
        smoothedFloor.and(newResult.floorMask)
        
        val smoothedWall = last.wallMask.clone() as BitSet
        smoothedWall.and(newResult.wallMask)

        val smoothedResult = newResult.copy(
            floorMask = smoothedFloor,
            wallMask = smoothedWall
        )

        currentCache = CachedResult(smoothedResult, SystemClock.elapsedRealtime())
    }

    @Synchronized
    fun get(): SegmentationResult? {
        val cache = currentCache ?: return null
        if (SystemClock.elapsedRealtime() - cache.timestamp > CACHE_EXPIRATION_MS) {
            currentCache = null
            return null
        }
        return cache.result
    }

    fun clear() {
        currentCache = null
    }
}
