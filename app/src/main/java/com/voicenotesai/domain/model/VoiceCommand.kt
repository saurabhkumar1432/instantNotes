package com.voicenotesai.domain.model

/**
 * Represents voice commands that can be recognized and executed by the app.
 */
enum class VoiceCommand(val phrases: List<String>) {
    // Recording commands
    START_RECORDING(
        listOf(
            "start recording",
            "begin recording", 
            "record note",
            "start note",
            "new recording",
            "record voice note",
            "begin voice note"
        )
    ),
    STOP_RECORDING(
        listOf(
            "stop recording",
            "end recording",
            "finish recording",
            "stop note",
            "end note",
            "finish note"
        )
    ),
    PAUSE_RECORDING(
        listOf(
            "pause recording",
            "pause note",
            "hold recording"
        )
    ),
    RESUME_RECORDING(
        listOf(
            "resume recording",
            "continue recording",
            "resume note"
        )
    ),
    
    // Note management commands
    SAVE_NOTE(
        listOf(
            "save note",
            "save recording",
            "keep note",
            "save this",
            "save current note"
        )
    ),
    DELETE_NOTE(
        listOf(
            "delete note",
            "remove note",
            "delete this note",
            "discard note"
        )
    ),
    SHARE_NOTE(
        listOf(
            "share note",
            "share this note",
            "send note",
            "export note"
        )
    ),
    
    // Navigation commands
    GO_HOME(
        listOf(
            "go home",
            "home screen",
            "main screen",
            "go to home"
        )
    ),
    OPEN_NOTES(
        listOf(
            "open notes",
            "view notes",
            "show notes",
            "notes list",
            "go to notes"
        )
    ),
    OPEN_TASKS(
        listOf(
            "open tasks",
            "view tasks",
            "show tasks",
            "task list",
            "go to tasks"
        )
    ),
    OPEN_SETTINGS(
        listOf(
            "open settings",
            "settings",
            "preferences",
            "go to settings"
        )
    ),
    GO_BACK(
        listOf(
            "go back",
            "back",
            "previous screen",
            "navigate back"
        )
    ),
    
    // Task management commands
    CREATE_TASK(
        listOf(
            "create task",
            "add task",
            "new task",
            "make task"
        )
    ),
    COMPLETE_TASK(
        listOf(
            "complete task",
            "mark task complete",
            "finish task",
            "task done"
        )
    ),
    CREATE_REMINDER(
        listOf(
            "create reminder",
            "set reminder",
            "remind me",
            "add reminder",
            "schedule reminder"
        )
    ),
    
    // Search and filter commands
    SEARCH_NOTES(
        listOf(
            "search notes",
            "find notes",
            "search for",
            "look for"
        )
    ),
    CLEAR_SEARCH(
        listOf(
            "clear search",
            "reset search",
            "remove search"
        )
    ),
    FILTER_NOTES(
        listOf(
            "filter notes",
            "show only",
            "filter by"
        )
    ),
    
    // Accessibility commands
    READ_SCREEN(
        listOf(
            "read screen",
            "what's on screen",
            "describe screen",
            "read content"
        )
    ),
    REPEAT_LAST(
        listOf(
            "repeat",
            "say again",
            "repeat last",
            "what did you say"
        )
    ),
    HELP(
        listOf(
            "help",
            "what can I say",
            "voice commands",
            "available commands"
        )
    ),
    
    // Playback commands
    PLAY_NOTE(
        listOf(
            "play note",
            "read note",
            "play this note",
            "read this note"
        )
    ),
    STOP_PLAYBACK(
        listOf(
            "stop playing",
            "stop reading",
            "pause playback"
        )
    );

    companion object {
        /**
         * Attempts to match a spoken phrase to a voice command.
         */
        fun fromPhrase(phrase: String): VoiceCommand? {
            val normalizedPhrase = phrase.lowercase().trim()
            return values().find { command ->
                command.phrases.any { commandPhrase ->
                    normalizedPhrase.contains(commandPhrase)
                }
            }
        }
    }
}

/**
 * Result of voice command processing.
 */
sealed class VoiceCommandResult {
    object Success : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
    object NotRecognized : VoiceCommandResult()
    object NotAvailable : VoiceCommandResult()
}