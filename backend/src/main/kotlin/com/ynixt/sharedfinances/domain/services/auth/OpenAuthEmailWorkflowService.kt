package com.ynixt.sharedfinances.domain.services.auth

import com.ynixt.sharedfinances.domain.entities.UserEntity

interface OpenAuthEmailWorkflowService {
    suspend fun sendRegistrationConfirmationIfNeeded(user: UserEntity)

    suspend fun confirmEmail(rawToken: String)

    suspend fun resendConfirmationEmail(email: String): Long

    suspend fun changePendingEmail(
        currentEmail: String,
        newEmail: String,
    ): Long

    suspend fun requestPasswordReset(email: String): Long

    suspend fun resendPasswordResetEmail(email: String): Long

    suspend fun confirmPasswordReset(
        rawToken: String,
        newPassword: String,
    )
}
