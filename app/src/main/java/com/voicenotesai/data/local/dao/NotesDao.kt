package com.voicenotesai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.voicenotesai.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Note] entities.
 * Provides database operations for managing voice notes.
 */
@Dao
interface NotesDao {
    /**
     * Inserts a new note into the database.
     * 
     * @param note The note to insert
     * @return The ID of the newly inserted note
     */
    @Insert
    suspend fun insert(note: Note): Long
    
    /**
     * Retrieves all notes ordered by timestamp (newest first).
     * 
     * @return Flow of notes list that updates automatically when data changes
     */
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>
    
    /**
     * Retrieves a specific note by its ID.
     * 
     * @param id The unique identifier of the note
     * @return The note if found, null otherwise
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Deletes a specific note from the database.
     * 
     * @param note The note entity to delete
     */
    @Delete
    suspend fun delete(note: Note)
    
    /**
     * Deletes a note by its ID.
     * 
     * @param id The unique identifier of the note to delete
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Deletes all notes from the database.
     * Use with caution as this operation cannot be undone.
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
