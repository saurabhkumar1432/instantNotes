package com.voicenotesai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.model.AISettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    private val keyProvider = stringPreferencesKey("ai_provider")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyModel = stringPreferencesKey("model")

    @Before
    fun setup() {
        dataStore = mockk(relaxed = true)
        repository = SettingsRepositoryImpl(dataStore, testDispatcher)
    }

    @Test
    fun `saveSettings should store all settings in DataStore`() = runTest(testDispatcher) {
        // Given
        val settings = AISettings(
            provider = AIProvider.OPENAI,
            apiKey = "test-api-key",
            model = "gpt-4"
        )
        val preferencesSlot = slot<suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit>()

        coEvery { dataStore.edit(capture(preferencesSlot)) } coAnswers {
            val mutablePrefs = mockk<androidx.datastore.preferences.core.MutablePreferences>(relaxed = true)
            preferencesSlot.captured.invoke(mutablePrefs)
            mockk<Preferences>(relaxed = true)
        }

        // When
        repository.saveSettings(settings)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `getSettings should return AISettings when all fields are present`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "test-api-key",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(AIProvider.OPENAI, settings?.provider)
            assertEquals("test-api-key", settings?.apiKey)
            assertEquals("gpt-4", settings?.model)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings should return null when provider is missing`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyApiKey to "test-api-key",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        repository.getSettings().test {
            val settings = awaitItem()
            assertNull(settings)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings should return null when apiKey is missing`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        repository.getSettings().test {
            val settings = awaitItem()
            assertNull(settings)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings should return null when model is missing`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "test-api-key"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        repository.getSettings().test {
            val settings = awaitItem()
            assertNull(settings)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings should return null when no settings are stored`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf()
        every { dataStore.data } returns flowOf(preferences)

        // When & Then
        repository.getSettings().test {
            val settings = awaitItem()
            assertNull(settings)
            awaitComplete()
        }
    }

    @Test
    fun `getSettings should handle different AI providers correctly`() = runTest(testDispatcher) {
        // Test ANTHROPIC
        val anthropicPrefs = preferencesOf(
            keyProvider to "ANTHROPIC",
            keyApiKey to "anthropic-key",
            keyModel to "claude-3"
        )
        every { dataStore.data } returns flowOf(anthropicPrefs)

        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(AIProvider.ANTHROPIC, settings?.provider)
            awaitComplete()
        }

        // Test GOOGLE_AI
        val googlePrefs = preferencesOf(
            keyProvider to "GOOGLE_AI",
            keyApiKey to "google-key",
            keyModel to "gemini-pro"
        )
        every { dataStore.data } returns flowOf(googlePrefs)

        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals(AIProvider.GOOGLE_AI, settings?.provider)
            awaitComplete()
        }
    }

    @Test
    fun `hasValidSettings should return true when all fields are valid`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "test-api-key",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasValidSettings should return false when settings are null`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf()
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidSettings should return false when apiKey is blank`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidSettings should return false when model is blank`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "test-api-key",
            keyModel to ""
        )
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidSettings should return false when apiKey is whitespace only`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "   ",
            keyModel to "gpt-4"
        )
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasValidSettings should return false when model is whitespace only`() = runTest(testDispatcher) {
        // Given
        val preferences = preferencesOf(
            keyProvider to "OPENAI",
            keyApiKey to "test-api-key",
            keyModel to "   "
        )
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.hasValidSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }
}
