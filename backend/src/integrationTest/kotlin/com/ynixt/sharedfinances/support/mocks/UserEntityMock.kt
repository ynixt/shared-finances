package com.ynixt.sharedfinances.support.mocks

import com.ynixt.sharedfinances.domain.entities.UserEntity

object UserEntityMock {
    fun defaultUser(
        email: String = "a@a.c",
        passwordHash: String?,
        emailVerified: Boolean = true,
    ) = UserEntity(
        email = email,
        passwordHash = passwordHash,
        firstName = "Credit",
        lastName = "Card IT",
        lang = "pt-BR",
        defaultCurrency = "BRL",
        tmz = "America/Sao_Paulo",
        photoUrl = null,
        emailVerified = emailVerified,
        mfaEnabled = false,
        totpSecret = null,
        onboardingDone = true,
    )
}
