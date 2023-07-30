package com.ynixt.sharedfinances.model.exceptions

import org.springframework.http.HttpStatus

class SFExceptionForbidden(
    reason: String? = null, i18nCode: String? = null
) : SFException(reason, HttpStatus.FORBIDDEN, i18nCode)
