package com.pathhelper.ai.perception

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.BitSet
import java.util.Collections

/**
 * Runs on-device semantic segmentation to identify structural components of the environment.
 *
 * Explain:
 * * Purpose of the component: Executes semantic segmentation on raw frames using DeepLabV3+ with MobileNetV2.
 * * Role within the Sarthi architecture: Provides pixel-level classification of obstacles, flooring, and structures for path assessment.
 * * Major inputs and outputs: Inputs a camera frame [Bitmap]; outputs a [SegmentationResult] mapping pixel classifications.
 */
class SegmentationEngine(private val context: Context) : AutoCloseable {

    companion
object {
        private const val TAG = "PathHelper.SegEngine"
        private const val MODEL_PATH = "deeplabv3_mobilenet_v2.onnx"
        private const val INPUT_SIZE = 256 // Recommended for Galaxy A36 balance
        private const val NUM_CLASSES = 21 // Pascal VOC classes used by standard DeepLabV3+
        
        // Semantic Mapping (Pascal VOC indices)
        // 0: Background, 15: Person, etc. 
        // We will need a model trained on ADE20K or COCO-Stuff for better floor/wall
        // For this baseline, we assume a model that outputs:
        // Index 0: Background/Floor (often)
        // Index 1: Wall (in custom indoor models)
        private const val CLASS_FLOOR = 0 
        private const val CLASS_WALL = 1
        private const val CLASS_DOOR = 2
    }

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null

    // Reusable buffers to avoid allocations per frame
    private val inputBuffer: FloatBuffer = FloatBuffer.allocate(1 * 3 * INPUT_SIZE * INPUT_SIZE)
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
    
    // Recycled Result masks
    private val floorMask = BitSet(INPUT_SIZE * INPUT_SIZE)
    private val wallMask = BitSet(INPUT_SIZE * INPUT_SIZE)
    private val doorwayMask = BitSet(INPUT_SIZE * INPUT_SIZE)

    private var isInitialized = false

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val modelBytes = context.assets.open(MODEL_PATH).readBytes()
            val options = OrtSession.SessionOptions()
            
            // Try NNAPI for Galaxy A36 NPU acceleration
            try {
                options.addNnapi()
                Log.i(TAG, "NNAPI acceleration enabled")
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI not supported, falling back to CPU: ${e.message}")
            }
            
            ortSession = ortEnv.createSession(modelBytes, options)
            isInitialized = true
            Log.i(TAG, "Segmentation Engine initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Segmentation Engine: ${e.message}")
        }
    }

    /**
     * Runs inference on the provided bitmap.
     * Scale the bitmap to INPUT_SIZE before calling.
     */
    fun process(bitmap: Bitmap): Pair<SegmentationResult?, Long> {
        if (!isInitialized || ortSession == null) return Pair(null, 0L)

        val startTime = SystemClock.elapsedRealtime()
        
        try {
            // 1. Preprocess
            preprocess(bitmap)

            // 2. Wrap into Tensor
            val inputName = ortSession?.inputNames?.iterator()?.next() ?: "input"
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong()))

            // 3. Run Inference
            inputTensor.use {
                val results = ortSession?.run(Collections.singletonMap(inputName, inputTensor))
                
                results?.use {
                    val outputTensor = results[0] as OnnxTensor
                    val outputData = outputTensor.floatBuffer
                    
                    // 4. Decode Output (Argmax or Probability threshold)
                    decode(outputData)
                }
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            
            val result = SegmentationResult(
                floorMask = floorMask.clone() as BitSet,
                wallMask = wallMask.clone() as BitSet,
                doorwayMask = doorwayMask.clone() as BitSet,
                maskWidth = INPUT_SIZE,
                maskHeight = INPUT_SIZE,
                floorDensityCenter = calculateCenterDensity(),
                confidence = 0.85f // Placeholder until prob check added
            )

            return Pair(result, duration)

        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            return Pair(null, 0L)
        }
    }

    private fun preprocess(bitmap: Bitmap) {
        val scaledBitmap = if (bitmap.width != INPUT_SIZE || bitmap.height != INPUT_SIZE) {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        } else {
            bitmap
        }

        scaledBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        inputBuffer.rewind()

        // NCHW Format: RRR... GGG... BBB...
        // Normalized to [0, 1] or [-1, 1] depending on model requirements
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            val pixel = intValues[i]
            inputBuffer.put(i, ((pixel shr 16 and 0xFF) / 255.0f)) // R
            inputBuffer.put(i + INPUT_SIZE * INPUT_SIZE, ((pixel shr 8 and 0xFF) / 255.0f)) // G
            inputBuffer.put(i + 2 * INPUT_SIZE * INPUT_SIZE, ((pixel and 0xFF) / 255.0f)) // B
        }
    }

    private fun decode(buffer: FloatBuffer) {
        buffer.rewind()
        floorMask.clear()
        wallMask.clear()
        doorwayMask.clear()

        // DeepLab output is typically [1, NUM_CLASSES, H, W]
        // For each pixel (y, x), find class with max probability
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                var maxProb = -1.0f
                var maxClass = -1
                
                for (c in 0 until NUM_CLASSES) {
                    val idx = c * (INPUT_SIZE * INPUT_SIZE) + (y * INPUT_SIZE + x)
                    val prob = buffer.get(idx)
                    if (prob > maxProb) {
                        maxProb = prob
                        maxClass = c
                    }
                }

                val bitIdx = y * INPUT_SIZE + x
                when (maxClass) {
                    CLASS_FLOOR -> floorMask.set(bitIdx)
                    CLASS_WALL -> wallMask.set(bitIdx)
                    CLASS_DOOR -> doorwayMask.set(bitIdx)
                }
            }
        }
    }

    private fun calculateCenterDensity(): Float {
        // Calculate floor density in the center vertical strip (33% to 66% width)
        val startX = INPUT_SIZE / 3
        val endX = (INPUT_SIZE * 2) / 3
        var floorCount = 0
        val total = (endX - startX) * INPUT_SIZE

        for (y in 0 until INPUT_SIZE) {
            for (x in startX until endX) {
                if (floorMask.get(y * INPUT_SIZE + x)) floorCount++
            }
        }
        return floorCount.toFloat() / total
    }

    override fun close() {
        ortSession?.close()
        ortEnv.close()
    }
}
