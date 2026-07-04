package com.pathhelper.ai.memory

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.environment.EnvironmentObservation
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.navigation.HorizontalZone
import java.util.Locale
import kotlin.math.abs
/**
* Coordinates Scene Memory Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Scene Memory Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class SceneMemoryEngine {
    private val activeMemories = mutableListOf<MemoryObservation>()
    private val sequenceCounters = mutableMapOf<String, Int>()

    var lastEvents: List<Pair<MemoryObservation, MemoryEvent>> = emptyList()
        private set

    private fun generateUniqueId(type: EnvironmentType, zone: HorizontalZone): String {
        val key = "${type.name}_${zone.name}"
        val count = sequenceCounters.getOrPut(key) { 1 }
        sequenceCounters[key] = count + 1
        return "${type.name}_${zone.name}_${String.format(Locale.US, "%03d", count)}"
    }

    private fun isNeighbor(z1: HorizontalZone, z2: HorizontalZone): Boolean {
        if (z1 == z2) return true
        if (z1 == HorizontalZone.CENTER || z2 == HorizontalZone.CENTER) return true
        return false // LEFT and RIGHT are not neighbors
    }

    fun update(
        observations: List<EnvironmentObservation>
    ): Pair<SceneMemory, MemoryMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        val eventsList = mutableListOf<Pair<MemoryObservation, MemoryEvent>>()
        var expiredCount = 0
        var newCount = 0
        var updatedCount = 0

        try {
            // 1. Check for expired memories (10 seconds timeout)
            val iterator = activeMemories.iterator()
            while (iterator.hasNext()) {
                val memory = iterator.next()
                if (currentTime - memory.lastSeenTimestamp > 10_000L) {
                    iterator.remove()
                    eventsList.add(Pair(memory, MemoryEvent.EXPIRED))
                    expiredCount++

                    if (BuildConfig.DEBUG) {
                        Log.d("SceneMemory", "EXPIRED ${memory.id} Removed after timeout")
                    }
                }
            }

            // 2. Match current observations with active memories
            for (obs in observations) {
                var matchedMemory: MemoryObservation? = null

                // A. Primary Match: Track ID (if not null)
                if (obs.trackId != null) {
                    matchedMemory = activeMemories.find { it.trackId == obs.trackId && it.type == obs.type }
                }

                // B. Fallback Match: Environment Type + Zone (with zone-neighbor tolerance)
                if (matchedMemory == null) {
                    matchedMemory = activeMemories.find { memory ->
                        memory.type == obs.type && (
                            memory.horizontalZone == obs.horizontalZone ||
                            isNeighbor(memory.horizontalZone, obs.horizontalZone)
                        )
                    }
                }

                // C. Spatial Match (Proximity)
                if (matchedMemory == null && obs.distanceMeters != null) {
                    matchedMemory = activeMemories.find { memory ->
                        memory.type == obs.type && 
                        memory.distanceMeters?.let { abs(it - obs.distanceMeters!!) < 1.0f } == true
                    }
                }

                if (matchedMemory != null) {
                    // Update and smooth parameters
                    matchedMemory.lastSeenTimestamp = currentTime
                    matchedMemory.confidence = (0.7f * matchedMemory.confidence) + (0.3f * obs.confidence)

                    val oldDist = matchedMemory.distanceMeters
                    val newDist = obs.distanceMeters
                    if (oldDist != null && newDist != null) {
                        matchedMemory.distanceMeters = (0.8f * oldDist) + (0.2f * newDist)
                    } else if (newDist != null) {
                        matchedMemory.distanceMeters = newDist
                    }

                    eventsList.add(Pair(matchedMemory, MemoryEvent.UPDATED))
                    updatedCount++

                    if (BuildConfig.DEBUG) {
                        Log.d("SceneMemory", "UPDATED ${matchedMemory.id} ${matchedMemory.description}")
                    }

                    if (matchedMemory.description.lowercase().contains("person")) {
                        Log.d("PERSON_WARNING", 
                            "className=PERSON confidence=${matchedMemory.confidence} distance=${matchedMemory.distanceMeters} " +
                            "trackId=${matchedMemory.trackId} trackAge=-1 source=SceneMemoryEngine_Updated memoryId=${matchedMemory.id}")
                    }
                } else {
                    // Create new memory with stable ID
                    val uniqueId = generateUniqueId(obs.type, obs.horizontalZone)
                    val newMemory = MemoryObservation(
                        id = uniqueId,
                        trackId = obs.trackId,
                        type = obs.type,
                        firstSeenTimestamp = currentTime,
                        lastSeenTimestamp = currentTime,
                        confidence = obs.confidence,
                        distanceMeters = obs.distanceMeters,
                        horizontalZone = obs.horizontalZone,
                        description = obs.description
                    )
                    activeMemories.add(newMemory)

                    if (newMemory.description.lowercase().contains("person")) {
                        Log.d("PERSON_WARNING", 
                            "className=PERSON confidence=${newMemory.confidence} distance=${newMemory.distanceMeters} " +
                            "trackId=${newMemory.trackId} trackAge=-1 source=SceneMemoryEngine_New memoryId=${newMemory.id}")
                    }

                    eventsList.add(Pair(newMemory, MemoryEvent.NEW))
                    newCount++

                    if (BuildConfig.DEBUG) {
                        Log.d("SceneMemory", "NEW ${newMemory.id} ${newMemory.description}")
                    }
                }
            }

            lastEvents = eventsList
            val duration = SystemClock.elapsedRealtime() - startTime

            val metadata = MemoryMetadata(
                activeObservations = activeMemories.size,
                newObservations = newCount,
                updatedObservations = updatedCount,
                expiredObservations = expiredCount,
                processingTimeMs = duration,
                successful = true
            )

            return Pair(SceneMemory(activeMemories.toList()), metadata)
        } catch (e: Exception) {
            lastEvents = emptyList()
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackMetadata = MemoryMetadata(
                activeObservations = activeMemories.size,
                newObservations = 0,
                updatedObservations = 0,
                expiredObservations = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown scene memory engine update error."
            )
            return Pair(SceneMemory(activeMemories.toList()), fallbackMetadata)
        }
    }
}
