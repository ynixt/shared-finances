package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class GroupOwnerCannotLeaveException :
    AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.groupOwnership.ownerCannotLeave",
        alternativeMessage = "The group owner must transfer ownership or delete the group before leaving.",
    )
