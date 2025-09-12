package com.ynixt.sharedfinances.domain.exceptions

import org.springframework.http.HttpStatusCode
import java.util.UUID

class MemberAlreadyInGroupException(
    userId: UUID,
    groupId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.memberAlreadyInGroup",
        argsI18n =
            mapOf<String, Any>(
                "userId" to userId,
                "groupId" to groupId,
            ),
        alternativeMessage = "User is already on this group.",
        cause = cause,
    )
