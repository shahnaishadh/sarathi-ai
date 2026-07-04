package com.pathhelper.ai.world

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.memory.MemoryObservation
/**
* Coordinates World Model Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for World Model Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class WorldModelEngine {

    fun build(
        memory: SceneMemory
    ): Pair<WorldModel, WorldModelMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val landmarksList = mutableListOf<Landmark>()
        val relationsList = mutableListOf<WorldRelationship>()

        try {
            // 1. Translate MemoryObservation -> Landmark
            for (obs in memory.observations) {
                val landmarkType = when (obs.type) {
                    EnvironmentType.DOOR -> LandmarkType.DOOR
                    EnvironmentType.STAIRS -> LandmarkType.STAIRS
                    EnvironmentType.CROSSWALK -> LandmarkType.CROSSWALK
                    EnvironmentType.ELEVATOR -> LandmarkType.ELEVATOR
                    EnvironmentType.ESCALATOR -> LandmarkType.ESCALATOR
                    EnvironmentType.ENTRANCE -> LandmarkType.ENTRANCE
                    EnvironmentType.HALLWAY -> LandmarkType.HALLWAY
                    EnvironmentType.UNKNOWN -> continue // Skip unknown categories
                }

                val landmark = Landmark(
                    id = obs.id,
                    type = landmarkType,
                    distanceMeters = obs.distanceMeters,
                    horizontalZone = obs.horizontalZone,
                    confidence = obs.confidence,
                    firstSeenTimestamp = obs.firstSeenTimestamp,
                    lastSeenTimestamp = obs.lastSeenTimestamp
                )
                landmarksList.add(landmark)

                if (BuildConfig.DEBUG) {
                    Log.d("WorldModelEngine", "LANDMARK ${landmark.id} Distance: ${landmark.distanceMeters ?: 0f}m")
                }
            }

            // 2. Generate relationships
            val n = landmarksList.size
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val l1 = landmarksList[i]
                    val l2 = landmarksList[j]

                    // A. LEFT_OF / RIGHT_OF
                    val z1 = l1.horizontalZone
                    val z2 = l2.horizontalZone
                    if (z1 != z2) {
                        val val1 = when (z1) {
                            HorizontalZone.LEFT -> 0
                            HorizontalZone.CENTER -> 1
                            HorizontalZone.RIGHT -> 2
                        }
                        val val2 = when (z2) {
                            HorizontalZone.LEFT -> 0
                            HorizontalZone.CENTER -> 1
                            HorizontalZone.RIGHT -> 2
                        }
                        if (val1 < val2) {
                            relationsList.add(WorldRelationship(l1.id, l2.id, LandmarkRelation.LEFT_OF))
                            relationsList.add(WorldRelationship(l2.id, l1.id, LandmarkRelation.RIGHT_OF))
                            if (BuildConfig.DEBUG) {
                                Log.d("WorldModelEngine", "RELATION ${l1.id} LEFT_OF ${l2.id}")
                            }
                        } else {
                            relationsList.add(WorldRelationship(l2.id, l1.id, LandmarkRelation.LEFT_OF))
                            relationsList.add(WorldRelationship(l1.id, l2.id, LandmarkRelation.RIGHT_OF))
                            if (BuildConfig.DEBUG) {
                                Log.d("WorldModelEngine", "RELATION ${l2.id} LEFT_OF ${l1.id}")
                            }
                        }
                    }

                    // B. AHEAD_OF / BEHIND
                    val d1 = l1.distanceMeters
                    val d2 = l2.distanceMeters
                    if (d1 != null && d2 != null) {
                        if (d1 < d2) {
                            relationsList.add(WorldRelationship(l1.id, l2.id, LandmarkRelation.AHEAD_OF))
                            relationsList.add(WorldRelationship(l2.id, l1.id, LandmarkRelation.BEHIND))
                            if (BuildConfig.DEBUG) {
                                Log.d("WorldModelEngine", "RELATION ${l1.id} AHEAD_OF ${l2.id}")
                            }
                        } else if (d1 > d2) {
                            relationsList.add(WorldRelationship(l2.id, l1.id, LandmarkRelation.AHEAD_OF))
                            relationsList.add(WorldRelationship(l1.id, l2.id, LandmarkRelation.BEHIND))
                            if (BuildConfig.DEBUG) {
                                Log.d("WorldModelEngine", "RELATION ${l2.id} AHEAD_OF ${l1.id}")
                            }
                        }
                    }

                    // C. CONNECTED_TO
                    val isL1Door = l1.type == LandmarkType.DOOR || l1.type == LandmarkType.ENTRANCE
                    val isL2Door = l2.type == LandmarkType.DOOR || l2.type == LandmarkType.ENTRANCE
                    val isL1Hall = l1.type == LandmarkType.HALLWAY
                    val isL2Hall = l2.type == LandmarkType.HALLWAY

                    if ((isL1Door && isL2Hall) || (isL2Door && isL1Hall)) {
                        relationsList.add(WorldRelationship(l1.id, l2.id, LandmarkRelation.CONNECTED_TO))
                        relationsList.add(WorldRelationship(l2.id, l1.id, LandmarkRelation.CONNECTED_TO))
                        if (BuildConfig.DEBUG) {
                            Log.d("WorldModelEngine", "RELATION ${l1.id} CONNECTED_TO ${l2.id}")
                        }
                    }
                }
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            val metadata = WorldModelMetadata(
                landmarks = landmarksList.size,
                relations = relationsList.size,
                processingTimeMs = duration,
                successful = true
            )

            return Pair(WorldModel(landmarksList, relationsList), metadata)
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackMetadata = WorldModelMetadata(
                landmarks = 0,
                relations = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown World Model Engine building error."
            )
            return Pair(WorldModel(emptyList(), emptyList()), fallbackMetadata)
        }
    }
}
