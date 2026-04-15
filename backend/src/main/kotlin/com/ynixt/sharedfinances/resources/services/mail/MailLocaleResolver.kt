package com.ynixt.sharedfinances.resources.services.mail

import java.util.Locale

internal object MailLocaleResolver {
    /**
     * Maps stored user [lang] (e.g. `en`, `pt-BR`) to a [Locale] for mail bundles (`messages_pt_BR`, etc.).
     */
    fun resolve(userLang: String): Locale {
        val t = userLang.trim()
        if (t.isEmpty()) {
            return Locale.ENGLISH
        }
        val locale = Locale.forLanguageTag(t.replace('_', '-'))
        return if (locale.language.isEmpty()) {
            Locale.ENGLISH
        } else {
            locale
        }
    }
}
