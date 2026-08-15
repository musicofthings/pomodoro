package com.cozyfocus.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.cozyfocus.app.R

enum class AmbientSound(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val rawResId: Int
) {
    BLACK_NOISE("black_noise", "Black Noise", "🔇", R.raw.black_noise),
    WATERFALL("waterfall", "Waterfall", "🌊", R.raw.waterfall),
    RAINFALL("rainfall", "Dynamic Rainfall", "🌧️", R.raw.rainfall),
    OCEAN_WAVES("ocean_waves", "Ocean Waves", "🌊", R.raw.ocean_waves),
    BINAURAL_PEAK_FOCUS("binaural_peak_focus", "Binaural: Peak Focus", "🎧", R.raw.binaural_peak_focus),
    BINAURAL_ANALYTICAL("binaural_analytical", "Binaural: Analytical", "🎧", R.raw.binaural_analytical),
    BINAURAL_FLOW_STATE("binaural_flow_state", "Binaural: Flow State", "🎧", R.raw.binaural_flow_state),
    BINAURAL_SHORT_BREAK("binaural_short_break", "Binaural: Short Break", "🎧", R.raw.binaural_short_break),
    BINAURAL_LONG_BREAK("binaural_long_break", "Binaural: Long Break", "🎧", R.raw.binaural_long_break),
    ISOCHRONIC_ANALYTICAL("isochronic_analytical", "Isochronic: Analytical", "⚡", R.raw.isochronic_analytical),
    SOMATIC_PURR("somatic_purr", "Somatic Purr", "🐾", R.raw.somatic_purr),
    EAR_BRUSHING("ear_brushing", "Ear-to-Ear Brushing", "👂", R.raw.ear_brushing),
    STOCHASTIC_CRINKLE("stochastic_crinkle", "Stochastic Crinkle", "✨", R.raw.stochastic_crinkle),
    VINYL_CRACKLE("vinyl_crackle", "Lo-Fi Vinyl Crackle", "📻", R.raw.vinyl_crackle);

    companion object {
        fun fromId(id: String): AmbientSound {
            return entries.firstOrNull { it.id == id } ?: RAINFALL
        }
    }
}

class AmbientAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var selectedSound: AmbientSound = AmbientSound.RAINFALL
        private set
    var isPlaying: Boolean = false
        private set

    fun setSelectedSound(sound: AmbientSound) {
        val wasPlaying = isPlaying
        if (wasPlaying) {
            stop()
        }
        selectedSound = sound
        if (wasPlaying) {
            play()
        }
    }

    fun toggle() {
        if (isPlaying) stop() else play()
    }

    fun play() {
        if (isPlaying) return
        try {
            stop()
            val player = MediaPlayer.create(context, selectedSound.rawResId) ?: return
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.isLooping = true
            player.setVolume(0.45f, 0.45f)
            player.start()
            mediaPlayer = player
            isPlaying = true
        } catch (e: Exception) {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }
}
