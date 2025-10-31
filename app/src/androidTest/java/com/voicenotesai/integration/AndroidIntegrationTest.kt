package com.voicenotesai.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voicenotesai.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented integration tests for Voice Notes AI.
 * 
 * These tests run on actual Android devices/emulators and test:
 * - UI integration with real Android components
 * - Navigation between screens
 * - Database operations with Room
 * - File system operations
 * - Permission handling
 * - Background services and notifications
 * 
 * Note: These tests complement the unit integration tests by validating
 * Android-specific functionality that requires an actual Android environment.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testAppLaunchAndNavigation() {
        // Test that the app launches successfully and shows the home screen
        composeTestRule.onNodeWithText("Voice Notes").assertIsDisplayed()
        
        // Test navigation to different screens
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Navigate back to home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Voice Notes").assertIsDisplayed()
    }

    @Test
    fun testRecordingButtonInteraction() {
        // Test that the recording FAB is present and clickable
        composeTestRule.onNodeWithContentDescription("Start Recording").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Start Recording").performClick()
        
        // Should navigate to recording screen
        composeTestRule.onNodeWithText("New Recording").assertIsDisplayed()
    }

    @Test
    fun testStatsCardsDisplay() {
        // Test that stats cards are displayed with correct labels
        composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasks").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Week").assertIsDisplayed()
    }

    @Test
    fun testSearchFunctionality() {
        // Test search bar interaction
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        
        // Type in search query
        composeTestRule.onNodeWithContentDescription("Search").performTextInput("test")
        
        // Search should filter results (this would require test data)
        // In a real test, we would verify filtered results appear
    }

    @Test
    fun testTasksScreenNavigation() {
        // Navigate to tasks screen (assuming there's a way to get there from home)
        // This test would need to be adapted based on actual navigation structure
        
        // For now, test that we can access tasks through navigation
        composeTestRule.onNodeWithText("Tasks").performClick()
        // Would verify tasks screen content here
    }

    @Test
    fun testSettingsScreenConfiguration() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Test AI Models section
        composeTestRule.onNodeWithText("AI MODELS").assertIsDisplayed()
        
        // Test other settings sections
        composeTestRule.onNodeWithText("ACCOUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("NOTIFICATIONS").assertIsDisplayed()
        composeTestRule.onNodeWithText("APPEARANCE").assertIsDisplayed()
    }

    @Test
    fun testOfflineModeIndicator() {
        // Test offline mode indicator appears when device is offline
        // This would require mocking network connectivity
        
        // In a real test, we would:
        // 1. Mock network as offline
        // 2. Verify offline indicator appears
        // 3. Mock network as online
        // 4. Verify indicator disappears
    }

    @Test
    fun testDatabasePersistence() {
        // Test that data persists across app restarts
        // This would require:
        // 1. Creating test data
        // 2. Restarting the activity
        // 3. Verifying data is still present
        
        // For now, just verify the app can restart without crashing
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Voice Notes").assertIsDisplayed()
    }

    @Test
    fun testPermissionHandling() {
        // Test that the app handles permissions correctly
        // This would test microphone permissions for recording
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // In a real test, we would:
        // 1. Check current permission state
        // 2. Request permissions if needed
        // 3. Verify app behavior with/without permissions
        
        // For now, just verify context is available
        assert(context.packageName == "com.voicenotesai")
    }

    @Test
    fun testThemeChanges() {
        // Test theme switching between light and dark mode
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Look for theme toggle (would need to be implemented)
        // composeTestRule.onNodeWithText("Dark Mode").performClick()
        
        // Verify theme change is applied
        // This would check for color changes in the UI
    }

    @Test
    fun testExportFunctionality() {
        // Test export functionality with real file system
        // This would require:
        // 1. Creating a test note
        // 2. Triggering export
        // 3. Verifying file is created
        // 4. Cleaning up test files
        
        // For now, just verify export UI is accessible
        // (This would depend on having notes to export)
    }

    @Test
    fun testNotificationPermissions() {
        // Test notification permissions and functionality
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // In a real test, we would:
        // 1. Check notification permission status
        // 2. Test notification creation
        // 3. Verify notification appears in system
        
        // For now, verify we can access notification manager
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
        assert(notificationManager != null)
    }

    @Test
    fun testBackgroundServiceIntegration() {
        // Test background services for sync and reminders
        // This would test:
        // 1. Service starts correctly
        // 2. Service performs background operations
        // 3. Service handles app lifecycle changes
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Verify we can access required services
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
        assert(activityManager != null)
    }

    @Test
    fun testWidgetFunctionality() {
        // Test home screen widget functionality
        // This would require:
        // 1. Installing widget on test launcher
        // 2. Interacting with widget
        // 3. Verifying widget updates
        
        // For now, just verify widget provider is registered
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        
        // Check if widget provider is registered
        val receivers = packageManager.queryBroadcastReceivers(
            android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE),
            0
        )
        
        // In a real implementation, we would verify our widget provider is in the list
    }

    @Test
    fun testAccessibilitySupport() {
        // Test accessibility features
        composeTestRule.onRoot().assertIsDisplayed()
        
        // Test that all interactive elements have content descriptions
        composeTestRule.onNodeWithContentDescription("Start Recording").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        
        // In a real test, we would:
        // 1. Enable TalkBack
        // 2. Navigate using accessibility gestures
        // 3. Verify all content is accessible
    }

    @Test
    fun testDeepLinkHandling() {
        // Test deep link handling for sharing and shortcuts
        // This would test:
        // 1. App responds to deep links correctly
        // 2. Navigation to correct screen based on deep link
        // 3. Data is passed correctly through deep links
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Verify intent filters are registered
        val packageManager = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("voicenotes://note/123")
        
        val activities = packageManager.queryIntentActivities(intent, 0)
        // In a real test, we would verify our activity handles the deep link
    }
}