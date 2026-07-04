package com.pathhelper.ai.navigation.persistence

import android.content.Context
/**
* Coordinates Map Repository operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Map Repository.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class MapRepository(context: Context) {
    private val store = MapStore(context)
    private var activeMap: PersistentMap? = null

    fun getActiveMap(): PersistentMap? {
        return activeMap
    }

    fun setActiveMap(map: PersistentMap?) {
        activeMap = map
    }

    fun saveMap(map: PersistentMap) {
        store.save(map)
        if (activeMap == null || activeMap?.mapId == map.mapId) {
            activeMap = map
        }
    }

    fun loadMap(mapId: String): PersistentMap? {
        val loaded = store.load(mapId)
        if (loaded != null) {
            activeMap = loaded
        }
        return loaded
    }

    fun deleteMap(mapId: String) {
        store.delete(mapId)
        if (activeMap?.mapId == mapId) {
            activeMap = null
        }
    }

    fun getAvailableMaps(): List<String> {
        return store.listMapIds()
    }
}
