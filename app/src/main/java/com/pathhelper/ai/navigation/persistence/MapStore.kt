package com.pathhelper.ai.navigation.persistence

import android.content.Context
import java.io.File
/**
* Coordinates Map Store operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Map Store.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class MapStore(private val context: Context) {
    private val serializer = MapSerializer()

    fun save(map: PersistentMap) {
        val file = File(context.filesDir, "map_${map.mapId}.json")
        val serialized = serializer.serialize(map)
        file.writeText(serialized)
    }

    fun load(mapId: String): PersistentMap? {
        val file = File(context.filesDir, "map_${mapId}.json")
        if (!file.exists()) return null
        val content = file.readText()
        return serializer.deserialize(content)
    }

    fun delete(mapId: String): Boolean {
        val file = File(context.filesDir, "map_${mapId}.json")
        return if (file.exists()) file.delete() else false
    }

    fun listMapIds(): List<String> {
        val files = context.filesDir.listFiles { _, name -> name.startsWith("map_") && name.endsWith(".json") }
        return files?.map { it.name.substringAfter("map_").substringBefore(".json") } ?: emptyList()
    }
}
