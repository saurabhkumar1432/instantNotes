package com.voicenotesai.data.repository

import com.voicenotesai.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    suspend fun saveNote(note: Note): Long
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun deleteNote(id: Long)
    suspend fun deleteAllNotes()
}
