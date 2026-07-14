package com.abdessalem.worktracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.abdessalem.worktracker.ui.theme.WorkTrackerTheme
import org.junit.Rule
import org.junit.Test

class ActionFeedbackOverlayTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun clockInOverlayShowsConfirmation() {
        composeRule.mainClock.autoAdvance = true
        composeRule.setContent { WorkTrackerTheme(darkTheme = true, dynamicColor = false) { ActionFeedbackOverlay(feedback = ActionFeedback.clockIn(100f, 100f, "08:00"), onFinished = {}) } }
        composeRule.waitUntil(timeoutMillis = 2_000) { composeRule.onAllNodesWithText("Clocked In").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Clocked In").assertIsDisplayed()
    }
}
