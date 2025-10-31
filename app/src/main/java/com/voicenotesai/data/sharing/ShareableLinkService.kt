package com.voicenotesai.data.sharing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of ShareableLinkService
 */
@Singleton
class ShareableLinkService @Inject constructor() {
    
    fun createShareableLink(noteId: String, options: Any): String {
        return "https://example.com/note/$noteId"
    }
}