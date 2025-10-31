package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.ShareableLink
import kotlinx.coroutines.flow.Flow

/**
 * DAO for shareable links
 */
@Dao
interface ShareableLinkDao {
    
    @Query("SELECT * FROM shareable_links WHERE id = :linkId")
    suspend fun getLinkById(linkId: String): ShareableLink?
    
    @Query("SELECT * FROM shareable_links WHERE noteId = :noteId AND isActive = 1")
    suspend fun getLinksForNote(noteId: String): List<ShareableLink>
    
    @Query("SELECT * FROM shareable_links WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveLinks(): Flow<List<ShareableLink>>
    
    @Query("SELECT * FROM shareable_links WHERE createdByUserId = :userId AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getUserLinks(userId: String): List<ShareableLink>
    
    @Query("SELECT * FROM shareable_links WHERE expiresAt < :currentTime AND isActive = 1")
    suspend fun getExpiredLinks(currentTime: Long): List<ShareableLink>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: ShareableLink)
    
    @Update
    suspend fun updateLink(link: ShareableLink)
    
    @Query("UPDATE shareable_links SET accessCount = accessCount + 1, lastAccessedAt = :accessTime WHERE id = :linkId")
    suspend fun incrementAccessCount(linkId: String, accessTime: Long)
    
    @Query("UPDATE shareable_links SET isActive = 0 WHERE id = :linkId")
    suspend fun deactivateLink(linkId: String)
    
    @Query("UPDATE shareable_links SET isActive = 0 WHERE expiresAt < :currentTime")
    suspend fun deactivateExpiredLinks(currentTime: Long): Int
    
    @Query("DELETE FROM shareable_links WHERE expiresAt < :cutoffTime AND isActive = 0")
    suspend fun deleteOldInactiveLinks(cutoffTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM shareable_links WHERE noteId = :noteId AND isActive = 1")
    suspend fun getActiveLinkCountForNote(noteId: String): Int
    
    @Query("SELECT * FROM shareable_links WHERE shareUrl = :shareUrl")
    suspend fun getLinkByUrl(shareUrl: String): ShareableLink?
    
    @Delete
    suspend fun deleteLink(link: ShareableLink)
    
    @Query("DELETE FROM shareable_links WHERE noteId = :noteId")
    suspend fun deleteLinksForNote(noteId: String)
}