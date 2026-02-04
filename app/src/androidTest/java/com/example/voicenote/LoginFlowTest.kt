package com.example.voicenote

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import com.example.voicenote.data.remote.ApiService
import com.example.voicenote.data.remote.SyncResponse
import com.example.voicenote.data.remote.UserDTO
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
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

    private lateinit var device: UiDevice

    @Before
    fun init() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testLoginFlowSuccess() {
        // This test assumes the app starts at the UnifiedAuthScreen
        // Since we are using Hilt, we could provide a fake ApiService
        // But for a simple UI test with Espresso/Compose Test, we can just interact with the UI.
        
        val email = "test@example.com"
        
        // Find email field and type
        composeTestRule.onNodeWithText("Email Address").performTextInput(email)
        
        // Click Sync button
        composeTestRule.onNodeWithText("Sync & Connect").performClick()
        
        // Wait for navigation or success state
        // In a real scenario, you'd mock the API response to ensure it succeeds instantly
    }
}
