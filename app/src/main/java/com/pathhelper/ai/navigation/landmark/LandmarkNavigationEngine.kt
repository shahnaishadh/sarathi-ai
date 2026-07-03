package com.pathhelper.ai.navigation.landmark

import android.os.SystemClock
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.world.WorldModel
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.world.LandmarkRelation
import com.pathhelper.ai.context.NavigationContext
import com.pathhelper.ai.route.RouteMemory
import com.pathhelper.ai.route.RouteEvent
import com.pathhelper.ai.navigation.graph.RoutePlan
import com.pathhelper.ai.navigation.common.target.NavigationTarget
import com.pathhelper.ai.navigation.common.target.NavigationProgress
import com.pathhelper.ai.navigation.common.target.NavigationStep
import com.pathhelper.ai.navigation.common.target.LandmarkTarget
/**
* Coordinates Landmark Navigation Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Landmark Navigation Engine.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class LandmarkNavigationEngine {
    private var previouslyVisible = false
    private var lastProgress = NavigationProgress.SEARCHING
    private val completedStepsList = mutableListOf<NavigationStep>()

    fun navigate(
        destination: NavigationTarget,
        worldModel: WorldModel,
        context: NavigationContext,
        routeMemory: RouteMemory,
        routePlan: RoutePlan
    ): Pair<LandmarkNavigationState, LandmarkNavigationMetadata> {
        val startTime = SystemClock.elapsedRealtime()
        val currentTime = SystemClock.elapsedRealtime()

        try {
            if (destination !is LandmarkTarget) {
                // Return fallback state for location/gps target types
                val duration = SystemClock.elapsedRealtime() - startTime
                val state = LandmarkNavigationState(destination, NavigationProgress.LOST, null, emptyList(), null)
                val metadata = LandmarkNavigationMetadata(false, 0, duration, true)
                return Pair(state, metadata)
            }

            val targetType = destination.landmarkType
            val targetLm = worldModel.landmarks.find { it.type == targetType }
            val found = targetLm != null

            // Determine navigation progress state
            val state = when {
                routePlan.plannedPath.isEmpty() -> {
                    if (previouslyVisible) {
                        NavigationProgress.LOST
                    } else {
                        NavigationProgress.SEARCHING
                    }
                }
                else -> {
                    previouslyVisible = true
                    val dist = routePlan.estimatedDistanceMeters ?: targetLm?.distanceMeters
                    when {
                        dist != null && dist < 1.0f -> NavigationProgress.ARRIVED
                        context.approachingLandmark?.landmarkType == targetType -> NavigationProgress.APPROACHING
                        else -> NavigationProgress.ROUTE_FOUND
                    }
                }
            }

            // Generate step directions based on targets, plans, and structures
            val generatedSteps = mutableListOf<NavigationStep>()

            if (state == NavigationProgress.SEARCHING) {
                val targetName = targetType.name.lowercase()
                generatedSteps.add(NavigationStep("Searching for $targetName", 0.5f, currentTime))
            } else if (state == NavigationProgress.LOST) {
                generatedSteps.add(NavigationStep("Destination temporarily lost", 0.5f, currentTime))
            } else if (state == NavigationProgress.ARRIVED) {
                generatedSteps.add(NavigationStep("Destination reached", 1.0f, currentTime))
            } else if (routePlan.plannedPath.isNotEmpty()) {
                // Multi-hop path instructions from the RoutePlanner override heuristics
                for (inst in routePlan.instructions) {
                    generatedSteps.add(NavigationStep(inst, 0.9f, currentTime))
                }
            } else {
                // Heuristic fallback step generation
                when (targetType) {
                    LandmarkType.ELEVATOR -> {
                        val hasDoor = worldModel.landmarks.any { it.type == LandmarkType.DOOR || it.type == LandmarkType.ENTRANCE }
                        val isDoorPassed = routeMemory.landmarks.any { 
                            (it.landmarkType == LandmarkType.DOOR || it.landmarkType == LandmarkType.ENTRANCE) &&
                            it.event == RouteEvent.PASSED 
                        }
                        val hasHallway = worldModel.landmarks.any { it.type == LandmarkType.HALLWAY }

                        if (hasDoor && !isDoorPassed) {
                            generatedSteps.add(NavigationStep("Pass doorway", 0.9f, currentTime))
                        }
                        if (hasHallway) {
                            generatedSteps.add(NavigationStep("Continue hallway", 0.9f, currentTime))
                        }
                        generatedSteps.add(NavigationStep("Elevator ahead", 0.9f, currentTime))
                    }
                    LandmarkType.CROSSWALK -> {
                        val aheadOfCrosswalk = worldModel.relations.any { 
                            it.relation == LandmarkRelation.AHEAD_OF &&
                            worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.CROSSWALK
                        }
                        if (aheadOfCrosswalk) {
                            generatedSteps.add(NavigationStep("Crosswalk beyond doorway", 0.9f, currentTime))
                        } else {
                            generatedSteps.add(NavigationStep("Continue forward", 0.8f, currentTime))
                            generatedSteps.add(NavigationStep("Crosswalk ahead", 0.9f, currentTime))
                        }
                    }
                    LandmarkType.DOOR, LandmarkType.ENTRANCE -> {
                        val connectedToDoor = worldModel.relations.any {
                            it.relation == LandmarkRelation.CONNECTED_TO &&
                            (worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.DOOR ||
                             worldModel.landmarks.find { target -> target.id == it.targetId }?.type == LandmarkType.ENTRANCE)
                        }
                        if (connectedToDoor) {
                            generatedSteps.add(NavigationStep("Continue through hallway", 0.9f, currentTime))
                        } else {
                            generatedSteps.add(NavigationStep("Proceed forward", 0.8f, currentTime))
                        }
                    }
                    else -> {
                        generatedSteps.add(NavigationStep("Proceed forward", 0.8f, currentTime))
                    }
                }
            }

            // Progress steps logging and archiving
            val currentStep = generatedSteps.firstOrNull()

            if (state == NavigationProgress.ARRIVED && lastProgress != NavigationProgress.ARRIVED) {
                currentStep?.let { completedStepsList.add(it) }
            }
            lastProgress = state

            // Logging triggers
            if (BuildConfig.DEBUG) {
                if (found) {
                    Log.d("LandmarkNavigation", "DESTINATION ${targetType.name} FOUND")
                }
                currentStep?.let {
                    Log.d("LandmarkNavigation", "STEP ${it.instruction}")
                }
                Log.d("LandmarkNavigation", "STATE ${state.name}")
                if (state == NavigationProgress.ARRIVED) {
                    Log.d("LandmarkNavigation", "DESTINATION REACHED")
                }
            }

            val duration = SystemClock.elapsedRealtime() - startTime
            val metadata = LandmarkNavigationMetadata(
                destinationFound = found,
                generatedSteps = generatedSteps.size,
                processingTimeMs = duration,
                successful = true
            )

            val navigationState = LandmarkNavigationState(
                destination = destination,
                progress = state,
                currentStep = currentStep,
                completedSteps = completedStepsList.toList(),
                remainingDistanceMeters = routePlan.estimatedDistanceMeters ?: targetLm?.distanceMeters
            )

            return Pair(navigationState, metadata)
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val fallbackState = LandmarkNavigationState(destination, NavigationProgress.LOST, null, emptyList(), null)
            val fallbackMetadata = LandmarkNavigationMetadata(
                destinationFound = false,
                generatedSteps = 0,
                processingTimeMs = duration,
                successful = false,
                errorMessage = e.localizedMessage ?: "Unknown Landmark Navigation Engine navigate error."
            )
            return Pair(fallbackState, fallbackMetadata)
        }
    }
}
