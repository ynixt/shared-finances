package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class FinancialGoalInvalidWalletException(
    walletItemId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.financialGoalInvalidWallet",
        argsI18n = mapOf("walletItemId" to walletItemId),
        alternativeMessage = "Wallet item $walletItemId is not valid for this goal.",
    )
