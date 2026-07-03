package com.pathhelper.ai.perception

/**
 * Encapsulates the output of the Semantic Segmentation model.
 * Masks are provided as bitsets for memory efficiency.
 */
data
class SegmentationResult(
    val floorMask: java.util.BitSet,
    val wallMask: java.util.BitSet,
    val doorwayMask: java.util.BitSet,
    val maskWidth: Int,
    val maskHeight: Int,
    val floorDensityCenter: Float,
    val confidence: Float
)
