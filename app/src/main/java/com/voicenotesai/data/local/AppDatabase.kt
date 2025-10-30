package com.voicenotesai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note

/**
 * Room database for storing voice notes locally.
 * Optimized for large datasets with proper indexing and performance features.
 * 
 * When adding new entities or modifying existing ones:
 * 1. Increment the version number
 * 2. Add a new Migration object in [DatabaseMigrations]
 * 3. Update the [DatabaseModule] to include the new migration
 */
@Database(
    entities = [Note::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}

/**
 * Database migration definitions.
 * Each migration handles schema changes between versions.
 */
object DatabaseMigrations {
    /**
     * Migration from version 1 to 2.
     * Adds new columns and indexes for enhanced performance with large datasets.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns
            database.execSQL("ALTER TABLE notes ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
            database.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN audioFingerprint TEXT")
            database.execSQL("ALTER TABLE notes ADD COLUMN language TEXT")
            database.execSQL("ALTER TABLE notes ADD COLUMN wordCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
            
            // Update lastModified to match timestamp for existing records
            database.execSQL("UPDATE notes SET lastModified = timestamp WHERE lastModified = 0")
            
            // Create indexes for performance optimization
            database.execSQL("CREATE INDEX idx_notes_timestamp ON notes(timestamp)")
            database.execSQL("CREATE INDEX idx_notes_content ON notes(content)")
            database.execSQL("CREATE INDEX idx_notes_transcribed_text ON notes(transcribedText)")
            database.execSQL("CREATE INDEX idx_notes_archived ON notes(isArchived)")
            database.execSQL("CREATE INDEX idx_notes_category ON notes(category)")
            database.execSQL("CREATE INDEX idx_notes_timestamp_archived ON notes(timestamp, isArchived)")
        }
    }
}
