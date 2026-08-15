package com.cozyfocus.app.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cozyfocus.app.MainActivity
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun focusScreenShowsCoreControls() {
        composeTestRule.onNodeWithText("Focus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose your time").assertIsDisplayed()
        composeTestRule.onNodeWithText("Journey").assertIsDisplayed()
        composeTestRule.onNodeWithText("Den").assertIsDisplayed()
    }
}
