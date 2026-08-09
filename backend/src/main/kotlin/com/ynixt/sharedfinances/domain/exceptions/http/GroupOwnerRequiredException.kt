package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class GroupOwnerRequiredException :
    AppResponseException(
        statusCode = HttpStatus.FORBIDDEN,
        messageI18n = "apiErrors.groupOwnership.ownerRequired",
        alternativeMessage = "Only the group owner can perform this operation.",
    )
