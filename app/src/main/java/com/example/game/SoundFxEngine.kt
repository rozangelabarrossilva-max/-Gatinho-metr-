package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundFxEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    var isMuted: Boolean = false

    fun playCoin(streak: Int = 0) {
        if (isMuted) return
        val semitoneBoost = (streak % 8) * 35.0
        playTone(
            startFreq = 880.0 + semitoneBoost,
            endFreq = 1320.0 + semitoneBoost,
            durationMs = 95,
            volume = 0.22f,
            waveHarmonic = 0.25f
        )
    }

    fun playSwipe() {
        if (isMuted) return
        playTone(startFreq = 380.0, endFreq = 210.0, durationMs = 70, volume = 0.14f, waveHarmonic = 0.0f)
    }

    fun playJump() {
        if (isMuted) return
        playTone(startFreq = 320.0, endFreq = 680.0, durationMs = 130, volume = 0.20f, waveHarmonic = 0.15f)
    }

    fun playPowerUp(isJetpack: Boolean) {
        if (isMuted) return
        scope.launch {
            val notes = if (isJetpack) listOf(523.25, 659.25, 783.99, 1046.50) else listOf(440.0, 554.37, 659.25, 880.0)
            notes.forEachIndexed { idx, freq ->
                playToneBlocking(
                    startFreq = freq,
                    endFreq = freq * 1.05,
                    durationMs = 80,
                    volume = 0.22f,
                    waveHarmonic = 0.2f
                )
                if (idx < notes.lastIndex) Thread.sleep(10)
            }
        }
    }

    fun playMissionComplete() {
        if (isMuted) return
        scope.launch {
            val notes = listOf(587.33, 739.99, 880.0, 1174.66)
            for (freq in notes) {
                playToneBlocking(freq, freq, 95, 0.24f, 0.25f)
            }
        }
    }

    fun playCrash() {
        if (isMuted) return
        playTone(startFreq = 240.0, endFreq = 75.0, durationMs = 280, volume = 0.30f, waveHarmonic = 0.45f)
    }

    private fun playTone(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float,
        waveHarmonic: Float
    ) {
        scope.launch {
            playToneBlocking(startFreq, endFreq, durationMs, volume, waveHarmonic)
        }
    }

    private fun playToneBlocking(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float,
        waveHarmonic: Float
    ) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            if (numSamples <= 0) return
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = startFreq + (endFreq - startFreq) * t
                phase += 2.0 * PI * freq / sampleRate
                val env = exp(-3.5 * t) * (1.0 - t * 0.2)
                val sample = sin(phase) + waveHarmonic * sin(phase * 2.0)
                val scaled = (sample * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[i] = scaled.toShort()
            }
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // AudioTrack may be unavailable in headless Robolectric tests
        }
    }
}
