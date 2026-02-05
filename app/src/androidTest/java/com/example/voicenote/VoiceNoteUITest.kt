package com.example.voicenote

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.voicenote.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for VoiceNote Application
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class VoiceNoteUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Test that the main activity loads correctly
     */
    @Test
    fun testMainActivityLoads() {
        onView(withId(R.id.root_layout)) // Assuming there's a root layout with this ID
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the recording button is displayed
     */
    @Test
    fun testRecordingButtonDisplayed() {
        onView(withContentDescription("Record"))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the recording button can be clicked
     */
    @Test
    fun testRecordingButtonClick() {
        onView(withContentDescription("Record"))
            .perform(click())
    }

    /**
     * Test that the AI status card is displayed
     */
    @Test
    fun testAiStatusCardDisplayed() {
        onView(withText("AI is idle · Ready to listen"))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the greeting text is displayed
     */
    @Test
    fun testGreetingTextDisplayed() {
        onView(withText("Good Morning, Alex Rivera"))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the recent notes header is displayed
     */
    @Test
    fun testRecentNotesHeaderDisplayed() {
        onView(withText("Recent Notes"))
            .check(matches(isDisplayed()))
    }

    /**
     * Test navigation to settings
     */
    @Test
    fun testNavigateToSettings() {
        onView(withContentDescription("Settings"))
            .perform(click())
    }

    /**
     * Test navigation to search
     */
    @Test
    fun testNavigateToSearch() {
        onView(withContentDescription("Search"))
            .perform(click())
    }
}