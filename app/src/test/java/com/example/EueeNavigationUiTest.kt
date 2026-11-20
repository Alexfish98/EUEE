package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EueeNavigationUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `dashboard renders and bottom navigation switches between primary sections`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_list").assertIsDisplayed()

        // Navigate to Study Library
        composeTestRule.onNodeWithTag("bottom_nav_study_library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("study_library_list").assertIsDisplayed()

        // Navigate to Mock Exams
        composeTestRule.onNodeWithTag("bottom_nav_exams").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mock_exams_screen").assertIsDisplayed()

        // Navigate to Notebook
        composeTestRule.onNodeWithTag("bottom_nav_notebook").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_note_btn").assertIsDisplayed()

        // Navigate to Daily Planner
        composeTestRule.onNodeWithTag("bottom_nav_planner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("planner_screen").assertIsDisplayed()
    }

    @Test
    fun `navigation drawer opens Scientific Tools and evaluates calculator expression`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_drawer_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_item_tools").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("scientific_tools_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sci_calc_eval_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sci_calc_result").performScrollTo().assertTextEquals("Result: 6.5")
    }

    @Test
    fun `navigation drawer opens Student Essentials and switches between countdown, score planner, and checklists`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_drawer_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_item_essentials").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("essentials_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("essentials_days_left").assertIsDisplayed()

        composeTestRule.onNodeWithTag("essentials_tab_score_planner").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("essentials_score_total").assertIsDisplayed()

        composeTestRule.onNodeWithTag("essentials_tab_checklists").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checklist_cb_kit_id").performScrollTo().assertIsDisplayed()
    }
}
