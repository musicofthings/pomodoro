package com.example.cozyfocus.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    fun generateAndShare(
        context: Context,
        companion: CompanionAnimal,
        cosmetic: Cosmetic?,
        completedSessions: Int,
        totalMinutes: Int
    ) {
        try {
            val bitmap = createShareBitmap(companion, cosmetic, completedSessions, totalMinutes)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "cozy_focus_share.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Cozy Focus Journey"))
            }
        } catch (_: Exception) {
        }
    }

    private fun createShareBitmap(
        companion: CompanionAnimal,
        cosmetic: Cosmetic?,
        completedSessions: Int,
        totalMinutes: Int
    ): Bitmap {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient Background
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                android.graphics.Color.argb(255, 255, 235, 215),
                android.graphics.Color.argb(255, 255, 248, 225),
                android.graphics.Color.WHITE
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(255, 140, 0)
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Header: COZY FOCUS
        canvas.drawText("COZY FOCUS", 108f, 140f, textPaint)

        // Subtitle: I showed up for myself today.
        val mainTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(40, 40, 40)
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("I showed up for myself today.", 108f, 750f, mainTextPaint)

        // Stats text
        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(110, 110, 110)
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✓ $completedSessions sessions   🕒 $totalMinutes minutes", 108f, 850f, statsPaint)

        // Companion & Cosmetic Symbol
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 340f
        }
        canvas.drawText(companion.symbol, 1380f, 850f, symbolPaint)

        if (cosmetic != null) {
            val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 120f
            }
            canvas.drawText(cosmetic.mark, 1490f, 540f, markPaint)
        }

        return bitmap
    }
}
