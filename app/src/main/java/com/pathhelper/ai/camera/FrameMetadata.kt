package com.pathhelper.ai.camera

/**
 * Represents raw technical characteristics of an input camera frame.
 *
 * This
data class captures the low-level buffer parameters of a CameraX frame, 
 * including planar strides and pixel formats. This information is essential for 
 * components like the BitmapConverter to correctly interpret and manipulate 
 * raw YUV memory layouts.
 */
data
class FrameMetadata(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val imageFormat: Int,
    val yRowStride: Int,
    val uRowStride: Int,
    val vRowStride: Int,
    val uPixelStride: Int,
    val vPixelStride: Int
)
