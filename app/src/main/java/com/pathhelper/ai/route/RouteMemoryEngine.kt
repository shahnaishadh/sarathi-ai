package com.pathhelper.ai.route

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.context.NavigationContext
/**
* Coordinates Route Memory Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Route Memory Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class RouteMemoryEngine {
    private val routeLandmarks = mutableListOf<RouteLandmark>()

    var lastEvents: List<Pair<RouteLandmark, RouteEvent>> = emptyList()
        private set

    fun update(
        worldModel: WorldModel,
        navigationContext: NavigationContext
    ): Pair<RouteMemory, RouteMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        val eventsList = mutableListOf<Pair<RouteLandmark, RouteEvent>>()
        val currentIds = worldModel.landmarks.map { it.id }.toSet()

        try {
            // 1. Process visible landmarks
            for (landmark in worldModel.landmarks) {
                val existing = routeLandmarks.find { it.landmarkId == landmark.id }
                val dist = landmark.distanceMeters

                if (existing == null) {
                    // Landmark first seen
                    val newLm = RouteLandmark(
                        landmarkId = landmark.id,
                        landmarkType = landmark.type,
                        firstSeenTimestamp = currentTime,
                        lastSeenTimestamp = currentTime,
                        visitCount = 1,
                        event = RouteEvent.DISCOVERED
                    )
                    routeLandmarks.add(newLm)
                    eventsList.add(Pair(newLm, RouteEvent.DISCOVERED))

                    if (BuildConfig.DEBUG) {
                        Log.d("RouteMemory", "ROUTE EVENT ${newLm.landmarkId} DISCOVERED")
                    }
                } else {
                    // Landmark already exists in history
                    existing.lastSeenTimestamp = currentTime

                    if (existing.event == RouteEvent.PASSED) {
                        // Re-entering view after being passed
                        existing.event = RouteEvent.REVISITED
                        existing.visitCount++
                        eventsList.add(Pair(existing, RouteEvent.REVISITED))

                        if (BuildConfig.DEBUG) {
                            Log.d("RouteMemory", "ROUTE EVENT ${existing.landmarkId} REVISITED")
                        }
                    } else {
                        // Progression checks
                        if (dist != null && dist < 1.0f && existing.event != RouteEvent.REACHED) {
                            existing.event = RouteEvent.REACHED
                            eventsList.add(Pair(existing, RouteEvent.REACHED))

                            if (BuildConfig.DEBUG) {
                                Log.d("RouteMemory", "ROUTE EVENT ${existing.landmarkId} REACHED")
                            }
                        } else if (navigationContext.approachingLandmark?.landmarkId == landmark.id &&
                            (existing.event == RouteEvent.DISCOVERED || existing.event == RouteEvent.REVISITED)) {
                            existing.event = RouteEvent.APPROACHING
                            eventsList.add(Pair(existing, RouteEvent.APPROACHING))

                            if (BuildConfig.DEBUG) {
                                Log.d("RouteMemory", "ROUTE EVENT ${existing.landmarkId} APPROACHING")
                            }
                        }
                    }
                }
            }

            // 2. Process landmarks no longer visible (detect PASSED transitions)
            for (routeLm in routeLandmarks) {
                if (!currentIds.contains(routeLm.landmarkId)) {
                    if (routeLm.event == RouteEvent.REACHED || routeLm.event == RouteEvent.APPROACHING || routeLm.event == RouteEvent.DISCOVERED) {
                        routeLm.event = RouteEvent.PASSED
                        eventsList.add(Pair(routeLm, RouteEvent.PASSED))

                        if (BuildConfig.DEBUG) {
                            Log.d("RouteMemory", "ROUTE EVENT ${routeLm.landmarkId} PASSED")
                        }
                    }
                }
            }

            lastEvents = eventsList
            val duration = SystemClock.elapsedRealtime() - startTime

            val passedCount = routeLandmarks.count { it.event == RouteEvent.PASSED }
            val revisitedCount = routeLandmarks.count { it.visitCount > 1 }

            val metadata = RouteMetadata(
                trackedLandmarks = routeLandmarks.size,
                passedLandmarks = passedCount,
                revisitedLandmarks = revisitedCount,
                processingTimeMs = duration,
                successful = true
            )

            return Pair(RouteMemory(routeLandmarks.toList()), metadata)
        } catch (e: Exception) {
            lastEvents = emptyList()
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackMetadata = RouteMetadata(
                trackedLandmarks = routeLandmarks.size,
                passedLandmarks = 0,
                revisitedLandmarks = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown Route Memory Engine update error."
            )
            return Pair(RouteMemory(routeLandmarks.toList()), fallbackMetadata)
        }
    }
}
