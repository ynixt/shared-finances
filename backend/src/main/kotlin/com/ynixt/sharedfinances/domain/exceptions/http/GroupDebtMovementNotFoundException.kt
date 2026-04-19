package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus
import java.util.UUID

class GroupDebtMovementNotFoundException(
    movementId: UUID,
) : AppResponseException(
        statusCode = HttpStatus.NOT_FOUND,
        messageI18n = "apiErrors.groupDebt.movementNotFound",
        alternativeMessage = "Group debt movement not found: $movementId",
    )
