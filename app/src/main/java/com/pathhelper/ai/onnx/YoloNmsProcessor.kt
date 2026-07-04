package com.pathhelper.ai.onnx

import android.os.SystemClock
import kotlin.math.max
import kotlin.math.min
/**
* Coordinates Yolo Nms Processor operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Yolo Nms Processor.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class YoloNmsProcessor {
    private val confidenceThreshold = 0.25f
    private val iouThreshold = 0.45f

    fun process(
        detections: List<Detection>
    ): Pair<List<Detection>, NmsMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        try {
            if (detections.isEmpty()) {
                return Pair(
                    emptyList(),
                    NmsMetadata(
                        filteringSuccessful = true,
                        candidateCount = 0,
                        confidenceFilteredCount = 0,
                        finalDetectionCount = 0,
                        maxConfidence = 0.0f,
                        nmsTimeMs = SystemClock.elapsedRealtime() - startTime,
                        errorMessage = null
                    )
                )
            }

            // 1. Identify screen detections (TV=62, Laptop=63) with moderate confidence
            val screens = detections.filter {
                (it.classId == 62 || it.classId == 63) && it.confidence >= 0.30f
            }

            // 2. Filter out person detections contained inside those screens
            val filteredCandidates = detections.filter { det ->
                if (det.classId == 0) {
                    val isContained = screens.any { screen ->
                        calculateContainment(det, screen) >= 0.80f
                    }
                    !isContained
                } else {
                    true
                }
            }

            // 3. Confidence Filtering (discarding scores below class-specific threshold and NaNs)
            val confidenceFiltered = filteredCandidates.filter {
                val threshold = if (it.classId == 0) 0.65f else confidenceThreshold
                it.confidence >= threshold && !it.confidence.isNaN()
            }

            // 2. Sorting by confidence descending
            val sortedDetections = confidenceFiltered.sortedByDescending { it.confidence }

            val candidateCount = detections.size
            val confidenceFilteredCount = sortedDetections.size
            val maxConfidence = if (sortedDetections.isNotEmpty()) sortedDetections.first().confidence else 0.0f

            // 3. Class-aware Non-Maximum Suppression
            val grouped = sortedDetections.groupBy { it.classId }
            val finalDetections = ArrayList<Detection>()

            for ((_, classBoxes) in grouped) {
                val suppressed = BooleanArray(classBoxes.size)
                for (i in classBoxes.indices) {
                    if (suppressed[i]) continue
                    val boxI = classBoxes[i]
                    finalDetections.add(boxI)

                    for (j in i + 1 until classBoxes.size) {
                        if (suppressed[j]) continue
                        val boxJ = classBoxes[j]
                        val iou = calculateIoU(boxI, boxJ)
                        if (iou > iouThreshold) {
                            suppressed[j] = true
                        }
                    }
                }
            }

            val nmsTime = SystemClock.elapsedRealtime() - startTime

            //----------- debug
            finalDetections.forEach {
                if (it.classId == 0) {
                    android.util.Log.d(
                        "YOLO_NMS",
                        "PERSON survived conf=${it.confidence}"
                    )
                }
            }
            ///------------

            return Pair(
                finalDetections,
                NmsMetadata(
                    filteringSuccessful = true,
                    candidateCount = candidateCount,
                    confidenceFilteredCount = confidenceFilteredCount,
                    finalDetectionCount = finalDetections.size,
                    maxConfidence = maxConfidence,
                    nmsTimeMs = nmsTime,
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            val nmsTime = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                NmsMetadata(
                    filteringSuccessful = false,
                    candidateCount = detections.size,
                    confidenceFilteredCount = 0,
                    finalDetectionCount = 0,
                    maxConfidence = 0.0f,
                    nmsTimeMs = nmsTime,
                    errorMessage = e.localizedMessage ?: "Unknown NMS error."
                )
            )
        }
    }

    fun calculateIoU(boxA: Detection, boxB: Detection): Float {
        // Compute min/max edges from center coordinates
        val box1Left = boxA.centerX - boxA.width / 2f
        val box1Right = boxA.centerX + boxA.width / 2f
        val box1Top = boxA.centerY - boxA.height / 2f
        val box1Bottom = boxA.centerY + boxA.height / 2f

        val box2Left = boxB.centerX - boxB.width / 2f
        val box2Right = boxB.centerX + boxB.width / 2f
        val box2Top = boxB.centerY - boxB.height / 2f
        val box2Bottom = boxB.centerY + boxB.height / 2f

        // Compute intersection bounds
        val interLeft = max(box1Left, box2Left)
        val interRight = min(box1Right, box2Right)
        val interTop = max(box1Top, box2Top)
        val interBottom = min(box1Bottom, box2Bottom)

        // Compute overlap areas
        val interWidth = max(0f, interRight - interLeft)
        val interHeight = max(0f, interBottom - interTop)
        val interArea = interWidth * interHeight

        val areaA = boxA.width * boxA.height
        val areaB = boxB.width * boxB.height

        val unionArea = areaA + areaB - interArea
        if (unionArea <= 0f) return 0f

        return interArea / unionArea
    }

    fun calculateContainment(boxA: Detection, boxB: Detection): Float {
        val box1Left = boxA.centerX - boxA.width / 2f
        val box1Right = boxA.centerX + boxA.width / 2f
        val box1Top = boxA.centerY - boxA.height / 2f
        val box1Bottom = boxA.centerY + boxA.height / 2f

        val box2Left = boxB.centerX - boxB.width / 2f
        val box2Right = boxB.centerX + boxB.width / 2f
        val box2Top = boxB.centerY - boxB.height / 2f
        val box2Bottom = boxB.centerY + boxB.height / 2f

        val interLeft = max(box1Left, box2Left)
        val interRight = min(box1Right, box2Right)
        val interTop = max(box1Top, box2Top)
        val interBottom = min(box1Bottom, box2Bottom)

        val interWidth = max(0f, interRight - interLeft)
        val interHeight = max(0f, interBottom - interTop)
        val interArea = interWidth * interHeight

        val areaA = boxA.width * boxA.height
        if (areaA <= 0f) return 0f

        return interArea / areaA
    }
}
