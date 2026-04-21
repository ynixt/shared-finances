package com.ynixt.sharedfinances.domain.services.auth

interface EmailTokenGeneratorService {
    fun generateEmailVerificationToken(): String
}
