package com.screentranslator.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    companion object {
        private const val PREF_NAME = "screen_translator_prefs"
        private const val KEY_SOURCE_LANG_IDX = "source_lang_idx"
        private const val KEY_TARGET_LANG_IDX = "target_lang_idx"
        private const val KEY_CAPTURE_INTERVAL = "capture_interval"
        private const val KEY_OVERLAY_ALPHA = "overlay_alpha"
        private const val KEY_FONT_SIZE = "font_size"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSourceLanguageIndex(index: Int) = prefs.edit().putInt(KEY_SOURCE_LANG_IDX, index).apply()
    fun getSourceLanguageIndex(): Int = prefs.getInt(KEY_SOURCE_LANG_IDX, 0)

    fun saveTargetLanguageIndex(index: Int) = prefs.edit().putInt(KEY_TARGET_LANG_IDX, index).apply()
    fun getTargetLanguageIndex(): Int = prefs.getInt(KEY_TARGET_LANG_IDX, 0)

    fun saveCaptureInterval(seconds: Int) = prefs.edit().putInt(KEY_CAPTURE_INTERVAL, seconds).apply()
    fun getCaptureInterval(): Int = prefs.getInt(KEY_CAPTURE_INTERVAL, 3)

    fun saveOverlayAlpha(alpha: Float) = prefs.edit().putFloat(KEY_OVERLAY_ALPHA, alpha).apply()
    fun getOverlayAlpha(): Float = prefs.getFloat(KEY_OVERLAY_ALPHA, 0.85f)

    fun saveFontSize(size: Int) = prefs.edit().putInt(KEY_FONT_SIZE, size).apply()
    fun getFontSize(): Int = prefs.getInt(KEY_FONT_SIZE, 14)
}
