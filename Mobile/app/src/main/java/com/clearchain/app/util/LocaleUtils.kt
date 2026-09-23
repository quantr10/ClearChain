package com.clearchain.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language handling.
 *
 * `AppCompatDelegate.setApplicationLocales` cannot do this job here. Below API 33 it needs
 * AppCompat to own the Activity, and from API 33 it only reaches the system `LocaleManager`
 * once AppCompat has recorded a Context of its own — which happens when an `AppCompatActivity`
 * or `AppCompatDelegate` is created. [com.clearchain.app.MainActivity] is a plain
 * `ComponentActivity`, so neither holds and the call silently does nothing: the language
 * preference was stored but never applied.
 *
 * Overriding the Configuration ourselves applies the choice directly and behaves the same on
 * every supported API level. The system language picker is not an alternative route in either
 * direction — it only lists apps that ship an `android:localeConfig`, which this one does not —
 * so there is no second source of truth to reconcile with.
 */
object LocaleUtils {

    /**
     * Returns [context] with its resources resolving in [language]. Call from
     * `attachBaseContext`, before any resource is read.
     */
    fun wrap(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        // Also moves the default used by String.format, dates and number formatting, so a
        // screen that formats a value outside of resources follows the chosen language too.
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }

    /**
     * The Activity behind a Compose [Context], which may be a ContextWrapper after [wrap].
     * Null when the context is not hosted by one.
     */
    fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
