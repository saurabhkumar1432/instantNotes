package com.voicenotesai.data.repository

import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val notesDao: NotesDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NotesRepository {
    
    override suspend fun saveNote(note: Note): Long = withContext(ioDispatcher) {
        return@withContext notesDao.insert(note)
    }
    
    override fun getAllNotes(): Flow<List<Note>> {
        return notesDao.getAllNotes().flowOn(ioDispatcher)
    }
    
    override suspend fun getNoteById(id: Long): Note? = withContext(ioDispatcher) {
        return@withContext notesDao.getNoteById(id)
    }
    
    override suspend fun deleteNote(id: Long) = withContext(ioDispatcher) {
        notesDao.deleteById(id)
    }
    
    override suspend fun deleteAllNotes() = withContext(ioDispatcher) {
        notesDao.deleteAll()
    }
}
