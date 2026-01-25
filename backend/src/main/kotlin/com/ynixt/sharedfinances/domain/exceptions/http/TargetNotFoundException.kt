package com.ynixt.sharedfinances.domain.exceptions.http

import java.util.UUID

class TargetNotFoundException(
    walletItemId: UUID,
    cause: Throwable? = null,
) : WalletItemNotFoundException(
        walletItemId = walletItemId,
        cause = cause,
        messageI18n = "apiErrors.targetItemNotFound",
        alternativeMessage = "Target Item $walletItemId not found.",
    )
