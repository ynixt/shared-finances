package com.ynixt.sharedfinances.model.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

open class SFException(
    reason: String?, val httpStatusCode: HttpStatus = HttpStatus.BAD_REQUEST, val i18nCode: String? = null
) : ResponseStatusException(httpStatusCode, reason)
