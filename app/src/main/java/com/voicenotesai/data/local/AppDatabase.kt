package com.voicenotesai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note

/**
 * Room database for storing voice notes locally.
 * 
 * When adding new entities or modifying existing ones:
 * 1. Increment the version number
 * 2. Add a new Migration object in [DatabaseMigrations]
 * 3. Update the [DatabaseModule] to include the new migration
 */
@Database(
    entities = [Note::class],
    version = 1,
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
     * Example migration from version 1 to 2.
     * Currently not used, but prepared for future schema changes.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example: Add a new column
            // database.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT")
        }
    }
}
