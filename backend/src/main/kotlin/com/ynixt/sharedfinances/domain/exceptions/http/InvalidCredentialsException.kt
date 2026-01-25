package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.security.authentication.BadCredentialsException

class InvalidCredentialsException(
    val ip: String?,
    val email: String?,
) : BadCredentialsException("invalid credentials")
