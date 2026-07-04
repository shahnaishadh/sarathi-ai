package com.pathhelper.ai.navigation.hybrid

/**
 * Defines the contract for Hybrid Route Step operations.
 *
 * Explain:
 * * Purpose of the component: Represents a single step in a hybrid indoor/outdoor navigation route.
 * * Role within the Sarthi architecture: Serves as the data model for routing engines to convey step instructions.
 * * Major inputs and outputs: Not applicable.
 */
sealed interface HybridRouteStep {
    val description: String
    val distanceMeters: Float

    /**
     * Represents the data structures or state of Outdoor steps.
     */
    data class Outdoor(
        override val description: String,
        override val distanceMeters: Float,
        val latitude: Double,
        val longitude: Double
    ) : HybridRouteStep

    /**
     * Represents the data structures or state of Indoor steps.
     */
    data class Indoor(
        override val description: String,
        override val distanceMeters: Float,
        val landmarkId: String
    ) : HybridRouteStep
}
