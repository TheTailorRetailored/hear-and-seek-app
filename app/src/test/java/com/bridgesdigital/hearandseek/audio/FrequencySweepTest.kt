package com.bridgesdigital.hearandseek.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequencySweepTest {
    private val sweep = FrequencySweep(
        startHz = 20_000.0,
        endHz = 800.0,
        durationMillis = 90_000L,
    )

    @Test
    fun startsAtConfiguredFrequency() {
        assertEquals(20_000.0, sweep.frequencyAt(0L), 0.0001)
    }

    @Test
    fun reachesMidpointHalfwayThrough() {
        assertEquals(10_400.0, sweep.frequencyAt(45_000L), 0.0001)
    }

    @Test
    fun endsAtConfiguredFrequency() {
        assertEquals(800.0, sweep.frequencyAt(90_000L), 0.0001)
    }

    @Test
    fun clampsBeforeAndAfterSweep() {
        assertEquals(20_000.0, sweep.frequencyAt(-1_000L), 0.0001)
        assertEquals(800.0, sweep.frequencyAt(120_000L), 0.0001)
    }
}
