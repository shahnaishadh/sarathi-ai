package com.pathhelper.ai.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathhelper.ai.camera.CameraPreview
import com.pathhelper.ai.perception.SegmentationAnalyzer
import com.pathhelper.ai.perception.SegmentationEngine
import com.pathhelper.ai.perception.SegmentationResult
import com.pathhelper.ai.ui.theme.PathHelperTheme
import java.util.Locale
/**
* Coordinates Segmentation Benchmark Activity operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Segmentation Benchmark Activity.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class SegmentationBenchmarkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PathHelperTheme {
                SegmentationBenchmarkScreen()
            }
        }
    }
}

@Composable
fun SegmentationBenchmarkScreen() {
    val context = LocalContext.current
    val segmentationEngine = remember { SegmentationEngine(context) }
    
    var lastResult by remember { mutableStateOf<SegmentationResult?>(null) }
    var avgFps by remember { mutableStateOf(0.0) }
    var avgLatency by remember { mutableStateOf(0L) }
    var frameCount by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    
    val analyzer = remember {
        SegmentationAnalyzer(context, segmentationEngine) { result, latency ->
            lastResult = result
            frameCount++
            
            val now = SystemClock.elapsedRealtime()
            val elapsed = (now - startTime) / 1000.0
            if (elapsed > 1.0) {
                avgFps = frameCount / elapsed
            }
            avgLatency = (avgLatency * 0.9 + latency * 0.1).toLong()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = analyzer,
                onStatusChanged = {},
                onError = {}
            )
            
            StructuralDebugOverlay(result = lastResult)
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Segmentation Benchmark", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                BenchmarkRow("FPS", String.format(Locale.US, "%.1f", avgFps))
                BenchmarkRow("Latency", "${avgLatency}ms")
                BenchmarkRow("Confidence", String.format(Locale.US, "%.1f%%", (lastResult?.confidence ?: 0f) * 100))
                BenchmarkRow("Floor Density", String.format(Locale.US, "%.1f%%", (lastResult?.floorDensityCenter ?: 0f) * 100))
                
                val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                BenchmarkRow("Heap Used", "${memory / 1024 / 1024} MB")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Requirements: > 8 FPS, > 85% Accuracy", color = Color.Yellow, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BenchmarkRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.Green, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
