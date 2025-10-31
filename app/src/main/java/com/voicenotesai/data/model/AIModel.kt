package com.voicenotesai.data.model

/**
 * Represents the capabilities that an AI model can have
 */
enum class AICapability {
    /**
     * General text generation and completion
     */
    TEXT_GENERATION,
    
    /**
     * Extracting tasks and action items from text
     */
    TASK_EXTRACTION,
    
    /**
     * Summarizing long text content
     */
    SUMMARIZATION,
    
    /**
     * Categorizing and tagging content
     */
    CATEGORIZATION,
    
    /**
     * Analyzing sentiment and tone
     */
    SENTIMENT_ANALYSIS,
    
    /**
     * Extracting entities (names, dates, locations, etc.)
     */
    ENTITY_EXTRACTION,
    
    /**
     * Language translation
     */
    TRANSLATION,
    
    /**
     * Code generation and analysis
     */
    CODE_GENERATION,
    
    /**
     * Image understanding and analysis
     */
    VISION,
    
    /**
     * Function calling and tool use
     */
    FUNCTION_CALLING
}

/**
 * Data class representing an AI model with its metadata and capabilities.
 * Used to display available models and their features to users.
 */
data class AIModel(
    /**
     * Unique identifier for the model
     */
    val id: String,
    
    /**
     * Human-readable name of the model
     */
    val name: String,
    
    /**
     * The provider that hosts this model
     */
    val provider: AIProviderType,
    
    /**
     * Set of capabilities this model supports
     */
    val capabilities: Set<AICapability>,
    
    /**
     * Optional description of the model
     */
    val description: String? = null,
    
    /**
     * Context window size (maximum tokens)
     */
    val contextWindow: Int? = null,
    
    /**
     * Whether this model is currently available
     */
    val isAvailable: Boolean = true,
    
    /**
     * Relative cost tier (1 = cheapest, 5 = most expensive)
     */
    val costTier: Int = 3,
    
    /**
     * Performance tier (1 = slowest, 5 = fastest)
     */
    val performanceTier: Int = 3
) {
    /**
     * Returns whether this model supports the given capability
     */
    fun supports(capability: AICapability): Boolean = capabilities.contains(capability)
    
    /**
     * Returns whether this model supports all the given capabilities
     */
    fun supportsAll(requiredCapabilities: Set<AICapability>): Boolean {
        return capabilities.containsAll(requiredCapabilities)
    }
    
    /**
     * Returns a display string showing the model's key features
     */
    fun getFeatureSummary(): String {
        val features = mutableListOf<String>()
        
        if (supports(AICapability.VISION)) features.add("Vision")
        if (supports(AICapability.FUNCTION_CALLING)) features.add("Functions")
        if (supports(AICapability.CODE_GENERATION)) features.add("Code")
        
        contextWindow?.let { 
            val contextSize = when {
                it >= 1000000 -> "${it / 1000000}M"
                it >= 1000 -> "${it / 1000}K"
                else -> it.toString()
            }
            features.add("${contextSize} tokens")
        }
        
        return if (features.isNotEmpty()) features.joinToString(" • ") else ""
    }
    
    companion object {
        /**
         * Common capability sets for different use cases
         */
        val BASIC_TEXT_CAPABILITIES = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.SUMMARIZATION
        )
        
        val NOTE_PROCESSING_CAPABILITIES = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.TASK_EXTRACTION,
            AICapability.SUMMARIZATION,
            AICapability.CATEGORIZATION,
            AICapability.ENTITY_EXTRACTION
        )
        
        val ADVANCED_CAPABILITIES = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.TASK_EXTRACTION,
            AICapability.SUMMARIZATION,
            AICapability.CATEGORIZATION,
            AICapability.SENTIMENT_ANALYSIS,
            AICapability.ENTITY_EXTRACTION,
            AICapability.FUNCTION_CALLING
        )
        
        /**
         * Predefined models for common providers
         */
        fun getOpenAIModels(): List<AIModel> = listOf(
            AIModel(
                id = "gpt-3.5-turbo",
                name = "GPT-3.5 Turbo",
                provider = AIProviderType.OpenAI,
                capabilities = NOTE_PROCESSING_CAPABILITIES,
                description = "Fast and efficient model for most tasks",
                contextWindow = 16385,
                costTier = 2,
                performanceTier = 4
            ),
            AIModel(
                id = "gpt-4",
                name = "GPT-4",
                provider = AIProviderType.OpenAI,
                capabilities = ADVANCED_CAPABILITIES,
                description = "Most capable model for complex reasoning",
                contextWindow = 8192,
                costTier = 4,
                performanceTier = 3
            ),
            AIModel(
                id = "gpt-4-turbo",
                name = "GPT-4 Turbo",
                provider = AIProviderType.OpenAI,
                capabilities = ADVANCED_CAPABILITIES + AICapability.VISION,
                description = "Latest GPT-4 with vision capabilities",
                contextWindow = 128000,
                costTier = 3,
                performanceTier = 4
            )
        )
        
        fun getAnthropicModels(): List<AIModel> = listOf(
            AIModel(
                id = "claude-3-haiku-20240307",
                name = "Claude 3 Haiku",
                provider = AIProviderType.Anthropic,
                capabilities = NOTE_PROCESSING_CAPABILITIES,
                description = "Fast and cost-effective model",
                contextWindow = 200000,
                costTier = 1,
                performanceTier = 5
            ),
            AIModel(
                id = "claude-3-sonnet-20240229",
                name = "Claude 3 Sonnet",
                provider = AIProviderType.Anthropic,
                capabilities = ADVANCED_CAPABILITIES + AICapability.VISION,
                description = "Balanced performance and capability",
                contextWindow = 200000,
                costTier = 3,
                performanceTier = 4
            ),
            AIModel(
                id = "claude-3-opus-20240229",
                name = "Claude 3 Opus",
                provider = AIProviderType.Anthropic,
                capabilities = ADVANCED_CAPABILITIES + AICapability.VISION,
                description = "Most powerful model for complex tasks",
                contextWindow = 200000,
                costTier = 5,
                performanceTier = 3
            )
        )
        
        fun getGoogleAIModels(): List<AIModel> = listOf(
            AIModel(
                id = "gemini-pro",
                name = "Gemini Pro",
                provider = AIProviderType.GoogleAI,
                capabilities = ADVANCED_CAPABILITIES,
                description = "Google's most capable text model",
                contextWindow = 32768,
                costTier = 2,
                performanceTier = 4
            ),
            AIModel(
                id = "gemini-pro-vision",
                name = "Gemini Pro Vision",
                provider = AIProviderType.GoogleAI,
                capabilities = ADVANCED_CAPABILITIES + AICapability.VISION,
                description = "Gemini Pro with vision capabilities",
                contextWindow = 16384,
                costTier = 3,
                performanceTier = 3
            )
        )
    }
}