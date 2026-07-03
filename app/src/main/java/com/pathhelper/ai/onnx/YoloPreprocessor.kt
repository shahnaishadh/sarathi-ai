package com.pathhelper.ai.onnx

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import ai.onnxruntime.OnnxTensor
import java.nio.ByteBuffer
import java.nio.ByteOrder
/**
* Coordinates Yolo Preprocessor operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Yolo Preprocessor.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class YoloPreprocessor {
    var lastLetterboxResult: LetterboxResult? = null
        private set

    private val shape = longArrayOf(1, 3, 640, 640)
    private val numPixels = 640 * 640
    private val pixels = IntArray(numPixels)
    private val floatArray = FloatArray(numPixels * 3)
    
    private val byteBuffer = ByteBuffer.allocateDirect(1 * 3 * 640 * 640 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val floatBuffer = byteBuffer.asFloatBuffer()

    private var reusableTargetBitmap: Bitmap? = null
    private var reusableCanvas: Canvas? = null

    fun preprocess(
        bitmap: Bitmap
    ): Pair<OnnxTensor?, TensorMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        try {
            // 1. Perform Letterbox Resizing preserving aspect ratio
            val letterboxRes = letterbox(bitmap)
            lastLetterboxResult = letterboxRes
            val letterboxedBitmap = letterboxRes.bitmap

            // 2. Extract RGB float components and convert HWC -> CHW layout
            letterboxedBitmap.getPixels(pixels, 0, 640, 0, 0, 640, 640)

            val rOffset = 0
            val gOffset = numPixels
            val bOffset = numPixels * 2

            for (i in 0 until numPixels) {
                val color = pixels[i]
                val r = ((color shr 16) and 0xFF) / 255.0f
                val g = ((color shr 8) and 0xFF) / 255.0f
                val b = (color and 0xFF) / 255.0f

                floatArray[rOffset + i] = r
                floatArray[gOffset + i] = g
                floatArray[bOffset + i] = b
            }

            floatBuffer.rewind()
            floatBuffer.put(floatArray)
            floatBuffer.rewind()

            // 3. Create OnnxTensor
            val env = OrtProvider.environment
            val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)

            val preprocessingTime = SystemClock.elapsedRealtime() - startTime

            return Pair(
                tensor,
                TensorMetadata(
                    tensorCreated = true,
                    shape = shape,
                    preprocessingTimeMs = preprocessingTime,
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            val preprocessingTime = SystemClock.elapsedRealtime() - startTime
            return Pair(
                null,
                TensorMetadata(
                    tensorCreated = false,
                    shape = shape,
                    preprocessingTimeMs = preprocessingTime,
                    errorMessage = e.localizedMessage ?: "Unknown preprocessing error"
                )
            )
        }
    }

    private fun letterbox(bitmap: Bitmap, targetWidth: Int = 640, targetHeight: Int = 640): LetterboxResult {
        val width = bitmap.width
        val height = bitmap.height

        val scale = minOf(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        if (reusableTargetBitmap == null) {
            reusableTargetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            reusableCanvas = Canvas(reusableTargetBitmap!!)
        }

        val canvas = reusableCanvas!!
        canvas.drawColor(Color.rgb(114, 114, 114))

        val padX = (targetWidth - newWidth) / 2
        val padY = (targetHeight - newHeight) / 2

        val srcRect = Rect(0, 0, width, height)
        val dstRect = Rect(padX, padY, padX + newWidth, padY + newHeight)
        
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)

        return LetterboxResult(reusableTargetBitmap!!, scale, padX, padY)
    }
}
