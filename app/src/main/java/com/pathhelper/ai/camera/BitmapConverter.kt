package com.pathhelper.ai.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Handles the conversion of Android CameraX ImageProxy frames into Bitmaps.
 *
 * This component is responsible for transforming raw YUV_420_888 camera frames into 
 * standard Bitmap format, which is required by the AI perception engines and for 
 * visual debugging. It also performs necessary image rotations and calculates 
 * scene luminance to assist with lighting-aware processing.
 */
class BitmapConverter {
    private var nv21Buffer: ByteArray? = null
    private var luminanceBuffer: ByteArray? = null

    /**
     * Converts a CameraX ImageProxy to a Bitmap.
     *
     * This method handles the complexity of YUV-to-RGB conversion, applies correct 
     * image orientation based on camera metadata, and calculates environment luminance.
     * It uses internal buffering to minimize
object allocation and reduce garbage 
     * collection overhead during high-frequency frame processing.
     *
     * @param image The raw camera frame from CameraX.
     * @return A pair containing the processed Bitmap (if successful) and associated metadata.
     */
    fun imageProxyToBitmap(
        image: ImageProxy
    ): Pair<Bitmap?, BitmapMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val rotationDegrees = image.imageInfo.rotationDegrees

        try {
            if (image.format != ImageFormat.YUV_420_888) {
                return Pair(
                    null,
                    BitmapMetadata(
                        width = 0,
                        height = 0,
                        rotationApplied = rotationDegrees,
                        conversionTimeMs = SystemClock.elapsedRealtime() - startTime,
                        bitmapCreated = false,
                        errorMessage = "Invalid image format: ${image.format}. Expected YUV_420_888."
                    )
                )
            }

            val nv21Bytes = yuv420ToNv21(image)
            val yuvImage = YuvImage(
                nv21Bytes,
                ImageFormat.NV21,
                image.width,
                image.height,
                null
            )

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val imageBytes = out.toByteArray()
            
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                return Pair(
                    null,
                    BitmapMetadata(
                        width = 0,
                        height = 0,
                        rotationApplied = rotationDegrees,
                        conversionTimeMs = SystemClock.elapsedRealtime() - startTime,
                        bitmapCreated = false,
                        errorMessage = "Failed to decode JPEG bytes into Bitmap."
                    )
                )
            }

            // Apply rotation matching camera parameters to orient it upright
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle() // Free old memory pointer instantly
                }
                bitmap = rotatedBitmap
            }

            val conversionTime = SystemClock.elapsedRealtime() - startTime
            val luminance = calculateLuminance(image)

            return Pair(
                bitmap,
                BitmapMetadata(
                    width = bitmap.width,
                    height = bitmap.height,
                    rotationApplied = rotationDegrees,
                    conversionTimeMs = conversionTime,
                    bitmapCreated = true,
                    errorMessage = null,
                    luminance = luminance
                )
            )
        } catch (e: Exception) {
            return Pair(
                null,
                BitmapMetadata(
                    width = 0,
                    height = 0,
                    rotationApplied = rotationDegrees,
                    conversionTimeMs = SystemClock.elapsedRealtime() - startTime,
                    bitmapCreated = false,
                    errorMessage = e.localizedMessage ?: "Unknown conversion error",
                    luminance = 0f
                )
            )
        }
    }

    private fun calculateLuminance(image: ImageProxy): Float {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val remaining = buffer.remaining()
        if (luminanceBuffer == null || luminanceBuffer!!.size < remaining) {
            luminanceBuffer = ByteArray(remaining)
        }
        val data = luminanceBuffer!!
        buffer.get(data, 0, remaining)
        var sum = 0L
        for (i in 0 until remaining) {
            sum += data[i].toInt() and 0xFF
        }
        // Normalize to 0..100 for consistency across the perception module
        return if (remaining > 0) (sum.toFloat() / remaining) / 255f * 100f else 0f
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val totalSize = ySize + ySize / 2
        if (nv21Buffer == null || nv21Buffer!!.size < totalSize) {
            nv21Buffer = ByteArray(totalSize)
        }
        val nv21 = nv21Buffer!!

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)

        // Interleave U and V components into V-U sequence
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride

        val width = image.width
        val height = image.height

        var offset = ySize
        if (uPixelStride == 2 && vPixelStride == 2 && uRowStride == vRowStride) {
            // Semi-planar optimization copy
            vBuffer.get(nv21, offset, vSize)
        } else {
            // Manual stride-aware component interleave
            for (row in 0 until height / 2) {
                for (col in 0 until width / 2) {
                    val uIndex = row * uRowStride + col * uPixelStride
                    val vIndex = row * vRowStride + col * vPixelStride

                    nv21[offset++] = vBuffer.get(vIndex)
                    nv21[offset++] = uBuffer.get(uIndex)
                }
            }
        }

        return nv21
    }
}
