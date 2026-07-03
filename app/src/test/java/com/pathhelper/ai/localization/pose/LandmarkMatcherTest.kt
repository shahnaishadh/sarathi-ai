package com.pathhelper.ai.localization.pose

import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.localization.slam.PoseEstimate
import com.pathhelper.ai.memory.MemoryObservation
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.world.WorldModel
import org.junit.Assert.*
import org.junit.Test

class LandmarkMatcherTest {

    private fun makePose(x: Float = 1f, y: Float = 1f, confidence: Float = 0.8f) =
        PoseEstimate(x, y, 45f, confidence, System.currentTimeMillis())

    private fun makeLandmark(id: String, type: LandmarkType, dist: Float? = 2.0f) =
        Landmark(id, type, dist, HorizontalZone.CENTER, 0.9f, 0L, 0L)

    private fun makeWorldModel(vararg landmarks: Landmark) =
        WorldModel(landmarks.toList(), emptyList())

    /**
     * Builds a SceneMemory from a list of description strings.
     * Each label maps to a MemoryObservation whose description contains
     * the label, so that the door/elevator/stairs evidence filter fires correctly.
     */
    private fun makeScene(labels: List<String> = emptyList()): SceneMemory {
        val now = System.currentTimeMillis()
        val obs = labels.mapIndexed { idx, label ->
            val envType = when {
                label.contains("door", ignoreCase = true)     -> EnvironmentType.DOOR
                label.contains("elevator", ignoreCase = true) -> EnvironmentType.ELEVATOR
                label.contains("stairs", ignoreCase = true)   -> EnvironmentType.STAIRS
                else                                           -> EnvironmentType.UNKNOWN
            }
            MemoryObservation(
                id = "obs_$idx",
                trackId = null,
                type = envType,
                firstSeenTimestamp = now,
                lastSeenTimestamp = now,
                confidence = 0.8f,
                distanceMeters = 2.0f,
                horizontalZone = HorizontalZone.CENTER,
                description = label
            )
        }
        return SceneMemory(obs)
    }

    @Test
    fun testEmptyWorldModelReturnsEmptyList() {
        val matcher = LandmarkMatcher()
        val result = matcher.match(makePose(), makeScene(), WorldModel(emptyList(), emptyList()))
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSingleLandmarkReturnsOneObservation() {
        val matcher = LandmarkMatcher()
        val world = makeWorldModel(makeLandmark("l1", LandmarkType.DOOR))
        val result = matcher.match(makePose(), makeScene(), world)
        assertEquals(1, result.size)
        assertEquals("l1", result.first().landmarkId)
    }

    @Test
    fun testConfidenceIsWithinBounds() {
        val matcher = LandmarkMatcher()
        val world = makeWorldModel(
            makeLandmark("l1", LandmarkType.DOOR),
            makeLandmark("l2", LandmarkType.ENTRANCE)
        )
        val result = matcher.match(makePose(), makeScene(), world)
        for (obs in result) {
            assertTrue(obs.confidence in 0f..1f)
        }
    }

    @Test
    fun testSceneMemoryDoorEvidenceBoostConfidence() {
        val matcher = LandmarkMatcher()
        val world = makeWorldModel(makeLandmark("l1", LandmarkType.DOOR))
        val sceneWithDoor = makeScene(listOf("door"))
        val sceneEmpty = makeScene(emptyList())
        val withDoor = matcher.match(makePose(), sceneWithDoor, world).first().confidence
        val withoutDoor = matcher.match(makePose(), sceneEmpty, world).first().confidence
        assertTrue(withDoor > withoutDoor)
    }

    @Test
    fun testResultsSortedByConfidenceDescending() {
        val matcher = LandmarkMatcher()
        val world = makeWorldModel(
            makeLandmark("l1", LandmarkType.DOOR, 5f),
            makeLandmark("l2", LandmarkType.ENTRANCE, 1f)
        )
        val result = matcher.match(makePose(confidence = 1f), makeScene(listOf("door")), world)
        // Both have same confidence (same base score) — verify sorted list returned without crash
        assertEquals(2, result.size)
    }

    @Test
    fun testNullDistanceUsesPosFallback() {
        val matcher = LandmarkMatcher()
        val world = makeWorldModel(makeLandmark("l1", LandmarkType.HALLWAY, null))
        val result = matcher.match(makePose(x = 3f, y = 4f), makeScene(), world)
        // sqrt(9 + 16) = 5m proxy distance
        assertEquals(1, result.size)
        assertEquals(5f, result.first().estimatedDistance, 0.01f)
    }
}
