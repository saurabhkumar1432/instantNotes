package com.voicenotesai.data.voice

import android.content.Context
import com.voicenotesai.domain.model.VoiceCommand
import com.voicenotesai.domain.model.VoiceCommandResult
import com.voicenotesai.domain.usecase.VoiceCommandUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for VoiceCommandService.
 */
class VoiceCommandServiceTest {
    
    private lateinit var context: Context
    private lateinit var voiceCommandUseCase: VoiceCommandUseCase
    private lateinit var voiceCommandService: VoiceCommandService
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        voiceCommandUseCase = mockk(relaxed = true)
        voiceCommandService = VoiceCommandService(context, voiceCommandUseCase)
    }
    
    @Test
    fun `processTextCommand recognizes start recording command`() = runTest {
        // Given
        val phrase = "start recording"
        every { voiceCommandUseCase.processCommand(phrase) } returns VoiceCommandResult.Success
        
        // When
        val result = voiceCommandService.processTextCommand(phrase)
        
        // Then
        assertEquals(VoiceCommandResult.Success, result)
    }
    
    @Test
    fun `processTextCommand recognizes stop recording command`() = runTest {
        // Given
        val phrase = "stop recording"
        every { voiceCommandUseCase.processCommand(phrase) } returns VoiceCommandResult.Success
        
        // When
        val result = voiceCommandService.processTextCommand(phrase)
        
        // Then
        assertEquals(VoiceCommandResult.Success, result)
    }
    
    @Test
    fun `processTextCommand returns not recognized for unknown phrase`() = runTest {
        // Given
        val phrase = "unknown command"
        every { voiceCommandUseCase.processCommand(phrase) } returns VoiceCommandResult.NotRecognized
        
        // When
        val result = voiceCommandService.processTextCommand(phrase)
        
        // Then
        assertEquals(VoiceCommandResult.NotRecognized, result)
    }
    
    @Test
    fun `getAvailableCommands returns all voice commands`() {
        // When
        val commands = voiceCommandService.getAvailableCommands()
        
        // Then
        assertTrue(commands.containsKey(VoiceCommand.START_RECORDING))
        assertTrue(commands.containsKey(VoiceCommand.STOP_RECORDING))
        assertTrue(commands.containsKey(VoiceCommand.SAVE_NOTE))
        assertTrue(commands.containsKey(VoiceCommand.CREATE_REMINDER))
    }
    
    @Test
    fun `VoiceCommand fromPhrase recognizes start recording phrases`() {
        // Test various start recording phrases
        val phrases = listOf("start recording", "begin recording", "record note", "start note", "new recording")
        
        phrases.forEach { phrase ->
            val command = VoiceCommand.fromPhrase(phrase)
            assertEquals(VoiceCommand.START_RECORDING, command)
        }
    }
    
    @Test
    fun `VoiceCommand fromPhrase recognizes stop recording phrases`() {
        // Test various stop recording phrases
        val phrases = listOf("stop recording", "end recording", "finish recording", "stop note")
        
        phrases.forEach { phrase ->
            val command = VoiceCommand.fromPhrase(phrase)
            assertEquals(VoiceCommand.STOP_RECORDING, command)
        }
    }
    
    @Test
    fun `VoiceCommand fromPhrase returns null for unknown phrase`() {
        // Given
        val phrase = "unknown command"
        
        // When
        val command = VoiceCommand.fromPhrase(phrase)
        
        // Then
        assertNull(command)
    }
    
    @Test
    fun `VoiceCommand fromPhrase is case insensitive`() {
        // Test case insensitive matching
        val phrases = listOf("START RECORDING", "Start Recording", "start RECORDING")
        
        phrases.forEach { phrase ->
            val command = VoiceCommand.fromPhrase(phrase)
            assertEquals(VoiceCommand.START_RECORDING, command)
        }
    }
}