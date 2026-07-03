package com.pathhelper.ai.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.pathhelper.ai.onnx.DetectionRenderData
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.navigation.TtcRiskLevel
import com.pathhelper.ai.navigation.ThreatLevel

private val classNames = listOf(
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard",
    "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
    "scissors", "teddy bear", "hair drier", "toothbrush"
)

@Composable
fun DetectionOverlay(
    detections: List<DetectionRenderData>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokePaint = Stroke(width = 4f)

        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 32f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (det in detections) {
            val width = det.right - det.left
            val height = det.bottom - det.top

            // Determine border color and text backdrop color based on threat level priority
            val boxColor = when (det.threatLevel) {
                ThreatLevel.CRITICAL -> Color(0xFFFF1744) // Bright Neon Red
                ThreatLevel.HIGH -> Color(0xFFFF9100)     // Neon Orange
                ThreatLevel.MEDIUM -> Color(0xFFFFEA00)   // Yellow
                ThreatLevel.LOW -> Color(0xFF00E676)      // Neon Green
            }

            // Backdrop has slight semi-transparency matching target border colors
            val textBackgroundPaint = when (det.threatLevel) {
                ThreatLevel.CRITICAL -> Color(0xBBFF1744)
                ThreatLevel.HIGH -> Color(0xBBFF9100)
                ThreatLevel.MEDIUM -> Color(0xBBFFEA00)
                ThreatLevel.LOW -> Color(0xBB00E676)
            }

            // Draw bounding box
            drawRect(
                color = boxColor,
                topLeft = Offset(det.left, det.top),
                size = Size(width, height),
                style = strokePaint
            )

            // Prepare multiline label list
            val labelNameRaw = classNames.getOrNull(det.classId) ?: "unknown"
            val labelName = labelNameRaw.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString()
            }
            
            val lines = mutableListOf<String>()
            
            // Add threat priority rank label (e.g. #1, #2, #3)
            if (det.threatRank != Int.MAX_VALUE) {
                lines.add("#${det.threatRank}")
            }
            
            lines.add(String.format(java.util.Locale.US, "%s #%d", labelName, det.trackId))
            det.horizontalZone?.name?.let { lines.add(it) }
            det.distanceMeters?.let { lines.add(String.format(java.util.Locale.US, "%.1fm", it)) }
            
            val ttcText = if (det.ttcSeconds != null) {
                String.format(java.util.Locale.US, "TTC: %.1fs", det.ttcSeconds)
            } else {
                "TTC: ∞"
            }
            lines.add(ttcText)
            
            // Add threat level name label
            lines.add(det.threatLevel.name)

            // Draw stacked text lines above the box top edge
            var currentY = det.top - 8f
            for (line in lines.reversed()) {
                val bounds = Rect()
                textPaint.getTextBounds(line, 0, line.length, bounds)
                val textWidth = bounds.width().toFloat()
                val textHeight = bounds.height().toFloat()

                // Draw background for this line
                drawRect(
                    color = textBackgroundPaint,
                    topLeft = Offset(det.left, currentY - textHeight - 8f),
                    size = Size(textWidth + 16f, textHeight + 12f)
                )

                // Draw text
                drawContext.canvas.nativeCanvas.drawText(
                    line,
                    det.left + 8f,
                    currentY - 4f,
                    textPaint
                )
                
                // Shift coordinate vertically for the next line above
                currentY -= (textHeight + 16f)
            }
        }
    }
}
