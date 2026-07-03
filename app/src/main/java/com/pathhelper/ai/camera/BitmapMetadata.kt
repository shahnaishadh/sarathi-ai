package com.pathhelper.ai.camera

/**
 * Represents descriptive information about a processed camera frame Bitmap.
 *
 * This
data class stores the resulting dimensions, orientation, and quality metrics 
 * of a Bitmap after it has been converted from a raw camera buffer. This metadata 
 * is used by downstream components to scale detection coordinates and adjust for 
 * environmental lighting conditions.
 */
data
class BitmapMetadata(
    val width: Int,
    val height: Int,
    val rotationApplied: Int,
    val conversionTimeMs: Long,
    val bitmapCreated: Boolean,
    val errorMessage: String? = null,
    val luminance: Float = 0.0f
)
