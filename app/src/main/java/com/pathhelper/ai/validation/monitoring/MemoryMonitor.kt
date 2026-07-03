package com.pathhelper.ai.validation.monitoring

import android.os.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors JVM + native heap memory usage.
 *
 * Updates every 5 seconds on an IO dispatcher.
 * Tracks current, peak, and maximum (system limit) memory.
 * Avoids per-frame allocations – only updated periodically.
 */
object MemoryMonitor {

    private const val UPDATE_INTERVAL_MS = 5_000L
    private const val MB = 1024L * 1024L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runtime = Runtime.getRuntime()

    private val _usedMemoryMb = MutableStateFlow(0f)
    val usedMemoryMb: StateFlow<Float> = _usedMemoryMb.asStateFlow()

    private val _peakMemoryMb = MutableStateFlow(0f)
    val peakMemoryMb: StateFlow<Float> = _peakMemoryMb.asStateFlow()

    private val _maxMemoryMb = MutableStateFlow(0f)
    val maxMemoryMb: StateFlow<Float> = _maxMemoryMb.asStateFlow()

    private var peakSoFar = 0f

    fun start() {
        scope.launch {
            while (true) {
                snapshot()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun snapshot() {
        // JVM heap used
        val jvmUsed = (runtime.totalMemory() - runtime.freeMemory()).toFloat() / MB
        // Native heap allocated (extra precision on devices that use native libs heavily)
        val nativeUsed = Debug.getNativeHeapAllocatedSize().toFloat() / MB
        val totalUsed = jvmUsed + nativeUsed

        _usedMemoryMb.value = totalUsed
        _maxMemoryMb.value = runtime.maxMemory().toFloat() / MB

        if (totalUsed > peakSoFar) {
            peakSoFar = totalUsed
            _peakMemoryMb.value = peakSoFar
        }
    }
}
