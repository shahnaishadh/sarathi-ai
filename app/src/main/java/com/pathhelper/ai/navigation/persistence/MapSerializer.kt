package com.pathhelper.ai.navigation.persistence

import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.navigation.graph.GraphRelationship
/**
* Coordinates Map Serializer operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Map Serializer.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class MapSerializer {
    fun serialize(map: PersistentMap): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"mapId\":\"").append(map.mapId).append("\",")
        sb.append("\"createdTimestamp\":").append(map.createdTimestamp).append(",")
        sb.append("\"lastModifiedTimestamp\":").append(map.lastModifiedTimestamp).append(",")
        sb.append("\"version\":").append(map.version).append(",")

        sb.append("\"nodes\":[")
        for (i in map.nodes.indices) {
            val node = map.nodes[i]
            sb.append("{")
            sb.append("\"nodeId\":\"").append(node.nodeId).append("\",")
            sb.append("\"landmarkType\":\"").append(node.landmarkType.name).append("\",")
            sb.append("\"confidence\":").append(node.confidence)
            sb.append("}")
            if (i < map.nodes.size - 1) sb.append(",")
        }
        sb.append("],")

        sb.append("\"edges\":[")
        for (i in map.edges.indices) {
            val edge = map.edges[i]
            sb.append("{")
            sb.append("\"sourceId\":\"").append(edge.sourceId).append("\",")
            sb.append("\"destinationId\":\"").append(edge.destinationId).append("\",")
            sb.append("\"relationship\":\"").append(edge.relationship.name).append("\",")
            sb.append("\"weight\":").append(edge.weight)
            sb.append("}")
            if (i < map.edges.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.append("}")
        return sb.toString()
    }

    fun deserialize(jsonStr: String): PersistentMap {
        val mapId = extractString(jsonStr, "mapId")
        val createdTimestamp = extractLong(jsonStr, "createdTimestamp")
        val lastModifiedTimestamp = extractLong(jsonStr, "lastModifiedTimestamp")
        val version = extractInt(jsonStr, "version")

        val nodes = mutableListOf<MapNode>()
        val nodesSection = extractArraySection(jsonStr, "nodes")
        val nodeObjects = splitObjects(nodesSection)
        for (obj in nodeObjects) {
            if (obj.isNotBlank()) {
                val nodeId = extractString(obj, "nodeId")
                val typeStr = extractString(obj, "landmarkType")
                val landmarkType = LandmarkType.valueOf(typeStr)
                val confidence = extractFloat(obj, "confidence")
                nodes.add(MapNode(nodeId, landmarkType, confidence))
            }
        }

        val edges = mutableListOf<MapEdge>()
        val edgesSection = extractArraySection(jsonStr, "edges")
        val edgeObjects = splitObjects(edgesSection)
        for (obj in edgeObjects) {
            if (obj.isNotBlank()) {
                val sourceId = extractString(obj, "sourceId")
                val destinationId = extractString(obj, "destinationId")
                val relStr = extractString(obj, "relationship")
                val relationship = GraphRelationship.valueOf(relStr)
                val weight = extractFloat(obj, "weight")
                edges.add(MapEdge(sourceId, destinationId, relationship, weight))
            }
        }

        return PersistentMap(mapId, nodes, edges, createdTimestamp, lastModifiedTimestamp, version)
    }

    private fun extractString(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractLong(json: String, key: String): Long {
        val pattern = "\"$key\"\\s*:\\s*([0-9\\-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLong() ?: 0L
    }

    private fun extractInt(json: String, key: String): Int {
        val pattern = "\"$key\"\\s*:\\s*([0-9\\-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractFloat(json: String, key: String): Float {
        val pattern = "\"$key\"\\s*:\\s*([0-9\\.\\-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toFloat() ?: 0.0f
    }

    private fun extractArraySection(json: String, key: String): String {
        val startIndex = json.indexOf("\"$key\"")
        if (startIndex == -1) return ""
        val bracketStart = json.indexOf("[", startIndex)
        if (bracketStart == -1) return ""
        var bracketCount = 1
        var index = bracketStart + 1
        while (index < json.length && bracketCount > 0) {
            if (json[index] == '[') bracketCount++
            else if (json[index] == ']') bracketCount--
            index++
        }
        return json.substring(bracketStart + 1, index - 1)
    }

    private fun splitObjects(arraySection: String): List<String> {
        val list = mutableListOf<String>()
        var index = 0
        while (index < arraySection.length) {
            val objStart = arraySection.indexOf("{", index)
            if (objStart == -1) break
            var braceCount = 1
            var i = objStart + 1
            while (i < arraySection.length && braceCount > 0) {
                if (arraySection[i] == '{') braceCount++
                else if (arraySection[i] == '}') braceCount--
                i++
            }
            list.add(arraySection.substring(objStart, i))
            index = i
        }
        return list
    }
}
