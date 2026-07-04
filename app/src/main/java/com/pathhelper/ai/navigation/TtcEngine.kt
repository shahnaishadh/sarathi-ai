package com.pathhelper.ai.navigation

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.tracking.Track
/**
* Coordinates Ttc Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Ttc Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class TtcEngine {

    /**
     * Estimates Time-to-Collision (TTC) for all active
object tracks.
     * Calculations are based on track distances and closing velocities, applying safety threat weights
     * to tag critical threat levels in proximity.
     */
    fun process(
        tracks: List<Track>
    ): Pair<List<TtcEstimate>, TtcMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        if (tracks.isEmpty()) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                TtcMetadata(
                    processedTracks = 0,
                    validTtcCount = 0,
                    criticalRiskCount = 0,
                    averageTtcSeconds = 0.0f,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        }

        val estimates = mutableListOf<TtcEstimate>()
        var validTtcCount = 0
        var criticalRiskCount = 0
        var sumTtcSeconds = 0.0f

        try {
            for (track in tracks) {
                val distance = track.distanceMeters
                val velocity = track.distanceVelocityMetersPerSecond
                val closingVelocity = -velocity

                var ttcSeconds: Float? = null
                var riskLevel = TtcRiskLevel.SAFE

                // TTC calculation criteria: closingVelocity > 0 and distance > 0, not NaN or Infinity
                val isValidForTtc = distance > 0f && 
                                    closingVelocity > 0f && 
                                    !distance.isNaN() && 
                                    !distance.isInfinite() && 
                                    !closingVelocity.isNaN() && 
                                    !closingVelocity.isInfinite()

                if (isValidForTtc) {
                    val computedTtc = distance / closingVelocity
                    if (!computedTtc.isNaN() && !computedTtc.isInfinite()) {
                        ttcSeconds = computedTtc
                        validTtcCount++
                        sumTtcSeconds += computedTtc

                        riskLevel = when {
                            computedTtc < 2f -> {
                                criticalRiskCount++
                                TtcRiskLevel.CRITICAL
                            }
                            computedTtc < 5f -> TtcRiskLevel.WARNING
                            computedTtc <= 8f -> TtcRiskLevel.CAUTION
                            else -> TtcRiskLevel.SAFE
                        }
                    }
                }

                // Static distance-based risk override for safety
                val distanceRisk = when {
                    distance < 1.0f -> TtcRiskLevel.CRITICAL
                    distance < 2.0f -> TtcRiskLevel.WARNING
                    distance < 3.5f -> TtcRiskLevel.CAUTION
                    else -> TtcRiskLevel.SAFE
                }
                if (distanceRisk.ordinal > riskLevel.ordinal) {
                    riskLevel = distanceRisk
                    if (riskLevel == TtcRiskLevel.CRITICAL) {
                        criticalRiskCount++
                    }
                }

                // Update track state properties
                track.ttcSeconds = ttcSeconds
                track.riskLevel = riskLevel

                // Debug logging (debug builds only)
                if (BuildConfig.DEBUG) {
                    Log.d("TtcEngine", "Track #${track.id}")
                    Log.d("TtcEngine", "  Distance = ${distance}m")
                    Log.d("TtcEngine", "  Velocity = ${velocity}m/s")
                    Log.d("TtcEngine", "  Closing Velocity = ${closingVelocity}m/s")
                    Log.d("TtcEngine", "  TTC = ${ttcSeconds ?: "Infinity"}s")
                    Log.d("TtcEngine", "  Risk = ${riskLevel.name}")
                }

                estimates.add(
                    TtcEstimate(
                        trackId = track.id,
                        distanceMeters = distance,
                        closingVelocityMetersPerSecond = closingVelocity,
                        ttcSeconds = ttcSeconds,
                        riskLevel = riskLevel
                    )
                )
            }

            val avgTtc = if (validTtcCount > 0) sumTtcSeconds / validTtcCount else 0.0f
            val duration = SystemClock.elapsedRealtime() - startTime

            return Pair(
                estimates,
                TtcMetadata(
                    processedTracks = tracks.size,
                    validTtcCount = validTtcCount,
                    criticalRiskCount = criticalRiskCount,
                    averageTtcSeconds = avgTtc,
                    processingTimeMs = duration,
                    successful = true
                )
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            return Pair(
                emptyList(),
                TtcMetadata(
                    processedTracks = tracks.size,
                    validTtcCount = 0,
                    criticalRiskCount = 0,
                    averageTtcSeconds = 0.0f,
                    processingTimeMs = duration,
                    successful = false,
                    errorMessage = e.localizedMessage ?: "Unknown TTC processing error."
                )
            )
        }
    }
}
