package com.bridgesdigital.hearandseek.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Streams a smooth sine-wave frequency sweep through the phone's media output.
 */
class TonePlayer(
    private val sampleRate: Int = 48_000,
    private val amplitude: Double = 0.18,
) {
    private val generation = AtomicInteger(0)

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var worker: Thread? = null

    fun start(sweep: FrequencySweep) {
        stop()
        val playbackGeneration = generation.incrementAndGet()

        worker = thread(name = "hear-and-seek-tone", isDaemon = true) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val minimumBufferBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufferSamples = maxOf(2_048, minimumBufferBytes / Short.SIZE_BYTES)
            val samples = ShortArray(bufferSamples)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSamples * Short.SIZE_BYTES * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            var phase = 0.0
            val startedAt = SystemClock.elapsedRealtime()

            try {
                track.play()

                while (generation.get() == playbackGeneration) {
                    val bufferStartedAt = SystemClock.elapsedRealtime() - startedAt
                    if (bufferStartedAt >= sweep.durationMillis) break

                    for (index in samples.indices) {
                        val sampleElapsedMillis = bufferStartedAt +
                            (index.toDouble() * 1_000.0 / sampleRate.toDouble()).toLong()
                        val frequency = sweep.frequencyAt(sampleElapsedMillis)
                        phase += 2.0 * PI * frequency / sampleRate.toDouble()
                        if (phase >= 2.0 * PI) phase %= 2.0 * PI

                        samples[index] = (
                            sin(phase) * Short.MAX_VALUE.toDouble() * amplitude
                            ).toInt().toShort()
                    }

                    val written = track.write(
                        samples,
                        0,
                        samples.size,
                        AudioTrack.WRITE_BLOCKING,
                    )
                    if (written < 0) break
                }
            } finally {
                runCatching { track.pause() }
                runCatching { track.flush() }
                runCatching { track.stop() }
                runCatching { track.release() }
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    fun stop() {
        generation.incrementAndGet()
        runCatching { audioTrack?.pause() }
        worker?.interrupt()
        worker = null
    }
}
