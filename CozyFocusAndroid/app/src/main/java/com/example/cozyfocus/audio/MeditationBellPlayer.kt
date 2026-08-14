package com.example.cozyfocus.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.cozyfocus.R

class MeditationBellPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play() {
        try {
            mediaPlayer?.release()
            val player = MediaPlayer.create(context, R.raw.meditation_bell) ?: return
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setVolume(0.55f, 0.55f)
            player.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            mediaPlayer = null
        }
    }
}
