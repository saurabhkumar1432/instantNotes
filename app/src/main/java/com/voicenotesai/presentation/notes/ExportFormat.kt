package com.voicenotesai.presentation.notes

import androidx.annotation.StringRes
import com.voicenotesai.R

/**
 * Export format options for the presentation layer
 */
enum class ExportFormat(
    @StringRes val displayNameRes: Int,
    val fileExtension: String
) {
    TEXT(R.string.export_format_text, "txt"),
    MARKDOWN(R.string.export_format_markdown, "md"),
    JSON(R.string.export_format_json, "json"),
    PDF(R.string.export_format_pdf, "pdf")
}