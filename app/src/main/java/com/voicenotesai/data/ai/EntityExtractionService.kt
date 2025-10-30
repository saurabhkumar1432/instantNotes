package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.ExtractedEntity
import com.voicenotesai.domain.ai.EntityType
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for extracting entities from text using pattern matching and NLP techniques.
 * Identifies dates, names, locations, tasks, and other important entities.
 */
@Singleton
class EntityExtractionService @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        // Email pattern
        private val EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        )
        
        // Phone number patterns (various formats)
        private val PHONE_PATTERNS = listOf(
            Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b"), // 123-456-7890
            Pattern.compile("\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b"), // (123) 456-7890
            Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{4}\\b"), // 123.456.7890
            Pattern.compile("\\b\\d{10}\\b"), // 1234567890
            Pattern.compile("\\+\\d{1,3}\\s*\\d{3,4}\\s*\\d{3,4}\\s*\\d{3,4}") // +1 123 456 7890
        )
        
        // URL pattern
        private val URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
        )
        
        // Date patterns
        private val DATE_PATTERNS = listOf(
            Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b"), // MM/DD/YYYY or M/D/YY
            Pattern.compile("\\b\\d{1,2}-\\d{1,2}-\\d{2,4}\\b"), // MM-DD-YYYY
            Pattern.compile("\\b\\d{4}-\\d{1,2}-\\d{1,2}\\b"), // YYYY-MM-DD
            Pattern.compile("\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?\\s+\\d{1,2},?\\s+\\d{4}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}\\b", Pattern.CASE_INSENSITIVE)
        )
        
        // Time patterns
        private val TIME_PATTERNS = listOf(
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(AM|PM)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}:\\d{2}:\\d{2}\\b"), // HH:MM:SS
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\b") // HH:MM
        )
        
        // Money patterns
        private val MONEY_PATTERNS = listOf(
            Pattern.compile("\\$\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\b"), // $1,234.56
            Pattern.compile("\\b\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\s*(?:dollars?|USD|usd)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("€\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\b"), // €1,234.56
            Pattern.compile("£\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\b") // £1,234.56
        )
        
        // Percentage pattern
        private val PERCENTAGE_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?%\\b")
        
        // Task indicators
        private val TASK_PATTERNS = listOf(
            Pattern.compile("(?:TODO|To do|Action item|Task|Need to|Should|Must|Remember to)\\s*:?\\s*([^.!?\\n]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:call|email|send|schedule|book|arrange|organize|prepare|review|complete|finish|start|begin)\\s+([^.!?\\n]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Deadline patterns
        private val DEADLINE_PATTERNS = listOf(
            Pattern.compile("(?:due|deadline|by|before|until)\\s+([^.!?\\n]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:expires?|ends?)\\s+([^.!?\\n]+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Common organization suffixes
        private val ORG_SUFFIXES = setOf(
            "Inc", "LLC", "Corp", "Corporation", "Company", "Co", "Ltd", "Limited",
            "Foundation", "Institute", "University", "College", "School", "Hospital",
            "Bank", "Group", "Partners", "Associates", "Solutions", "Services",
            "Technologies", "Systems", "Consulting", "Enterprises"
        )
        
        // Common person name prefixes and suffixes
        private val NAME_PREFIXES = setOf("Mr", "Mrs", "Ms", "Dr", "Prof", "Professor")
        private val NAME_SUFFIXES = setOf("Jr", "Sr", "II", "III", "IV", "PhD", "MD", "Esq")
    }

    /**
     * Extracts entities from the given text.
     */
    suspend fun extractEntities(text: String): List<ExtractedEntity> = withContext(ioDispatcher) {
        val entities = mutableListOf<ExtractedEntity>()
        
        // Extract different types of entities
        entities.addAll(extractEmails(text))
        entities.addAll(extractPhoneNumbers(text))
        entities.addAll(extractUrls(text))
        entities.addAll(extractDates(text))
        entities.addAll(extractTimes(text))
        entities.addAll(extractMoney(text))
        entities.addAll(extractPercentages(text))
        entities.addAll(extractTasks(text))
        entities.addAll(extractDeadlines(text))
        entities.addAll(extractPersonNames(text))
        entities.addAll(extractOrganizations(text))
        entities.addAll(extractLocations(text))
        
        // Sort by start index and remove overlapping entities
        entities.sortedBy { it.startIndex }
            .fold(mutableListOf<ExtractedEntity>()) { acc, entity ->
                if (acc.isEmpty() || acc.last().endIndex <= entity.startIndex) {
                    acc.add(entity)
                }
                acc
            }
    }

    private fun extractEmails(text: String): List<ExtractedEntity> {
        return extractWithPattern(text, EMAIL_PATTERN, EntityType.EMAIL)
    }

    private fun extractPhoneNumbers(text: String): List<ExtractedEntity> {
        return PHONE_PATTERNS.flatMap { pattern ->
            extractWithPattern(text, pattern, EntityType.PHONE_NUMBER)
        }
    }

    private fun extractUrls(text: String): List<ExtractedEntity> {
        return extractWithPattern(text, URL_PATTERN, EntityType.URL)
    }

    private fun extractDates(text: String): List<ExtractedEntity> {
        return DATE_PATTERNS.flatMap { pattern ->
            extractWithPattern(text, pattern, EntityType.DATE)
        }
    }

    private fun extractTimes(text: String): List<ExtractedEntity> {
        return TIME_PATTERNS.flatMap { pattern ->
            extractWithPattern(text, pattern, EntityType.TIME)
        }
    }

    private fun extractMoney(text: String): List<ExtractedEntity> {
        return MONEY_PATTERNS.flatMap { pattern ->
            extractWithPattern(text, pattern, EntityType.MONEY)
        }
    }

    private fun extractPercentages(text: String): List<ExtractedEntity> {
        return extractWithPattern(text, PERCENTAGE_PATTERN, EntityType.PERCENTAGE)
    }

    private fun extractTasks(text: String): List<ExtractedEntity> {
        return TASK_PATTERNS.flatMap { pattern ->
            extractWithPatternGroup(text, pattern, EntityType.TASK, groupIndex = 1)
        }
    }

    private fun extractDeadlines(text: String): List<ExtractedEntity> {
        return DEADLINE_PATTERNS.flatMap { pattern ->
            extractWithPatternGroup(text, pattern, EntityType.DEADLINE, groupIndex = 1)
        }
    }

    private fun extractPersonNames(text: String): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()
        
        // Pattern for names with prefixes/suffixes
        val nameWithPrefixPattern = Pattern.compile(
            "\\b(?:${NAME_PREFIXES.joinToString("|")})\\.?\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*(?:\\s+(?:${NAME_SUFFIXES.joinToString("|")})\\.?)?)\\b"
        )
        
        entities.addAll(extractWithPatternGroup(text, nameWithPrefixPattern, EntityType.PERSON, groupIndex = 1))
        
        // Pattern for capitalized names (2-3 words)
        val capitalizedNamePattern = Pattern.compile("\\b[A-Z][a-z]+\\s+[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?\\b")
        val potentialNames = extractWithPattern(text, capitalizedNamePattern, EntityType.PERSON)
        
        // Filter out common non-names
        val commonNonNames = setOf(
            "United States", "New York", "Los Angeles", "San Francisco", "Washington DC",
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
            "January", "February", "March", "April", "May", "June", "July", "August",
            "September", "October", "November", "December"
        )
        
        entities.addAll(potentialNames.filter { entity ->
            !commonNonNames.contains(entity.text) && 
            !entity.text.matches("\\b[A-Z]{2,}\\b".toRegex()) // Avoid acronyms
        })
        
        return entities
    }

    private fun extractOrganizations(text: String): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()
        
        // Pattern for organizations with common suffixes
        val orgPattern = Pattern.compile(
            "\\b[A-Z][a-zA-Z\\s&]+(?:${ORG_SUFFIXES.joinToString("|")})\\.?\\b"
        )
        
        entities.addAll(extractWithPattern(text, orgPattern, EntityType.ORGANIZATION))
        
        // Pattern for acronyms (likely organizations)
        val acronymPattern = Pattern.compile("\\b[A-Z]{2,5}\\b")
        entities.addAll(extractWithPattern(text, acronymPattern, EntityType.ORGANIZATION))
        
        return entities
    }

    private fun extractLocations(text: String): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()
        
        // Common location patterns
        val locationPatterns = listOf(
            Pattern.compile("\\b[A-Z][a-z]+,\\s*[A-Z]{2}\\b"), // City, ST
            Pattern.compile("\\b[A-Z][a-z]+\\s+[A-Z][a-z]+,\\s*[A-Z]{2}\\b"), // City Name, ST
            Pattern.compile("\\b\\d+\\s+[A-Z][a-zA-Z\\s]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln|Way|Place|Pl)\\b", Pattern.CASE_INSENSITIVE)
        )
        
        locationPatterns.forEach { pattern ->
            entities.addAll(extractWithPattern(text, pattern, EntityType.LOCATION))
        }
        
        return entities
    }

    private fun extractWithPattern(
        text: String,
        pattern: Pattern,
        entityType: EntityType,
        confidence: Float = 0.8f
    ): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            entities.add(
                ExtractedEntity(
                    text = matcher.group(),
                    type = entityType,
                    confidence = confidence,
                    startIndex = matcher.start(),
                    endIndex = matcher.end()
                )
            )
        }
        
        return entities
    }

    private fun extractWithPatternGroup(
        text: String,
        pattern: Pattern,
        entityType: EntityType,
        groupIndex: Int,
        confidence: Float = 0.7f
    ): List<ExtractedEntity> {
        val entities = mutableListOf<ExtractedEntity>()
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            if (matcher.groupCount() >= groupIndex) {
                val groupText = matcher.group(groupIndex)?.trim()
                if (!groupText.isNullOrEmpty()) {
                    entities.add(
                        ExtractedEntity(
                            text = groupText,
                            type = entityType,
                            confidence = confidence,
                            startIndex = matcher.start(groupIndex),
                            endIndex = matcher.end(groupIndex)
                        )
                    )
                }
            }
        }
        
        return entities
    }
}