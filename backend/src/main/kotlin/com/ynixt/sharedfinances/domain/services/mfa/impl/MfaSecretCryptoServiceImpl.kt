package com.ynixt.sharedfinances.domain.services.mfa.impl

import com.ynixt.sharedfinances.domain.services.mfa.MfaSecretCryptoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Service

@Service
class MfaSecretCryptoServiceImpl(
    @param:Value("\${app.security.mfa.crypto.password}") private val password: String,
    @param:Value("\${app.security.mfa.crypto.saltHex}") private val saltHex: String,
) : MfaSecretCryptoService {
    private val encryptor: TextEncryptor =
        Encryptors.delux(password, saltHex)

    override fun encryptTotpSecret(secretBase32: String): String = encryptor.encrypt(secretBase32.trim())

    override fun decryptTotpSecret(secretEnc: String): String = encryptor.decrypt(secretEnc.trim())
}
