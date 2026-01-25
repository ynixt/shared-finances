package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class BankAccountAlreadyInGroupException(
    bankAccountId: UUID,
    groupId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.bankAccountAlreadyInGroup",
        argsI18n =
            mapOf<String, Any>(
                "bankAccount" to bankAccountId,
                "groupId" to groupId,
            ),
        alternativeMessage = "Bank account is already associated to this group.",
        cause = cause,
    )
