package com.pathhelper.ai.onnx

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.io.FileOutputStream
/**
* Coordinates Model Loader operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Model Loader.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class ModelLoader(private val context: Context) {
    private var session: OrtSession? = null

    fun loadModel(): ModelMetadata {
        try {
            val modelName = "yolov8n.onnx"
            val assetPath = "models/$modelName"
            val targetDir = File(context.filesDir, "models")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val modelFile = File(targetDir, modelName)

            // Copy model from assets to internal storage if not present
            if (!modelFile.exists()) {
                Log.d("ModelLoader", "Copying model from assets to internal storage: ${modelFile.absolutePath}")
                context.assets.open(assetPath).use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }
            }

            // Initialize OrtSession using the file path
            val env = OrtProvider.environment
            val sessionOptions = OrtSession.SessionOptions()
            val loadedSession = env.createSession(modelFile.absolutePath, sessionOptions)
            session = loadedSession
            OrtProvider.session = loadedSession

            val inputInfo = session?.inputInfo
            val outputInfo = session?.outputInfo

            val inputName = inputInfo?.keys?.firstOrNull() ?: "unknown"
            val inputMeta = inputInfo?.values?.firstOrNull()
            val inputTensorInfo = inputMeta?.info as? TensorInfo
            val inputShape = inputTensorInfo?.shape ?: longArrayOf()

            val outputName = outputInfo?.keys?.firstOrNull() ?: "unknown"
            val outputMeta = outputInfo?.values?.firstOrNull()
            val outputTensorInfo = outputMeta?.info as? TensorInfo
            val outputShape = outputTensorInfo?.shape ?: longArrayOf()

            Log.d("ModelLoader", "ONNX model loaded successfully from file path.")

            return ModelMetadata(
                isLoaded = true,
                inputName = inputName,
                inputShape = inputShape,
                outputName = outputName,
                outputShape = outputShape
            )
        } catch (e: Exception) {
            Log.e("ModelLoader", "Failed to load ONNX model", e)
            return ModelMetadata(
                isLoaded = false,
                inputName = "unknown",
                inputShape = longArrayOf(),
                outputName = "unknown",
                outputShape = longArrayOf(),
                errorMessage = e.localizedMessage ?: "Unknown loading error"
            )
        }
    }

    fun close() {
        try {
            session?.close()
            session = null
            OrtProvider.session = null
            Log.d("ModelLoader", "OrtSession closed successfully.")
        } catch (e: Exception) {
            Log.e("ModelLoader", "Error closing OrtSession", e)
        }
    }
}
