package com.pathhelper.ai.localization.slam
/**
* Coordinates Local Mapping Engine operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Local Mapping Engine.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class LocalMappingEngine {
    private val mapPoints = mutableListOf<SlamMapPoint>()
    private var nextId = 1L

    fun getMap(): SlamMap = SlamMap(mapPoints.toList())

    fun updateMap(
        features: List<VisualFeature>,
        matchedFeatures: Int
    ): Int {
        if (matchedFeatures < 20) {
            for (feat in features) {
                if (mapPoints.size >= 500) {
                    mapPoints.removeAt(0)
                }
                mapPoints.add(
                    SlamMapPoint(
                        id = nextId++,
                        x = feat.point.x,
                        y = feat.point.y,
                        descriptor = feat.descriptor,
                        observedCount = 1
                    )
                )
            }
        } else {
            for (point in mapPoints) {
                point.observedCount++
            }
        }
        return mapPoints.size
    }

    fun clear() {
        mapPoints.clear()
        nextId = 1L
    }
}
