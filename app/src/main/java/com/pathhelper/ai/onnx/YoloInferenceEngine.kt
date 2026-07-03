package com.pathhelper.ai.onnx

import android.os.SystemClock
import android.util.Log
import ai.onnxruntime.OnnxTensor
import java.util.Collections
/**
* Coordinates Yolo Inference Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Yolo Inference Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class YoloInferenceEngine {
    private var reusableFloatArray: FloatArray? = null

    fun runInference(
        tensor: OnnxTensor
    ): Pair<FloatArray?, InferenceMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val expectedShape = longArrayOf(1, 84, 8400)

        val session = OrtProvider.session
        if (session == null) {
            return Pair(
                null,
                InferenceMetadata(
                    inferenceSuccessful = false,
                    outputName = "unknown",
                    outputShape = longArrayOf(),
                    inferenceTimeMs = SystemClock.elapsedRealtime() - startTime,
                    errorMessage = "OrtSession is not initialized."
                )
            )
        }

        try {
            val inputName = session.inputNames.firstOrNull() ?: "images"
            val inputs = Collections.singletonMap(inputName, tensor)

            // Run model inference using OrtSession
            session.run(inputs).use { results ->
                if (results.size() == 0) {
                    return Pair(
                        null,
                        InferenceMetadata(
                            inferenceSuccessful = false,
                            outputName = "unknown",
                            outputShape = longArrayOf(),
                            inferenceTimeMs = SystemClock.elapsedRealtime() - startTime,
                            errorMessage = "OrtSession execution returned no results."
                        )
                    )
                }

                val iterator = results.iterator()
                val firstResult = iterator.next()
                val outputName = firstResult.key
                val outputTensor = firstResult.value as? OnnxTensor

                if (outputTensor == null) {
                    return Pair(
                        null,
                        InferenceMetadata(
                            inferenceSuccessful = false,
                            outputName = outputName,
                            outputShape = longArrayOf(),
                            inferenceTimeMs = SystemClock.elapsedRealtime() - startTime,
                            errorMessage = "Output value is not an OnnxTensor."
                        )
                    )
                }

                val outputShape = outputTensor.info.shape
                if (!outputShape.contentEquals(expectedShape)) {
                    return Pair(
                        null,
                        InferenceMetadata(
                            inferenceSuccessful = false,
                            outputName = outputName,
                            outputShape = outputShape,
                            inferenceTimeMs = SystemClock.elapsedRealtime() - startTime,
                            errorMessage = "Shape mismatch: expected [1, 84, 8400], got ${outputShape.contentToString()}."
                        )
                    )
                }

                // Safely copy floats out of direct NIO FloatBuffer to prevent array UnsupportedOperationExceptions
                val floatBuffer = outputTensor.floatBuffer
                val size = floatBuffer.remaining()
                
                if (reusableFloatArray == null || reusableFloatArray!!.size != size) {
                    reusableFloatArray = FloatArray(size)
                }
                val floatArray = reusableFloatArray!!
                floatBuffer.get(floatArray)

                val inferenceTime = SystemClock.elapsedRealtime() - startTime

                return Pair(
                    floatArray,
                    InferenceMetadata(
                        inferenceSuccessful = true,
                        outputName = outputName,
                        outputShape = outputShape,
                        inferenceTimeMs = inferenceTime,
                        errorMessage = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("YoloInferenceEngine", "ONNX inference failed", e)
            val inferenceTime = SystemClock.elapsedRealtime() - startTime
            return Pair(
                null,
                InferenceMetadata(
                    inferenceSuccessful = false,
                    outputName = "unknown",
                    outputShape = longArrayOf(),
                    inferenceTimeMs = inferenceTime,
                    errorMessage = e.localizedMessage ?: "Unknown inference error"
                )
            )
        }
    }
}
