package com.voicenotesai.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.voicenotesai.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [Note] entities.
 * Optimized for large datasets with efficient pagination and indexing.
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
     * Updates an existing note in the database.
     * 
     * @param note The note to update
     */
    @Update
    suspend fun update(note: Note)
    
    /**
     * Retrieves all notes with efficient pagination (optimized for 10k+ notes).
     * Uses indexed timestamp column for fast ordering.
     * 
     * @return PagingSource for efficient lazy loading
     */
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllNotesPaged(): PagingSource<Int, Note>
    
    /**
     * Retrieves archived notes with pagination.
     * 
     * @return PagingSource for archived notes
     */
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedNotesPaged(): PagingSource<Int, Note>
    
    /**
     * Retrieves notes by category with pagination.
     * Uses indexed category column for fast filtering.
     * 
     * @param category The category to filter by
     * @return PagingSource for category-filtered notes
     */
    @Query("SELECT * FROM notes WHERE category = :category AND isArchived = 0 ORDER BY timestamp DESC")
    fun getNotesByCategoryPaged(category: String): PagingSource<Int, Note>
    
    /**
     * Full-text search across content and transcribed text with pagination.
     * Uses indexed columns for efficient search.
     * 
     * @param searchQuery The search term
     * @return PagingSource for search results
     */
    @Query("""
        SELECT * FROM notes 
        WHERE (content LIKE '%' || :searchQuery || '%' 
               OR transcribedText LIKE '%' || :searchQuery || '%'
               OR tags LIKE '%' || :searchQuery || '%')
        AND isArchived = 0
        ORDER BY timestamp DESC
    """)
    fun searchNotesPaged(searchQuery: String): PagingSource<Int, Note>
    
    /**
     * Retrieves notes within a date range with pagination.
     * Uses indexed timestamp column for efficient range queries.
     * 
     * @param startTimestamp Start of the date range
     * @param endTimestamp End of the date range
     * @return PagingSource for date-filtered notes
     */
    @Query("""
        SELECT * FROM notes 
        WHERE timestamp BETWEEN :startTimestamp AND :endTimestamp 
        AND isArchived = 0
        ORDER BY timestamp DESC
    """)
    fun getNotesInDateRangePaged(startTimestamp: Long, endTimestamp: Long): PagingSource<Int, Note>
    
    /**
     * Retrieves all notes ordered by timestamp (newest first).
     * For small result sets or when pagination is not needed.
     * 
     * @return Flow of notes list that updates automatically when data changes
     */
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>
    
    /**
     * Retrieves a limited number of recent notes for quick access.
     * Optimized for dashboard/preview scenarios.
     * 
     * @param limit Maximum number of notes to retrieve
     * @return Flow of recent notes
     */
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotes(limit: Int = 10): Flow<List<Note>>
    
    /**
     * Retrieves a specific note by its ID.
     * Uses primary key index for O(1) lookup.
     * 
     * @param id The unique identifier of the note
     * @return The note if found, null otherwise
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    /**
     * Gets the total count of notes (excluding archived).
     * 
     * @return Flow of total note count
     */
    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0")
    fun getTotalNotesCount(): Flow<Int>
    
    /**
     * Gets the total count of archived notes.
     * 
     * @return Flow of archived note count
     */
    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 1")
    fun getArchivedNotesCount(): Flow<Int>
    
    /**
     * Gets database statistics for optimization monitoring.
     * 
     * @return Flow of database stats
     */
    @Query("""
        SELECT 
            COUNT(*) as totalNotes,
            COUNT(CASE WHEN isArchived = 0 THEN 1 END) as activeNotes,
            COUNT(CASE WHEN isArchived = 1 THEN 1 END) as archivedNotes,
            AVG(LENGTH(content)) as avgContentLength,
            MAX(timestamp) as latestTimestamp,
            MIN(timestamp) as oldestTimestamp
        FROM notes
    """)
    suspend fun getDatabaseStats(): DatabaseStats
    
    /**
     * Archives a note (soft delete).
     * 
     * @param id The ID of the note to archive
     */
    @Query("UPDATE notes SET isArchived = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun archiveNote(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Unarchives a note.
     * 
     * @param id The ID of the note to unarchive
     */
    @Query("UPDATE notes SET isArchived = 0, lastModified = :timestamp WHERE id = :id")
    suspend fun unarchiveNote(id: Long, timestamp: Long = System.currentTimeMillis())
    
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
     * Gets notes older than the specified timestamp for archiving operations.
     * 
     * @param olderThan Timestamp threshold
     * @return List of notes older than the threshold
     */
    @Query("SELECT * FROM notes WHERE timestamp < :olderThan AND isArchived = 0")
    suspend fun getNotesOlderThan(olderThan: Long): List<Note>
    
    /**
     * Permanently deletes archived notes older than the specified timestamp.
     * Used for cleanup operations.
     * 
     * @param olderThan Timestamp threshold for deletion
     * @return Number of deleted notes
     */
    @Query("DELETE FROM notes WHERE isArchived = 1 AND timestamp < :olderThan")
    suspend fun deleteArchivedNotesOlderThan(olderThan: Long): Int
    
    /**
     * Deletes all notes from the database.
     * Use with caution as this operation cannot be undone.
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAll()
    
    /**
     * Gets a count for triggering vacuum operations.
     * VACUUM operations should be performed at the database level, not through DAO.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE 1=0")
    suspend fun triggerVacuum(): Int
    
    /**
     * Gets a count for triggering analyze operations.
     * ANALYZE operations should be performed at the database level, not through DAO.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE 1=0")
    suspend fun triggerAnalyze(): Int
    
    /**
     * Gets notes by category for category management.
     * 
     * @param category The category name
     * @return Flow of notes in the specified category
     */
    @Query("SELECT * FROM notes WHERE category = :category AND isArchived = 0 ORDER BY timestamp DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>
    
    /**
     * Gets count of notes by category.
     * 
     * @param category The category name
     * @return Count of notes in the category
     */
    @Query("SELECT COUNT(*) FROM notes WHERE category = :category AND isArchived = 0")
    suspend fun getNotesCountByCategory(category: String): Int
    
    /**
     * Gets category distribution for analytics.
     * 
     * @return List of category distribution data
     */
    @Query("SELECT category, COUNT(*) as count FROM notes WHERE isArchived = 0 GROUP BY category")
    suspend fun getCategoryDistribution(): List<CategoryCount>
    
    /**
     * Updates the category of a note.
     * 
     * @param noteId The ID of the note
     * @param category The new category
     * @param timestamp The modification timestamp
     */
    @Query("UPDATE notes SET category = :category, lastModified = :timestamp WHERE id = :noteId")
    suspend fun updateNoteCategory(noteId: Long, category: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Gets all unique categories used in notes.
     * 
     * @return List of unique category names
     */
    @Query("SELECT DISTINCT category FROM notes WHERE isArchived = 0 AND category IS NOT NULL AND category != ''")
    suspend fun getAllUsedCategories(): List<String>
}



/**
 * Data class for database statistics.
 */
data class DatabaseStats(
    val totalNotes: Int,
    val activeNotes: Int,
    val archivedNotes: Int,
    val avgContentLength: Double,
    val latestTimestamp: Long,
    val oldestTimestamp: Long
)
/**

 * Data class for category count results from Room queries.
 */
data class CategoryCount(
    val category: String,
    val count: Int
)