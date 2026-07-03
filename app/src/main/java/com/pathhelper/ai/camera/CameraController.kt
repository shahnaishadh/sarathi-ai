package com.pathhelper.ai.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages the lifecycle and configuration of the Android camera.
 *
 * This component is responsible for initializing the CameraX provider, binding 
 * preview and analysis use cases to the application's lifecycle, and providing 
 * control over camera hardware features like the torch (flash). It ensures that 
 * frame analysis is offloaded to a dedicated background executor to maintain UI 
 * responsiveness.
 */
class CameraController(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService? = null
    private var camera: androidx.camera.core.Camera? = null

    /**
     * Initializes and starts the camera preview and frame analysis.
     *
     * Binds the camera to the provided [lifecycleOwner] and connects the preview stream 
     * to the [previewView]. Frame analysis is routed through the provided [analyzer] 
     * running on a dedicated single-thread executor.
     *
     * @param lifecycleOwner The lifecycle owner (Activity/Fragment) to bind the camera to.
     * @param previewView The view where the camera preview will be rendered.
     * @param analyzer The logic for processing individual camera frames.
     * @param onStatusChanged Callback for reporting changes in camera connectivity state.
     * @param onError Callback for reporting fatal initialization errors.
     */
    fun startCameraPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        onStatusChanged: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Initialize single thread executor for offloading frame analysis
                analysisExecutor?.shutdown()
                val executor = Executors.newSingleThreadExecutor()
                analysisExecutor = executor

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, analyzer)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                onStatusChanged("Active")
                Log.d("CameraController", "Camera preview and analysis started successfully.")
            } catch (e: Exception) {
                Log.e("CameraController", "Use case binding failed", e)
                onError("Camera initialization failed: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Toggles the camera torch (flashlight) state.
     *
     * @param enabled True to turn the torch on, false to turn it off.
     */
    fun setFlash(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * Releases camera resources and stops all active use cases.
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        analysisExecutor?.shutdown()
        analysisExecutor = null
        camera = null
    }
}
