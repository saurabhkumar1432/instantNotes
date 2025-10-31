package com.voicenotesai.data.repository

import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.ai.CategoryManager
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val notesDao: NotesDao,
    private val categoryManager: CategoryManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NotesRepository {
    
    override suspend fun saveNote(note: Note): Long = withContext(ioDispatcher) {
        // Auto-categorize the note if no category is set or if it's "General"
        val noteToSave = if (note.category.isBlank() || note.category == "General") {
            try {
                val userHistory = categoryManager.getCategoryUsageStats().first()
                val suggestion = categoryManager.categorizeNote(
                    content = note.content,
                    transcribedText = note.transcribedText ?: "",
                    userHistory = userHistory
                )
                
                // Only apply if confidence is reasonably high
                if (suggestion.confidence > 0.5f) {
                    // Record the usage for learning
                    categoryManager.recordCategoryUsage(
                        content = note.content,
                        selectedCategory = suggestion.category,
                        confidence = suggestion.confidence
                    )
                    
                    note.copy(category = suggestion.category.name)
                } else {
                    note
                }
            } catch (e: Exception) {
                // If categorization fails, just save the note as-is
                note
            }
        } else {
            note
        }
        
        return@withContext notesDao.insert(noteToSave)
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
    
    /**
     * Update the category of a note and record the usage for learning.
     */
    suspend fun updateNoteCategory(
        noteId: Long,
        category: String,
        noteContent: String = ""
    ) = withContext(ioDispatcher) {
        notesDao.updateNoteCategory(noteId, category)
        
        // Record usage for learning if we have the note content
        if (noteContent.isNotBlank()) {
            try {
                val contentCategory = com.voicenotesai.domain.ai.ContentCategory.valueOf(category)
                categoryManager.recordCategoryUsage(
                    content = noteContent,
                    selectedCategory = contentCategory,
                    confidence = 1.0f // User manually selected, so high confidence
                )
            } catch (e: IllegalArgumentException) {
                // Category not found in enum, skip learning
            }
        }
    }
}
