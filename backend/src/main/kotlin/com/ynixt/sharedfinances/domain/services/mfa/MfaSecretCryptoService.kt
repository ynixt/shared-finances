package com.ynixt.sharedfinances.domain.services.mfa

interface MfaSecretCryptoService {
    fun encryptTotpSecret(secretBase32: String): String

    fun decryptTotpSecret(secretEnc: String): String
}
