package ai.hev.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage {
    SYSTEM,
    EN,
}

class LanguageStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        applyLocales(_language.value)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
        _language.value = language
        applyLocales(language)
    }

    private fun loadLanguage(): AppLanguage {
        val raw = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name)
        // Former ZH preference maps to system (English resources only).
        return when (raw) {
            AppLanguage.EN.name -> AppLanguage.EN
            else -> AppLanguage.SYSTEM
        }
    }

    companion object {
        private const val PREFS_NAME = "hev_language_prefs"
        private const val KEY_LANGUAGE = "app_language"

        fun applyLocales(language: AppLanguage) {
            val locales = when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
