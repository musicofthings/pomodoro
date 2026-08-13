package com.example.cozyfocus.model

import androidx.compose.ui.graphics.Color

enum class CompanionAnimal(
    val id: String,
    val displayName: String,
    val symbol: String,
    val accentColor: Color,
    val baseFrequencyHz: Double
) {
    RED_PANDA("redPanda", "Red Panda", "🦊", Color(0xFFFF9233), 780.0),
    CAPYBARA("capybara", "Capybara", "🦫", Color(0xFF8D5B34), 230.0),
    RABBIT("rabbit", "Rabbit", "🐇", Color(0xFFAB77C2), 620.0),
    PUPPY("puppy", "Puppy", "🐶", Color(0xFFE5A93C), 430.0),
    CAT("cat", "Cat", "🐈", Color(0xFFE960A2), 480.0),
    HORSE("horse", "Horse", "🐴", Color(0xFF2EA09A), 180.0);

    companion object {
        fun fromRaw(raw: String): CompanionAnimal {
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: RED_PANDA
        }
    }
}

enum class Cosmetic(
    val id: String,
    val displayName: String,
    val mark: String,
    val price: Int
) {
    FLOWER_CROWN("flowerCrown", "Flower crown", "🌼", 15),
    SUN_HAT("sunHat", "Sun hat", "👒", 20),
    BANDANA("bandana", "Bandana", "🧣", 10),
    ROUND_GLASSES("roundGlasses", "Round glasses", "👓", 25),
    BOW_TIE("bowTie", "Bow tie", "🎀", 15);

    companion object {
        fun fromRaw(raw: String?): Cosmetic? {
            if (raw == null) return null
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) }
        }
    }
}
