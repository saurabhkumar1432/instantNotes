package com.voicenotesai.domain.cache

import com.voicenotesai.data.local.entity.Note
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Cacheable wrapper for Note entities with compression support
 */
data class CacheableNote(
    val note: Note,
    override val size: Long = calculateSize(note),
    override val lastAccessed: Long = System.currentTimeMillis(),
    override val accessCount: Int = 1
) : CacheableData {

    override fun compress(): ByteArray {
        val noteData = "${note.id}|${note.content}|${note.timestamp}|${note.transcribedText ?: ""}"
        
        return ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzos ->
                gzos.write(noteData.toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }
    }

    override fun decompress(data: ByteArray): CacheableData {
        val decompressedData = ByteArrayInputStream(data).use { bais ->
            GZIPInputStream(bais).use { gzis ->
                gzis.readBytes().toString(Charsets.UTF_8)
            }
        }
        
        val parts = decompressedData.split("|")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid compressed note data format")
        }
        
        val note = Note(
            id = parts[0].toLong(),
            content = parts[1],
            timestamp = parts[2].toLong(),
            transcribedText = parts[3].takeIf { it.isNotEmpty() }
        )
        
        return CacheableNote(
            note = note,
            lastAccessed = System.currentTimeMillis(),
            accessCount = accessCount + 1
        )
    }

    companion object {
        private fun calculateSize(note: Note): Long {
            return (note.content.length + (note.transcribedText?.length ?: 0)) * 2L // UTF-16 chars
        }
    }
}

/**
 * Cacheable wrapper for search results
 */
data class CacheableSearchResults(
    val query: String,
    val results: List<Note>,
    val totalCount: Int,
    override val size: Long = calculateSize(results),
    override val lastAccessed: Long = System.currentTimeMillis(),
    override val accessCount: Int = 1
) : CacheableData {

    override fun compress(): ByteArray {
        val searchData = buildString {
            append(query)
            append("|")
            append(totalCount)
            append("|")
            results.forEach { note ->
                append("${note.id},${note.content},${note.timestamp},${note.transcribedText ?: ""};")
            }
        }
        
        return ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzos ->
                gzos.write(searchData.toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }
    }

    override fun decompress(data: ByteArray): CacheableData {
        val decompressedData = ByteArrayInputStream(data).use { bais ->
            GZIPInputStream(bais).use { gzis ->
                gzis.readBytes().toString(Charsets.UTF_8)
            }
        }
        
        val mainParts = decompressedData.split("|", limit = 3)
        if (mainParts.size != 3) {
            throw IllegalArgumentException("Invalid compressed search results format")
        }
        
        val query = mainParts[0]
        val totalCount = mainParts[1].toInt()
        val notesData = mainParts[2]
        
        val notes = if (notesData.isNotEmpty()) {
            notesData.split(";").filter { it.isNotEmpty() }.map { noteStr ->
                val parts = noteStr.split(",", limit = 4)
                Note(
                    id = parts[0].toLong(),
                    content = parts[1],
                    timestamp = parts[2].toLong(),
                    transcribedText = parts[3].takeIf { it.isNotEmpty() }
                )
            }
        } else {
            emptyList()
        }
        
        return CacheableSearchResults(
            query = query,
            results = notes,
            totalCount = totalCount,
            lastAccessed = System.currentTimeMillis(),
            accessCount = accessCount + 1
        )
    }

    companion object {
        private fun calculateSize(results: List<Note>): Long {
            return results.sumOf { note ->
                (note.content.length + (note.transcribedText?.length ?: 0)) * 2L
            }
        }
    }
}

/**
 * Cacheable wrapper for AI processing results
 */
data class CacheableAIResult(
    val inputHash: String,
    val result: String,
    val processingTime: Long,
    val model: String,
    override val size: Long = result.length * 2L,
    override val lastAccessed: Long = System.currentTimeMillis(),
    override val accessCount: Int = 1
) : CacheableData {

    override fun compress(): ByteArray {
        val aiData = "$inputHash|$result|$processingTime|$model"
        
        return ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzos ->
                gzos.write(aiData.toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }
    }

    override fun decompress(data: ByteArray): CacheableData {
        val decompressedData = ByteArrayInputStream(data).use { bais ->
            GZIPInputStream(bais).use { gzis ->
                gzis.readBytes().toString(Charsets.UTF_8)
            }
        }
        
        val parts = decompressedData.split("|", limit = 4)
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid compressed AI result format")
        }
        
        return CacheableAIResult(
            inputHash = parts[0],
            result = parts[1],
            processingTime = parts[2].toLong(),
            model = parts[3],
            lastAccessed = System.currentTimeMillis(),
            accessCount = accessCount + 1
        )
    }
}