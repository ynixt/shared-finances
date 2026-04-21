package com.ynixt.sharedfinances.resources.services.auth

import com.ynixt.sharedfinances.domain.services.auth.EmailTokenGeneratorService
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class EmailTokenGeneratorServiceImpl : EmailTokenGeneratorService {
    override fun generateEmailVerificationToken(): String {
        val code = StringBuilder(CODE_LENGTH)

        repeat(CODE_LENGTH) {
            code.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
        }

        return code.toString()
    }

    private companion object {
        private const val CODE_LENGTH = 8
        private const val CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }
}
