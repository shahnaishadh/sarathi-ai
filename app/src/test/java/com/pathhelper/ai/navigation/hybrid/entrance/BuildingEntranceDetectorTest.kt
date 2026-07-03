package com.pathhelper.ai.navigation.hybrid.entrance

import com.pathhelper.ai.environment.EnvironmentObservation
import com.pathhelper.ai.environment.EnvironmentType
import com.pathhelper.ai.memory.MemoryObservation
import com.pathhelper.ai.memory.SceneMemory
import com.pathhelper.ai.navigation.HorizontalZone
import com.pathhelper.ai.tracking.Track
import com.pathhelper.ai.world.Landmark
import com.pathhelper.ai.world.LandmarkType
import com.pathhelper.ai.world.WorldModel
import org.junit.Assert.*
import org.junit.Test

class BuildingEntranceDetectorTest {

    @Test
    fun testEntranceConfirmed() {
        val detector = BuildingEntranceDetector()
        
        // Mock a center vertical door track close to user
        val tracks = listOf(
            Track(
                id = 1,
                classId = 0,
                centerX = 320f, // Center aligned
                centerY = 240f,
                velocityX = 0f,
                velocityY = 0f,
                age = 10,
                missedFrames = 0,
                confidence = 0.95f,
                width = 100f,
                height = 200f // vertical aspect = 2.0 >= 1.4
            )
        )

        // Mock 4 door memory observations in scene memory
        val memoryObservations = List(4) { idx ->
            MemoryObservation(
                id = "mem_$idx",
                trackId = 1,
                type = EnvironmentType.DOOR,
                firstSeenTimestamp = 0L,
                lastSeenTimestamp = 100L,
                confidence = 0.9f,
                distanceMeters = 3f,
                horizontalZone = HorizontalZone.CENTER,
                description = "Door memory"
            )
        }
        val sceneMemory = SceneMemory(memoryObservations)

        // Mock registered door in world model
        val worldModel = WorldModel(
            landmarks = listOf(
                Landmark("lm_1", LandmarkType.DOOR, 3f, HorizontalZone.CENTER, 0.9f, 0L, 100L)
            ),
            relations = emptyList()
        )

        // 2m GPS distance (proximity score ~0.86)
        val (decision, confidenceAndMeta) = detector.evaluate(
            tracks = tracks,
            sceneMemory = sceneMemory,
            worldModel = worldModel,
            gpsDistance = 2.0f
        )
        val (confidence, _) = confidenceAndMeta

        assertEquals(EntranceDecision.ENTRANCE_CONFIRMED, decision)
        assertTrue(confidence.score >= 0.70f)
        assertEquals(0.95f, confidence.doorConfidence, 0.001f)
        assertEquals(1.0f, confidence.sceneMemoryEvidence, 0.001f)
        assertEquals(1.0f, confidence.worldModelEvidence, 0.001f)
    }

    @Test
    fun testEntranceRejected() {
        val detector = BuildingEntranceDetector()
        
        // No tracks, no landmarks, 80m GPS distance
        val (decision, confidenceAndMeta) = detector.evaluate(
            tracks = emptyList(),
            sceneMemory = SceneMemory(emptyList()),
            worldModel = WorldModel(emptyList(), emptyList()),
            gpsDistance = 80.0f
        )
        val (confidence, _) = confidenceAndMeta

        assertEquals(EntranceDecision.ENTRANCE_REJECTED, decision)
        assertEquals(0.0f, confidence.score, 0.001f)
    }

    @Test
    fun testEntranceMoreEvidenceRequired() {
        val detector = BuildingEntranceDetector()

        // Track is visible but GPS distance is 10m (medium proximity)
        val tracks = listOf(
            Track(
                id = 1,
                classId = 0,
                centerX = 320f,
                centerY = 240f,
                velocityX = 0f,
                velocityY = 0f,
                age = 5,
                missedFrames = 0,
                confidence = 0.6f,
                width = 100f,
                height = 200f
            )
        )

        val (decision, confidenceAndMeta) = detector.evaluate(
            tracks = tracks,
            sceneMemory = SceneMemory(emptyList()),
            worldModel = WorldModel(emptyList(), emptyList()),
            gpsDistance = 10.0f
        )
        val (confidence, _) = confidenceAndMeta

        assertEquals(EntranceDecision.MORE_EVIDENCE_REQUIRED, decision)
        assertTrue(confidence.score in 0.25f..0.70f)
    }
}
