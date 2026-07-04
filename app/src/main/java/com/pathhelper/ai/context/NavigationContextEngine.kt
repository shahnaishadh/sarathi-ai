package com.pathhelper.ai.context

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.world.LandmarkRelation
/**
* Coordinates Navigation Context Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Navigation Context Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class NavigationContextEngine {
    private val distanceHistory = mutableMapOf<String, MutableList<Float>>()

    fun analyze(
        worldModel: WorldModel
    ): Pair<NavigationContext, NavigationContextMetadata> {
        val startTime = SystemClock.elapsedRealtime()

        try {
            val prioritizedObservations = mutableListOf<ContextObservation>()
            val currentIds = worldModel.landmarks.map { it.id }.toSet()

            // Prune history for landmarks no longer visible
            distanceHistory.keys.retainAll(currentIds)

            var approachingLm: ContextObservation? = null

            for (landmark in worldModel.landmarks) {
                // 1. Resolve base priority
                val basePriority = when (landmark.type) {
                    LandmarkType.STAIRS, LandmarkType.ESCALATOR, LandmarkType.CROSSWALK -> ContextPriority.CRITICAL
                    LandmarkType.DOOR, LandmarkType.ENTRANCE, LandmarkType.ELEVATOR -> ContextPriority.HIGH
                    LandmarkType.HALLWAY -> ContextPriority.MEDIUM
                }

                // 2. Distance Boosting (< 3m increases priority level)
                val dist = landmark.distanceMeters
                val priority = if (dist != null && dist < 3.0f) {
                    when (basePriority) {
                        ContextPriority.LOW -> ContextPriority.MEDIUM
                        ContextPriority.MEDIUM -> ContextPriority.HIGH
                        ContextPriority.HIGH -> ContextPriority.CRITICAL
                        ContextPriority.CRITICAL -> ContextPriority.CRITICAL
                    }
                } else {
                    basePriority
                }

                // 3. Track approach velocity dynamically over multiple frames
                // Threshold: Must move at least 15cm closer over 3 frames to be "approaching"
                var isApproaching = false
                if (dist != null) {
                    val history = distanceHistory.getOrPut(landmark.id) { mutableListOf() }
                    history.add(dist)
                    if (history.size > 5) {
                        history.removeAt(0)
                    }
                    if (history.size >= 3) {
                        val totalDelta = history.first() - history.last()
                        val consistentlyMovingCloser = history.zipWithNext().all { it.first >= it.second }
                        if (consistentlyMovingCloser && totalDelta > 0.15f) {
                            isApproaching = true
                        }
                    }
                }

                // 4. Generate sequenced topological description
                var description = when (landmark.type) {
                    LandmarkType.DOOR, LandmarkType.ENTRANCE -> "Door ahead"
                    LandmarkType.CROSSWALK -> "Crosswalk ahead"
                    LandmarkType.ELEVATOR -> "Elevator ahead"
                    LandmarkType.STAIRS -> "Stairs ahead"
                    LandmarkType.ESCALATOR -> "Escalator ahead"
                    LandmarkType.HALLWAY -> "Hallway clear"
                }

                // Sequence checks based on WorldModel relations
                val aheadOfCrosswalk = worldModel.relations.any { 
                    it.sourceId == landmark.id && it.relation == LandmarkRelation.AHEAD_OF &&
                    worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.CROSSWALK
                }
                val connectedToDoor = worldModel.relations.any {
                    it.sourceId == landmark.id && it.relation == LandmarkRelation.CONNECTED_TO &&
                    (worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.DOOR ||
                     worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.ENTRANCE)
                }

                if (landmark.type == LandmarkType.DOOR || landmark.type == LandmarkType.ENTRANCE) {
                    if (aheadOfCrosswalk) {
                        description = "Crosswalk beyond doorway"
                    }
                } else if (landmark.type == LandmarkType.HALLWAY) {
                    if (connectedToDoor) {
                        description = "Door at end of hallway"
                    }
                }

                val obs = ContextObservation(
                    landmarkId = landmark.id,
                    landmarkType = landmark.type,
                    priority = priority,
                    distanceMeters = dist,
                    description = description
                )
                prioritizedObservations.add(obs)

                if (isApproaching) {
                    approachingLm = obs
                }
            }

            // 5. Select Primary & Secondary focus points
            // Sort by priority (descending) then distance (ascending, nulls at the end)
            val sortedObs = prioritizedObservations.sortedWith(
                compareByDescending<ContextObservation> { it.priority.ordinal }
                    .thenBy { it.distanceMeters ?: Float.MAX_VALUE }
            )

            val primary = sortedObs.getOrNull(0)
            val secondary = sortedObs.getOrNull(1)

            // 6. Resolve Active Context
            val activeContext = when {
                approachingLm != null && (approachingLm.landmarkType == LandmarkType.DOOR || approachingLm.landmarkType == LandmarkType.ENTRANCE) -> "Approaching Entrance"
                worldModel.landmarks.any { it.type == LandmarkType.CROSSWALK } -> "Crosswalk Ahead"
                worldModel.landmarks.any { it.type == LandmarkType.ELEVATOR } -> "Elevator Available"
                worldModel.landmarks.any { it.type == LandmarkType.HALLWAY } && (primary?.landmarkType == LandmarkType.HALLWAY) -> "Hallway Navigation"
                worldModel.landmarks.any { it.type == LandmarkType.HALLWAY } && worldModel.landmarks.any { it.type == LandmarkType.DOOR || it.type == LandmarkType.ENTRANCE } -> "Door At End Of Hallway"
                else -> "Open Path Forward"
            }

            // Debug logging
            if (BuildConfig.DEBUG) {
                primary?.let {
                    Log.d("NavigationContext", "PRIMARY ${it.landmarkId} Priority ${it.priority.name}")
                }
                secondary?.let {
                    Log.d("NavigationContext", "SECONDARY ${it.landmarkId} Priority ${it.priority.name}")
                }
                Log.d("NavigationContext", "CONTEXT $activeContext")
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            val metadata = NavigationContextMetadata(
                processedLandmarks = worldModel.landmarks.size,
                prioritizedLandmarks = prioritizedObservations.size,
                processingTimeMs = duration,
                successful = true
            )

            return Pair(
                NavigationContext(
                    primaryLandmark = primary,
                    secondaryLandmark = secondary,
                    approachingLandmark = approachingLm,
                    activeContext = activeContext,
                    observations = prioritizedObservations
                ),
                metadata
            )
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackMetadata = NavigationContextMetadata(
                processedLandmarks = 0,
                prioritizedLandmarks = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown Navigation Context Engine analysis error."
            )
            return Pair(
                NavigationContext(null, null, null, "Open Path Forward", emptyList()),
                fallbackMetadata
            )
        }
    }
}
