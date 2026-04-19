package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class GroupDebtForbiddenException :
    AppResponseException(
        statusCode = HttpStatus.FORBIDDEN,
        messageI18n = "apiErrors.groupDebt.forbidden",
        alternativeMessage = "User cannot access this group debt workspace",
    )
