package com.pathhelper.ai.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A Jetpack Compose wrapper for the Android CameraX PreviewView.
 *
 * This component integrates the camera preview into a Compose-based UI. It manages 
 * the lifecycle of a [CameraController], ensuring the camera starts when the 
 * component is added to the composition and is released when it is removed.
 *
 * @param modifier Modifier for the preview container.
 * @param analyzer The image analyzer used to process frames for AI perception.
 * @param onStatusChanged Callback invoked when the camera status changes.
 * @param onError Callback invoked when a camera-related error occurs.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraController: CameraController,
    analyzer: ImageAnalysis.Analyzer,
    onStatusChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(cameraController, previewViewRef) {
        val pv = previewViewRef
        if (pv != null) {
            cameraController.startCameraPreview(
                lifecycleOwner = lifecycleOwner,
                previewView = pv,
                analyzer = analyzer,
                onStatusChanged = onStatusChanged,
                onError = onError
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                previewViewRef = this
            }
        },
        modifier = modifier,
        update = {}
    )
}
