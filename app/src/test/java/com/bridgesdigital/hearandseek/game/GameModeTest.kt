package com.bridgesdigital.hearandseek.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeTest {
    @Test
    fun modesIncreaseInDuration() {
        val durations = GameMode.entries.map { it.durationMillis }
        assertEquals(durations.sorted(), durations)
        assertTrue(durations.zipWithNext().all { (first, second) -> first < second })
    }

    @Test
    fun classicPreservesOriginalRoundLength() {
        assertEquals(90_000L, GameMode.CLASSIC.durationMillis)
        assertEquals(15, GameMode.CLASSIC.hideSeconds)
    }

    @Test
    fun marathonRunsForFiveMinutes() {
        assertEquals(300_000L, GameMode.MARATHON.durationMillis)
    }
}
