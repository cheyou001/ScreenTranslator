package com.screentranslator.utils

import com.google.mlkit.nl.translate.TranslateLanguage

data class Language(val displayName: String, val code: String)

object LanguageConfig {

    val SOURCE_LANGUAGES = listOf(
        Language("自动检测 (英文)", TranslateLanguage.ENGLISH),
        Language("中文 (简体)", TranslateLanguage.CHINESE),
        Language("日本語", TranslateLanguage.JAPANESE),
        Language("한국어", TranslateLanguage.KOREAN),
        Language("Español", TranslateLanguage.SPANISH),
        Language("Français", TranslateLanguage.FRENCH),
        Language("Deutsch", TranslateLanguage.GERMAN),
        Language("Português", TranslateLanguage.PORTUGUESE),
        Language("Русский", TranslateLanguage.RUSSIAN),
        Language("Arabic / العربية", TranslateLanguage.ARABIC),
        Language("हिन्दी", TranslateLanguage.HINDI),
        Language("Italiano", TranslateLanguage.ITALIAN),
        Language("Bahasa Indonesia", TranslateLanguage.INDONESIAN),
        Language("Tiếng Việt", TranslateLanguage.VIETNAMESE),
        Language("ภาษาไทย", TranslateLanguage.THAI),
        Language("Türkçe", TranslateLanguage.TURKISH),
        Language("Nederlands", TranslateLanguage.DUTCH),
        Language("Polski", TranslateLanguage.POLISH),
        Language("Svenska", TranslateLanguage.SWEDISH)
    )

    val TARGET_LANGUAGES = listOf(
        Language("中文 (简体)", TranslateLanguage.CHINESE),
        Language("English", TranslateLanguage.ENGLISH),
        Language("日本語", TranslateLanguage.JAPANESE),
        Language("한국어", TranslateLanguage.KOREAN),
        Language("Español", TranslateLanguage.SPANISH),
        Language("Français", TranslateLanguage.FRENCH),
        Language("Deutsch", TranslateLanguage.GERMAN),
        Language("Português", TranslateLanguage.PORTUGUESE),
        Language("Русский", TranslateLanguage.RUSSIAN),
        Language("Arabic / العربية", TranslateLanguage.ARABIC),
        Language("हिन्दी", TranslateLanguage.HINDI),
        Language("Italiano", TranslateLanguage.ITALIAN),
        Language("Bahasa Indonesia", TranslateLanguage.INDONESIAN),
        Language("Tiếng Việt", TranslateLanguage.VIETNAMESE),
        Language("ภาษาไทย", TranslateLanguage.THAI),
        Language("Türkçe", TranslateLanguage.TURKISH),
        Language("Nederlands", TranslateLanguage.DUTCH),
        Language("Polski", TranslateLanguage.POLISH),
        Language("Svenska", TranslateLanguage.SWEDISH)
    )
}
