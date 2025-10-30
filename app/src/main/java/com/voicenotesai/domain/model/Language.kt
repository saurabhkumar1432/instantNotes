package com.voicenotesai.domain.model

/**
 * Supported languages for processing.
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Spanish"),
    FRENCH("fr", "French"),
    GERMAN("de", "German"),
    ITALIAN("it", "Italian"),
    PORTUGUESE("pt", "Portuguese"),
    RUSSIAN("ru", "Russian"),
    CHINESE_SIMPLIFIED("zh-CN", "Chinese (Simplified)"),
    CHINESE_TRADITIONAL("zh-TW", "Chinese (Traditional)"),
    JAPANESE("ja", "Japanese"),
    KOREAN("ko", "Korean"),
    ARABIC("ar", "Arabic"),
    HINDI("hi", "Hindi"),
    DUTCH("nl", "Dutch"),
    SWEDISH("sv", "Swedish"),
    NORWEGIAN("no", "Norwegian"),
    DANISH("da", "Danish"),
    FINNISH("fi", "Finnish"),
    POLISH("pl", "Polish"),
    CZECH("cs", "Czech"),
    HUNGARIAN("hu", "Hungarian"),
    TURKISH("tr", "Turkish"),
    GREEK("el", "Greek"),
    HEBREW("he", "Hebrew"),
    THAI("th", "Thai"),
    VIETNAMESE("vi", "Vietnamese"),
    INDONESIAN("id", "Indonesian"),
    MALAY("ms", "Malay"),
    FILIPINO("tl", "Filipino"),
    UKRAINIAN("uk", "Ukrainian"),
    BULGARIAN("bg", "Bulgarian"),
    CROATIAN("hr", "Croatian"),
    SERBIAN("sr", "Serbian"),
    SLOVENIAN("sl", "Slovenian"),
    SLOVAK("sk", "Slovak"),
    ROMANIAN("ro", "Romanian"),
    LITHUANIAN("lt", "Lithuanian"),
    LATVIAN("lv", "Latvian"),
    ESTONIAN("et", "Estonian"),
    CATALAN("ca", "Catalan"),
    BASQUE("eu", "Basque"),
    GALICIAN("gl", "Galician"),
    WELSH("cy", "Welsh"),
    IRISH("ga", "Irish"),
    SCOTTISH_GAELIC("gd", "Scottish Gaelic"),
    ICELANDIC("is", "Icelandic"),
    MALTESE("mt", "Maltese"),
    LUXEMBOURGISH("lb", "Luxembourgish"),
    AFRIKAANS("af", "Afrikaans"),
    SWAHILI("sw", "Swahili"),
    AMHARIC("am", "Amharic"),
    YORUBA("yo", "Yoruba"),
    ZULU("zu", "Zulu"),
    XHOSA("xh", "Xhosa"),
    HAUSA("ha", "Hausa"),
    IGBO("ig", "Igbo"),
    SOMALI("so", "Somali"),
    OROMO("om", "Oromo"),
    TIGRINYA("ti", "Tigrinya"),
    KINYARWANDA("rw", "Kinyarwanda"),
    KIRUNDI("rn", "Kirundi"),
    SHONA("sn", "Shona"),
    NDEBELE("nd", "Ndebele"),
    SESOTHO("st", "Sesotho"),
    SETSWANA("tn", "Setswana"),
    TSONGA("ts", "Tsonga"),
    VENDA("ve", "Venda"),
    AUTO_DETECT("auto", "Auto-detect");

    companion object {
        fun fromCode(code: String): Language? {
            return values().find { it.code.equals(code, ignoreCase = true) }
        }
        
        fun getPopularLanguages(): List<Language> {
            return listOf(
                ENGLISH, SPANISH, FRENCH, GERMAN, ITALIAN, PORTUGUESE,
                RUSSIAN, CHINESE_SIMPLIFIED, JAPANESE, KOREAN, ARABIC, HINDI
            )
        }
    }
}