package com.voicenotesai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.dao.AnalyticsDao
import com.voicenotesai.data.local.dao.TaskDao
import com.voicenotesai.data.local.dao.ReminderDao
import com.voicenotesai.data.local.dao.CategoryUsageDao
import com.voicenotesai.data.local.dao.CustomCategoryDao
import com.voicenotesai.data.local.dao.ShareableLinkDao
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.local.entity.TaskEntity
import com.voicenotesai.data.local.entity.ReminderEntity
import com.voicenotesai.data.local.entity.CategoryUsageEntity
import com.voicenotesai.data.local.entity.CustomCategoryEntity
import com.voicenotesai.data.local.entity.ShareableLink
import com.voicenotesai.data.local.entity.AnalyticsEvent
import com.voicenotesai.data.local.entity.AnalyticsSession
import com.voicenotesai.data.local.entity.UserJourney
import com.voicenotesai.data.local.entity.AnalyticsAggregate
import com.voicenotesai.data.local.entity.AnalyticsPrivacyPreferences

/**
 * Room database for storing voice notes and analytics data locally.
 * Optimized for large datasets with proper indexing and performance features.
 * 
 * When adding new entities or modifying existing ones:
 * 1. Increment the version number
 * 2. Add a new Migration object in [DatabaseMigrations]
 * 3. Update the [DatabaseModule] to include the new migration
 */
