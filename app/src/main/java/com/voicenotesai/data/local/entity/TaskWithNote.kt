package com.voicenotesai.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class representing a TaskEntity with its associated Note.
 * This relation allows efficient querying of tasks along with their source notes.
 * 
 * The relation is established through the sourceNoteId field in TaskEntity
 * and the id field in Note.
 */
data class TaskWithNote(
    @Embedded 
    val task: TaskEntity,
    
    @Relation(
        parentColumn = "sourceNoteId",
        entityColumn = "id"
    )
    val sourceNote: Note?
)