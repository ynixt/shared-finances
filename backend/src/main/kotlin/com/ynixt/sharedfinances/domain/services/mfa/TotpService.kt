package com.ynixt.sharedfinances.domain.services.mfa

interface TotpService {
    fun generateNewSecret(): String

    fun verifyRaw(
        rawSecret: String,
        code: String,
    ): Boolean
}
