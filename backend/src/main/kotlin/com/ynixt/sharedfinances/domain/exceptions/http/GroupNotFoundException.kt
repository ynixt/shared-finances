package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class GroupNotFoundException(
    groupId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.groupNotFound",
        argsI18n =
            mapOf<String, Any>(
                "groupId" to groupId,
            ),
        alternativeMessage = "Group $groupId not found.",
        cause = cause,
    )
