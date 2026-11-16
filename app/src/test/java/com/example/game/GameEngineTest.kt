package com.example.game

import com.example.data.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun `swipe left and right changes target lane within bounds`() {
        val engine = GameEngine(SoundFxEngine()) {}
        engine.startNewRun()
        assertEquals(1, engine.targetLane)

        engine.swipeLeft()
        assertEquals(0, engine.targetLane)
        engine.swipeLeft()
        assertEquals(0, engine.targetLane)

        engine.swipeRight()
        assertEquals(1, engine.targetLane)
        engine.swipeRight()
        assertEquals(2, engine.targetLane)
        engine.swipeRight()
        assertEquals(2, engine.targetLane)
    }

    @Test
    fun `jump lifts player above ground and gravity returns player`() {
        val engine = GameEngine(SoundFxEngine()) {}
        engine.startNewRun()
        engine.swipeUp()
        repeat(5) { engine.step(0.03f) }
        assertTrue("Player should be airborne after jump", engine.playerY > 0.5f)
    }

    @Test
    fun `profile missions include magnet and jetpack objectives`() {
        val profile = PlayerProfile(
            missionTier = 1L,
            missionCoinsWithMagnet = 10L,
            missionJetpackFlights = 1L,
            missionTotalMeters = 200L
        )
        val missions = profile.activeMissions()
        assertEquals(3, missions.size)
        assertTrue(missions.any { it.id == "magnet" && !it.isCompleted })
        assertTrue(missions.any { it.id == "jetpack" && it.isCompleted })
    }
}
