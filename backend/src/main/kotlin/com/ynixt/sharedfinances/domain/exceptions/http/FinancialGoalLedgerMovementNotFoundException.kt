package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class FinancialGoalLedgerMovementNotFoundException(
    movementId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.financialGoalMovementNotFound",
        argsI18n = mapOf("movementId" to movementId),
        alternativeMessage = "Goal ledger movement $movementId not found.",
    )
