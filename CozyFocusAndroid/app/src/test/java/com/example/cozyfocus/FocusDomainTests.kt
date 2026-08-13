package com.example.cozyfocus

import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FocusDomainTests {

    @Test
    fun companion_fromRaw_resolvesDefaultOrMatching() {
        assertEquals(CompanionAnimal.RED_PANDA, CompanionAnimal.fromRaw("redPanda"))
        assertEquals(CompanionAnimal.CAPYBARA, CompanionAnimal.fromRaw("capybara"))
        assertEquals(CompanionAnimal.RABBIT, CompanionAnimal.fromRaw("rabbit"))
        assertEquals(CompanionAnimal.PUPPY, CompanionAnimal.fromRaw("puppy"))
        assertEquals(CompanionAnimal.CAT, CompanionAnimal.fromRaw("cat"))
        assertEquals(CompanionAnimal.HORSE, CompanionAnimal.fromRaw("horse"))
        assertEquals(CompanionAnimal.RED_PANDA, CompanionAnimal.fromRaw("unknown_companion"))
    }

    @Test
    fun cosmetic_fromRaw_resolvesCorrectly() {
        assertEquals(Cosmetic.FLOWER_CROWN, Cosmetic.fromRaw("flowerCrown"))
        assertEquals(Cosmetic.SUN_HAT, Cosmetic.fromRaw("sunHat"))
        assertEquals(Cosmetic.BANDANA, Cosmetic.fromRaw("bandana"))
        assertEquals(Cosmetic.ROUND_GLASSES, Cosmetic.fromRaw("roundGlasses"))
        assertEquals(Cosmetic.BOW_TIE, Cosmetic.fromRaw("bowTie"))
        assertNull(Cosmetic.fromRaw(null))
        assertNull(Cosmetic.fromRaw("invalid"))
    }

    @Test
    fun companion_baseFrequencies_areValid() {
        CompanionAnimal.entries.forEach { companion ->
            assertNotNull(companion.displayName)
            assertNotNull(companion.symbol)
            assertEquals(true, companion.baseFrequencyHz > 100.0)
        }
    }
}
