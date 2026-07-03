package com.pathhelper.ai.validation.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors battery percentage and temperature.
 *
 * - Polls the sticky ACTION_BATTERY_CHANGED intent every 30 seconds.
 * - No persistent BroadcastReceiver registered (avoids lifecycle issues).
 * - Safe to call start() multiple times; only the first call has effect.
 */
object SystemHealthMonitor {

    private const val UPDATE_INTERVAL_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    private val _batteryPercent = MutableStateFlow(0f)
    val batteryPercent: StateFlow<Float> = _batteryPercent.asStateFlow()

    private val _batteryTemperatureC = MutableStateFlow(0f)
    val batteryTemperatureC: StateFlow<Float> = _batteryTemperatureC.asStateFlow()

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        scope.launch {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            while (true) {
                // registerReceiver with null receiver returns the last sticky broadcast immediately
                val intent = appContext.registerReceiver(null, filter)
                intent?.let { updateFromIntent(it) }
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) {
            _batteryPercent.value = level * 100f / scale
        }
        // Temperature reported in tenths of a degree Celsius
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (tempTenths >= 0) {
            _batteryTemperatureC.value = tempTenths / 10f
        }
    }
}
