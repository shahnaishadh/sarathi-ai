package com.pathhelper.ai.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
/**
* Singleton instance or companion helper for Ort Provider.
*/
object OrtProvider {
    val environment: OrtEnvironment by lazy {
        OrtEnvironment.getEnvironment()
    }

    var session: OrtSession? = null
}
