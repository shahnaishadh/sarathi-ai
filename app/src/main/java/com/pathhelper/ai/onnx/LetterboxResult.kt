package com.pathhelper.ai.onnx

import android.graphics.Bitmap
/**
* Represents the data structures or state of Letterbox Result.
*/
data
class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Int,
    val padY: Int
)
