package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class MixedCurrencyWalletSourcesException(
    currencies: Collection<String>,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.mixedCurrencyWalletSources",
        alternativeMessage = "All wallet sources must use the same currency (found: ${currencies.distinct().sorted()})",
        argsI18n = mapOf("currencies" to currencies.distinct().sorted().joinToString()),
    )
