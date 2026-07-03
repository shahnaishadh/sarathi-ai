package com.pathhelper.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pathhelper.ai.perception.SegmentationResult

/**
 * Renders semantic segmentation masks on top of the camera preview.
 *
 * This component provides a visual debug overlay that highlights different structural 
 * elements of the environment, such as floors, walls, and doorways, based on the 
 * segmentation model's output.
 *
 * @param result The semantic segmentation result containing mask data.
 * @param showFloor Whether to highlight floor areas in green.
 * @param showWall Whether to highlight wall areas in red.
 * @param showDoor Whether to highlight doorway areas in yellow.
 */
@Composable
fun StructuralDebugOverlay(
    result: SegmentationResult?,
    showFloor: Boolean = true,
    showWall: Boolean = true,
    showDoor: Boolean = true
) {
    if (result == null) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        val cellWidth = canvasWidth / result.maskWidth
        val cellHeight = canvasHeight / result.maskHeight

        for (y in 0 until result.maskHeight) {
            for (x in 0 until result.maskWidth) {
                val idx = y * result.maskWidth + x
                
                var color: Color? = null
                
                if (showFloor && result.floorMask.get(idx)) {
                    color = Color.Green.copy(alpha = 0.3f)
                } else if (showWall && result.wallMask.get(idx)) {
                    color = Color.Red.copy(alpha = 0.3f)
                } else if (showDoor && result.doorwayMask.get(idx)) {
                    color = Color.Yellow.copy(alpha = 0.5f)
                }

                color?.let {
                    drawRect(
                        color = it,
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth + 1, cellHeight + 1) // +1 to avoid grid gaps
                    )
                }
            }
        }
    }
}
