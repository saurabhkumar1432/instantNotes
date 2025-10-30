package com.voicenotesai.data.repository

import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.cache.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Cache-aware wrapper for NotesRepository that provides intelligent caching
 * and preloading for optimal performance
 */
@Singleton
class CachedNotesRepository @Inject constructor(
    private val notesRepository: NotesRepository,
    private val cacheManager: CacheManager,
    private val preloadingStrategy: PreloadingStrategy
) : NotesRepository {

    companion object {
        private val NOTE_CACHE_POLICY = CachePolicy(
            ttl = 24.hours,
            maxSize = 1024 * 1024, // 1MB per note max
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true,
            priority = CachePriority.HIGH
        )
        
        private val SEARCH_CACHE_POLICY = CachePolicy(
            ttl = 30.minutes,
            maxSize = 512 * 1024, // 512KB per search result
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true,
            priority = CachePriority.NORMAL
        )
    }

    override suspend fun saveNote(note: Note): Long {
        val noteId = notesRepository.saveNote(note)
        
        // Cache the saved note
        val savedNote = note.copy(id = noteId)
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, noteId.toString())
        val cacheableNote = CacheableNote(savedNote)
        
        cacheManager.cache(cacheKey, cacheableNote, NOTE_CACHE_POLICY)
        
        // Invalidate related caches
        cacheManager.invalidate(CachePattern.ByType(CacheType.SEARCH_RESULTS))
        
        return noteId
    }

    override fun getAllNotes(): Flow<List<Note>> {
        return notesRepository.getAllNotes().map { notes ->
            // Cache frequently accessed notes
            preloadFrequentlyAccessedNotes(notes)
            notes
        }
    }

    override suspend fun getNoteById(id: Long): Note? {
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, id.toString())
        
        // Try to get from cache first
        val cachedData = cacheManager.retrieve(cacheKey)
        if (cachedData is CacheableNote) {
            return cachedData.note
        }
        
        // Cache miss - load from repository
        val note = notesRepository.getNoteById(id)
        
        // Cache the result if found
        note?.let {
            val cacheableNote = CacheableNote(it)
            cacheManager.cache(cacheKey, cacheableNote, NOTE_CACHE_POLICY)
        }
        
        return note
    }

    override suspend fun deleteNote(id: Long) {
        notesRepository.deleteNote(id)
        
        // Remove from cache
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, id.toString())
        cacheManager.invalidate(CachePattern.ByPrefix(cacheKey.toStringKey()))
        
        // Invalidate search results
        cacheManager.invalidate(CachePattern.ByType(CacheType.SEARCH_RESULTS))
    }

    override suspend fun deleteAllNotes() {
        notesRepository.deleteAllNotes()
        
        // Clear all note-related caches
        cacheManager.invalidate(CachePattern.ByType(CacheType.NOTE_CONTENT))
        cacheManager.invalidate(CachePattern.ByType(CacheType.NOTE_METADATA))
        cacheManager.invalidate(CachePattern.ByType(CacheType.SEARCH_RESULTS))
    }

    /**
     * Search notes with caching support
     */
    suspend fun searchNotes(query: String): List<Note> {
        val cacheKey = CacheKey(CacheType.SEARCH_RESULTS, query.hashCode().toString())
        
        // Try cache first
        val cachedResults = cacheManager.retrieve(cacheKey)
        if (cachedResults is CacheableSearchResults) {
            return cachedResults.results
        }
        
        // Perform search (this would be implemented in the actual repository)
        val results = performSearch(query)
        
        // Cache the results
        val cacheableResults = CacheableSearchResults(
            query = query,
            results = results,
            totalCount = results.size
        )
        cacheManager.cache(cacheKey, cacheableResults, SEARCH_CACHE_POLICY)
        
        return results
    }

    /**
     * Get notes with intelligent preloading
     */
    suspend fun getNotesWithPreloading(context: PreloadContext): List<Note> {
        // Get preloading recommendations
        val recommendations = preloadingStrategy.getRecommendations(context)
        
        // Preload high-priority items
        val highPriorityKeys = recommendations
            .filter { it.priority >= PreloadPriority.HIGH }
            .map { it.key }
        
        if (highPriorityKeys.isNotEmpty()) {
            cacheManager.preload(highPriorityKeys)
        }
        
        // Return all notes
        return notesRepository.getAllNotes().map { notes ->
            notes
        }.let { _ ->
            // Convert flow to list for this example
            // In real implementation, you'd handle this differently
            emptyList()
        }
    }

    /**
     * Optimize cache performance
     */
    suspend fun optimizeCache(): OptimizationResult {
        return cacheManager.optimize()
    }

    /**
     * Get cache performance metrics
     */
    fun getCacheMetrics(): Flow<CacheMetrics> {
        return cacheManager.getMetrics()
    }

    private suspend fun preloadFrequentlyAccessedNotes(notes: List<Note>) {
        // Get preload candidates
        val candidates = preloadingStrategy.getPreloadCandidates()
        
        // Filter to notes that exist
        val existingCandidates = candidates.filter { key ->
            key.type == CacheType.NOTE_CONTENT && 
            notes.any { it.id.toString() == key.identifier }
        }
        
        // Preload in background
        if (existingCandidates.isNotEmpty()) {
            cacheManager.preload(existingCandidates)
        }
    }

    private suspend fun performSearch(query: String): List<Note> {
        // This is a placeholder implementation
        // In a real app, this would perform full-text search
        return notesRepository.getAllNotes().map { notes ->
            notes.filter { note ->
                note.content.contains(query, ignoreCase = true) ||
                note.transcribedText?.contains(query, ignoreCase = true) == true
            }
        }.let { _ ->
            // Convert flow to list for this example
            emptyList()
        }
    }
}