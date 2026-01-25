package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class AccountTemporaryBlocked :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(401),
        messageI18n = "apiErrors.accountTemporaryBlockedTooManyWrongAttempts",
    )
