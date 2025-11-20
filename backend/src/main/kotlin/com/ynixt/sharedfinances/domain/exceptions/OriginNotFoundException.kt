package com.ynixt.sharedfinances.domain.exceptions

import java.util.UUID

class OriginNotFoundException(
    walletItemId: UUID,
    cause: Throwable? = null,
) : WalletItemNotFoundException(
        walletItemId = walletItemId,
        cause = cause,
        messageI18n = "apiErrors.originItemNotFound",
        alternativeMessage = "Origin Item $walletItemId not found.",
    )
