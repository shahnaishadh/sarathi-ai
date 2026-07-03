package com.pathhelper.ai.onnx

import android.os.SystemClock
/**
* Coordinates Yolo Detection Parser operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Yolo Detection Parser.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class YoloDetectionParser {

    fun parse(
        output: FloatArray,
        outputShape: LongArray
    ): Pair<List<Detection>, DetectionMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        android.util.Log.e(
            "YOLO_PARSE",
            "PARSER CALLED"
        )
        try {
            // Validate output array and shape dimensions
            if (output.isEmpty() || outputShape.size < 3) {
                return Pair(
                    emptyList(),
                    DetectionMetadata(
                        parsingSuccessful = false,
                        totalCandidates = 0,
                        maxConfidence = 0.0f,
                        averageConfidence = 0.0f,
                        parsingTimeMs = SystemClock.elapsedRealtime() - startTime,
                        tensorShape = outputShape,
                        errorMessage = "Invalid output tensor data or dimensions."
                    )
                )
            }

            val shape1 = outputShape[1]
            val shape2 = outputShape[2]

            val numClasses: Int
            val numAnchors: Int
            val isPlanar: Boolean

            if (shape1 == 84L) {
                numClasses = 84
                numAnchors = shape2.toInt()
                isPlanar = true
            } else if (shape2 == 84L) {
                numClasses = 84
                numAnchors = shape1.toInt()
                isPlanar = false
            } else {
                return Pair(
                    emptyList(),
                    DetectionMetadata(
                        parsingSuccessful = false,
                        totalCandidates = 0,
                        maxConfidence = 0.0f,
                        averageConfidence = 0.0f,
                        parsingTimeMs = SystemClock.elapsedRealtime() - startTime,
                        tensorShape = outputShape,
                        errorMessage = "Unexpected output tensor shape dimensions: ${outputShape.contentToString()}."
                    )
                )
            }

            // Expected size check
            val expectedSize = numClasses * numAnchors
            if (output.size < expectedSize) {
                return Pair(
                    emptyList(),
                    DetectionMetadata(
                        parsingSuccessful = false,
                        totalCandidates = 0,
                        maxConfidence = 0.0f,
                        averageConfidence = 0.0f,
                        parsingTimeMs = SystemClock.elapsedRealtime() - startTime,
                        tensorShape = outputShape,
                        errorMessage = "Flat output array size (${output.size}) is smaller than expected size (${expectedSize})."
                    )
                )
            }

            val detections = ArrayList<Detection>()
            var maxConfidence = 0.0f
            var sumConfidence = 0.0f
            val confidenceThreshold = 0.25f

            for (a in 0 until numAnchors) {
                // Find highest scoring
/**
* Coordinates class Id operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for class Id.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class classId
                var maxScore = -1.0f
                var bestClassId = -1

                for (c in 4 until numClasses) {
                    val scoreIdx = if (isPlanar) (c * numAnchors + a) else (a * numClasses + c)
                    val score = output[scoreIdx]
                    if (score > maxScore) {
                        maxScore = score
                        bestClassId = c - 4
                    }
                }

                val confidence = maxScore
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                }
                sumConfidence += confidence

                if (confidence >= confidenceThreshold) {
                    // Extract bounding coordinates only if confidence is high enough
                    val idx0 = if (isPlanar) (0 * numAnchors + a) else (a * numClasses + 0)
                    val idx1 = if (isPlanar) (1 * numAnchors + a) else (a * numClasses + 1)
                    val idx2 = if (isPlanar) (2 * numAnchors + a) else (a * numClasses + 2)
                    val idx3 = if (isPlanar) (3 * numAnchors + a) else (a * numClasses + 3)

                    val cx = output[idx0]
                    val cy = output[idx1]
                    val w = output[idx2]
                    val h = output[idx3]

                    detections.add(
                        Detection(
                            classId = bestClassId,
                            confidence = confidence,
                            centerX = cx,
                            centerY = cy,
                            width = w,
                            height = h
                        )
                    )
                }
            }

            val avgConfidence = if (numAnchors > 0) (sumConfidence / numAnchors) else 0.0f
            val parsingTime = SystemClock.elapsedRealtime() - startTime

            return Pair(
                detections,
                DetectionMetadata(
                    parsingSuccessful = true,
                    totalCandidates = numAnchors,
                    maxConfidence = maxConfidence,
                    averageConfidence = avgConfidence,
                    parsingTimeMs = parsingTime,
                    tensorShape = outputShape,
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            val parsingTime = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                DetectionMetadata(
                    parsingSuccessful = false,
                    totalCandidates = 0,
                    maxConfidence = 0.0f,
                    averageConfidence = 0.0f,
                    parsingTimeMs = parsingTime,
                    tensorShape = outputShape,
                    errorMessage = e.localizedMessage ?: "Unknown parsing error."
                )
            )
        }
    }
}
