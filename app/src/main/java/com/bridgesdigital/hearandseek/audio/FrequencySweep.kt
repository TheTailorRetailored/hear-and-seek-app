package com.bridgesdigital.hearandseek.audio

import kotlin.math.max
import kotlin.math.min

/**
 * A linear frequency sweep used by the game.
 */
data class FrequencySweep(
    val startHz: Double,
    val endHz: Double,
    val durationMillis: Long,
) {
    init {
        require(startHz > 0.0) { "startHz must be positive" }
        require(endHz > 0.0) { "endHz must be positive" }
        require(durationMillis > 0L) { "durationMillis must be positive" }
    }

    fun progressAt(elapsedMillis: Long): Double {
        val raw = elapsedMillis.toDouble() / durationMillis.toDouble()
        return min(1.0, max(0.0, raw))
    }

    fun frequencyAt(elapsedMillis: Long): Double {
        val progress = progressAt(elapsedMillis)
        return startHz + (endHz - startHz) * progress
    }
}
