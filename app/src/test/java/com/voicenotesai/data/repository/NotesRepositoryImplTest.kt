package com.voicenotesai.data.repository

import app.cash.turbine.test
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NotesRepositoryImplTest {

    private lateinit var notesDao: NotesDao
    private lateinit var repository: NotesRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        notesDao = mockk(relaxed = true)
        repository = NotesRepositoryImpl(notesDao, testDispatcher)
    }

    // Note Insertion Tests
    @Test
    fun `saveNote should insert note and return generated id`() = runTest(testDispatcher) {
        // Given
        val note = Note(
            id = 0,
            content = "Test note content",
            timestamp = 1234567890L,
            transcribedText = "Test transcription"
        )
        val expectedId = 1L
        coEvery { notesDao.insert(note) } returns expectedId

        // When
        val result = repository.saveNote(note)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(expectedId, result)
        coVerify { notesDao.insert(note) }
    }

    @Test
    fun `saveNote should handle note without transcribed text`() = runTest(testDispatcher) {
        // Given
        val note = Note(
            id = 0,
            content = "Note without transcription",
            timestamp = 1234567890L,
            transcribedText = null
        )
        val expectedId = 2L
        coEvery { notesDao.insert(note) } returns expectedId

        // When
        val result = repository.saveNote(note)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(expectedId, result)
        coVerify { notesDao.insert(note) }
    }

    // Note Retrieval Tests
    @Test
    fun `getAllNotes should return flow of all notes`() = runTest(testDispatcher) {
        // Given
        val notes = listOf(
            Note(id = 1, content = "Note 1", timestamp = 1000L),
            Note(id = 2, content = "Note 2", timestamp = 2000L),
            Note(id = 3, content = "Note 3", timestamp = 3000L)
        )
        every { notesDao.getAllNotes() } returns flowOf(notes)

        // When & Then
        repository.getAllNotes().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals("Note 1", result[0].content)
            assertEquals("Note 2", result[1].content)
            assertEquals("Note 3", result[2].content)
            awaitComplete()
        }
    }

    @Test
    fun `getAllNotes should return empty list when no notes exist`() = runTest(testDispatcher) {
        // Given
        every { notesDao.getAllNotes() } returns flowOf(emptyList())

        // When & Then
        repository.getAllNotes().test {
            val result = awaitItem()
            assertEquals(0, result.size)
            awaitComplete()
        }
    }

    @Test
    fun `getNoteById should return note when it exists`() = runTest(testDispatcher) {
        // Given
        val noteId = 1L
        val expectedNote = Note(
            id = noteId,
            content = "Test note",
            timestamp = 1234567890L,
            transcribedText = "Test transcription"
        )
        coEvery { notesDao.getNoteById(noteId) } returns expectedNote

        // When
        val result = repository.getNoteById(noteId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(expectedNote, result)
        coVerify { notesDao.getNoteById(noteId) }
    }

    @Test
    fun `getNoteById should return null when note does not exist`() = runTest(testDispatcher) {
        // Given
        val noteId = 999L
        coEvery { notesDao.getNoteById(noteId) } returns null

        // When
        val result = repository.getNoteById(noteId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(result)
        coVerify { notesDao.getNoteById(noteId) }
    }

    // Note Deletion Tests
    @Test
    fun `deleteNote should call dao deleteById with correct id`() = runTest(testDispatcher) {
        // Given
        val noteId = 1L
        coEvery { notesDao.deleteById(noteId) } returns Unit

        // When
        repository.deleteNote(noteId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { notesDao.deleteById(noteId) }
    }

    @Test
    fun `deleteNote should handle deletion of non-existent note`() = runTest(testDispatcher) {
        // Given
        val noteId = 999L
        coEvery { notesDao.deleteById(noteId) } returns Unit

        // When
        repository.deleteNote(noteId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { notesDao.deleteById(noteId) }
    }

    @Test
    fun `deleteAllNotes should call dao deleteAll`() = runTest(testDispatcher) {
        // Given
        coEvery { notesDao.deleteAll() } returns Unit

        // When
        repository.deleteAllNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { notesDao.deleteAll() }
    }

    @Test
    fun `deleteAllNotes should work when no notes exist`() = runTest(testDispatcher) {
        // Given
        coEvery { notesDao.deleteAll() } returns Unit

        // When
        repository.deleteAllNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { notesDao.deleteAll() }
    }

    // Integration-style tests
    @Test
    fun `should handle multiple note insertions`() = runTest(testDispatcher) {
        // Given
        val note1 = Note(id = 0, content = "Note 1", timestamp = 1000L)
        val note2 = Note(id = 0, content = "Note 2", timestamp = 2000L)
        coEvery { notesDao.insert(note1) } returns 1L
        coEvery { notesDao.insert(note2) } returns 2L

        // When
        val id1 = repository.saveNote(note1)
        val id2 = repository.saveNote(note2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1L, id1)
        assertEquals(2L, id2)
        coVerify(exactly = 1) { notesDao.insert(note1) }
        coVerify(exactly = 1) { notesDao.insert(note2) }
    }

    @Test
    fun `should handle note retrieval after insertion`() = runTest(testDispatcher) {
        // Given
        val note = Note(id = 0, content = "Test note", timestamp = 1000L)
        val savedNote = note.copy(id = 1L)
        coEvery { notesDao.insert(note) } returns 1L
        coEvery { notesDao.getNoteById(1L) } returns savedNote

        // When
        val insertedId = repository.saveNote(note)
        val retrievedNote = repository.getNoteById(insertedId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1L, insertedId)
        assertEquals(savedNote, retrievedNote)
    }

    @Test
    fun `should handle note deletion after insertion`() = runTest(testDispatcher) {
        // Given
        val noteId = 1L
        coEvery { notesDao.deleteById(noteId) } returns Unit
        coEvery { notesDao.getNoteById(noteId) } returns null

        // When
        repository.deleteNote(noteId)
        val retrievedNote = repository.getNoteById(noteId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(retrievedNote)
        coVerify { notesDao.deleteById(noteId) }
    }
}
