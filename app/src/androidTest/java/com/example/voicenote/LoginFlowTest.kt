package com.example.voicenote

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.remote.SyncResponse
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import javax.inject.Inject

@HiltAndroidTest
class LoginFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var securityManager: SecurityManager

    @Before
    fun init() {
        hiltRule.inject()
        // Ensure we start from a logged-out state for the login test
        every { securityManager.getUserEmail() } returns null
        every { securityManager.getSessionToken() } returns null
    }

    @Test
    fun testLoginFlowSuccess() {
        val email = "test@example.com"
        val fakeResponse = SyncResponse(
            accessToken = "fake_token",
            refreshToken = "fake_refresh",
            user = mockk(relaxed = true)
        )

        coEvery { apiService.syncUser(any()) } returns Response.success(fakeResponse)

        // Find email field and type
        composeTestRule.onNodeWithText("Email Address").performTextInput(email)
        
        // Click Sync button
        composeTestRule.onNodeWithText("Sync & Connect").performClick()
        
        // After successful sync, it should navigate to the main content
        // We can verify this by checking for a UI element on the Home screen
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Recent Intel").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Recent Intel").assertIsDisplayed()
        composeTestRule.onNodeWithText("test").assertIsDisplayed() // substringBefore("@")
    }

    @Test
    fun testLoginFlowFailure() {
        val email = "wrong@example.com"
        
        coEvery { apiService.syncUser(any()) } returns Response.error(403, okhttp3.ResponseBody.create(null, "Trial already claimed"))

        composeTestRule.onNodeWithText("Email Address").performTextInput(email)
        composeTestRule.onNodeWithText("Sync & Connect").performClick()

        // Check for error message (based on SyncUser failure mapping in Repository)
        composeTestRule.onNodeWithText("Trial already claimed: This device has already used its free package. Please upgrade to continue.").assertIsDisplayed()
    }
}