@Database(
    entities = [
        Note::class,
        TaskEntity::class,
        ReminderEntity::class,
        CategoryUsageEntity::class,
        CustomCategoryEntity::class,
        ShareableLink::class,
        AnalyticsEvent::class,
        AnalyticsSession::class,
        UserJourney::class,
        AnalyticsAggregate::class,
        AnalyticsPrivacyPreferences::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryUsageDao(): CategoryUsageDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun shareableLinkDao(): ShareableLinkDao
    abstract fun analyticsDao(): AnalyticsDao
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

    /**
     * Migration from version 2 to 3.
     * Adds analytics tables for privacy-first local analytics.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create analytics_events table
            database.execSQL("""
                CREATE TABLE analytics_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    eventType TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    sessionId TEXT NOT NULL,
                    properties TEXT NOT NULL,
                    anonymized INTEGER NOT NULL DEFAULT 1
                )
            """)
            
            // Create analytics_sessions table
            database.execSQL("""
                CREATE TABLE analytics_sessions (
                    sessionId TEXT PRIMARY KEY NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    eventCount INTEGER NOT NULL DEFAULT 0,
                    appVersion TEXT NOT NULL,
                    deviceInfo TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1
                )
            """)
            
            // Create user_journeys table
            database.execSQL("""
                CREATE TABLE user_journeys (
                    journeyId TEXT PRIMARY KEY NOT NULL,
                    sessionId TEXT NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    steps TEXT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0,
                    abandonedAt TEXT
                )
            """)
            
            // Create analytics_aggregates table
            database.execSQL("""
                CREATE TABLE analytics_aggregates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    aggregateType TEXT NOT NULL,
                    data TEXT NOT NULL,
                    lastUpdated INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // Create analytics_privacy_preferences table
            database.execSQL("""
                CREATE TABLE analytics_privacy_preferences (
                    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                    collectUsageData INTEGER NOT NULL DEFAULT 1,
                    collectPerformanceData INTEGER NOT NULL DEFAULT 1,
                    collectErrorData INTEGER NOT NULL DEFAULT 1,
                    collectContentInsights INTEGER NOT NULL DEFAULT 0,
                    dataRetentionDays INTEGER NOT NULL DEFAULT 90,
                    anonymizeData INTEGER NOT NULL DEFAULT 1,
                    lastUpdated INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // Create indexes for analytics tables
            database.execSQL("CREATE INDEX idx_analytics_timestamp ON analytics_events(timestamp)")
            database.execSQL("CREATE INDEX idx_analytics_event_type ON analytics_events(eventType)")
            database.execSQL("CREATE INDEX idx_analytics_session ON analytics_events(sessionId)")
            database.execSQL("CREATE INDEX idx_analytics_timestamp_type ON analytics_events(timestamp, eventType)")
            
            database.execSQL("CREATE INDEX idx_session_id ON analytics_sessions(sessionId)")
            database.execSQL("CREATE INDEX idx_session_start_time ON analytics_sessions(startTime)")
            database.execSQL("CREATE INDEX idx_session_end_time ON analytics_sessions(endTime)")
            
            database.execSQL("CREATE INDEX idx_journey_id ON user_journeys(journeyId)")
            database.execSQL("CREATE INDEX idx_journey_session ON user_journeys(sessionId)")
            database.execSQL("CREATE INDEX idx_journey_start_time ON user_journeys(startTime)")
            database.execSQL("CREATE INDEX idx_journey_completed ON user_journeys(completed)")
            
            database.execSQL("CREATE INDEX idx_aggregate_date ON analytics_aggregates(date)")
            database.execSQL("CREATE INDEX idx_aggregate_type ON analytics_aggregates(aggregateType)")
            database.execSQL("CREATE INDEX idx_aggregate_date_type ON analytics_aggregates(date, aggregateType)")
        }
    }

    /**
     * Migration from version 3 to 4.
     * Adds tasks table for task management functionality.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create tasks table
            database.execSQL("""
                CREATE TABLE tasks (
                    id TEXT PRIMARY KEY NOT NULL,
                    text TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    sourceNoteId INTEGER,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER,
                    dueDate INTEGER,
                    priority TEXT NOT NULL DEFAULT 'NORMAL'
                )
            """)
            
            // Create indexes for tasks table
            database.execSQL("CREATE INDEX idx_tasks_completed ON tasks(isCompleted)")
            database.execSQL("CREATE INDEX idx_tasks_source_note ON tasks(sourceNoteId)")
            database.execSQL("CREATE INDEX idx_tasks_created_at ON tasks(createdAt)")
            database.execSQL("CREATE INDEX idx_tasks_due_date ON tasks(dueDate)")
            database.execSQL("CREATE INDEX idx_tasks_priority ON tasks(priority)")
            database.execSQL("CREATE INDEX idx_tasks_completed_created ON tasks(isCompleted, createdAt)")
            database.execSQL("CREATE INDEX idx_tasks_source_completed ON tasks(sourceNoteId, isCompleted)")
        }
    }

    /**
     * Migration from version 4 to 5.
     * Adds reminders table for smart reminder functionality.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create reminders table
            database.execSQL("""
                CREATE TABLE reminders (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    triggerTime INTEGER NOT NULL,
                    sourceNoteId INTEGER,
                    sourceTaskId TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    reminderType TEXT NOT NULL DEFAULT 'ONE_TIME',
                    repeatInterval INTEGER,
                    createdAt INTEGER NOT NULL,
                    completedAt INTEGER,
                    FOREIGN KEY(sourceNoteId) REFERENCES notes(id) ON DELETE CASCADE,
                    FOREIGN KEY(sourceTaskId) REFERENCES tasks(id) ON DELETE CASCADE
                )
            """)
            
            // Create indexes for reminders table
            database.execSQL("CREATE INDEX idx_reminders_trigger_time ON reminders(triggerTime)")
            database.execSQL("CREATE INDEX idx_reminders_completed ON reminders(isCompleted)")
            database.execSQL("CREATE INDEX idx_reminders_source_note ON reminders(sourceNoteId)")
            database.execSQL("CREATE INDEX idx_reminders_source_task ON reminders(sourceTaskId)")
            database.execSQL("CREATE INDEX idx_reminders_type ON reminders(reminderType)")
            database.execSQL("CREATE INDEX idx_reminders_trigger_completed ON reminders(triggerTime, isCompleted)")
        }
    }

    /**
     * Migration from version 5 to 6.
     * Adds category management tables for smart categorization.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create category_usage table
            database.execSQL("""
                CREATE TABLE category_usage (
                    category TEXT PRIMARY KEY NOT NULL,
                    usageCount INTEGER NOT NULL DEFAULT 1,
                    lastUsed INTEGER NOT NULL,
                    averageConfidence REAL NOT NULL DEFAULT 0.0,
                    commonKeywords TEXT NOT NULL DEFAULT '',
                    totalConfidence REAL NOT NULL DEFAULT 0.0
                )
            """)
            
            // Create custom_categories table
            database.execSQL("""
                CREATE TABLE custom_categories (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    colorValue INTEGER NOT NULL,
                    iconName TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    usageCount INTEGER NOT NULL DEFAULT 0
                )
            """)
            
            // Create indexes for category_usage table
            database.execSQL("CREATE INDEX idx_category_usage_category ON category_usage(category)")
            database.execSQL("CREATE INDEX idx_category_usage_last_used ON category_usage(lastUsed)")
            database.execSQL("CREATE INDEX idx_category_usage_count ON category_usage(usageCount)")
            
            // Create indexes for custom_categories table
            database.execSQL("CREATE INDEX idx_custom_categories_name ON custom_categories(name)")
            database.execSQL("CREATE INDEX idx_custom_categories_created ON custom_categories(createdAt)")
            database.execSQL("CREATE INDEX idx_custom_categories_usage ON custom_categories(usageCount)")
        }
    }

    /**
     * Migration from version 6 to 7.
     * Adds shareable_links table for sharing functionality.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create shareable_links table
            database.execSQL("""
                CREATE TABLE shareable_links (
                    id TEXT PRIMARY KEY NOT NULL,
                    noteId TEXT NOT NULL,
                    shareUrl TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    expiresAt INTEGER,
                    accessCount INTEGER NOT NULL DEFAULT 0,
                    maxAccessCount INTEGER,
                    password TEXT,
                    allowDownload INTEGER NOT NULL DEFAULT 1,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdByUserId TEXT,
                    lastAccessedAt INTEGER,
                    accessLog TEXT,
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON DELETE CASCADE
                )
            """)
            
            // Create indexes for shareable_links table
            database.execSQL("CREATE INDEX idx_shareable_links_note_id ON shareable_links(noteId)")
            database.execSQL("CREATE UNIQUE INDEX idx_shareable_links_share_url ON shareable_links(shareUrl)")
            database.execSQL("CREATE INDEX idx_shareable_links_expires_at ON shareable_links(expiresAt)")
            database.execSQL("CREATE INDEX idx_shareable_links_is_active ON shareable_links(isActive)")
        }
    }
}
