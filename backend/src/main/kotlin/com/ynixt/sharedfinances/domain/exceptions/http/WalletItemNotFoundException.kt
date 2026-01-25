package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

open class WalletItemNotFoundException(
    walletItemId: UUID,
    cause: Throwable? = null,
    messageI18n: String = "apiErrors.walletItemNotFound",
    alternativeMessage: String = "Wallet Item $walletItemId not found.",
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = messageI18n,
        argsI18n =
            mapOf<String, Any>(
                "walletItemId" to walletItemId,
            ),
        alternativeMessage = alternativeMessage,
        cause = cause,
    )
