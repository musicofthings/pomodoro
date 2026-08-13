package com.example.cozyfocus.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.cozyfocus.model.CompanionAnimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AnimalSoundSynthesizer {

    suspend fun playGreeting(companion: CompanionAnimal) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val durationSeconds = if (companion == CompanionAnimal.HORSE) 0.72 else 0.42
        val totalFrames = (sampleRate * durationSeconds).toInt()
        val buffer = FloatArray(totalFrames)

        val baseFreq = companion.baseFrequencyHz

        for (frame in 0 until totalFrames) {
            val progress = frame.toDouble() / totalFrames.toDouble()
            val envelope = sin(PI * min(1.0, progress * 1.3)) * (1.0 - max(0.0, progress - 0.75) * 3.2)

            val glide: Double = when (companion) {
                CompanionAnimal.CAT -> 1.0 + sin(progress * PI) * 0.65
                CompanionAnimal.PUPPY, CompanionAnimal.RED_PANDA -> 1.15 - progress * 0.35
                CompanionAnimal.HORSE -> 0.70 + progress * 0.85
                else -> 1.0
            }

            val time = frame.toDouble() / sampleRate.toDouble()
            val fundamental = sin(2 * PI * baseFreq * glide * time)
            val overtone = sin(2 * PI * baseFreq * glide * 2.02 * time) * 0.24
            val signal = ((fundamental + overtone) * envelope * 0.13).toFloat()

            buffer[frame] = signal
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBufferSize, buffer.size * 4))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        track.play()

        val sleepTimeMs = (durationSeconds * 1000).toLong() + 100
        kotlinx.coroutines.delay(sleepTimeMs)
        track.stop()
        track.release()
    }
}
