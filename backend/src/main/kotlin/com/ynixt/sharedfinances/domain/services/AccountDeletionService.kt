package com.ynixt.sharedfinances.domain.services

import java.util.UUID

interface AccountDeletionService {
    suspend fun deleteAccountForUser(userId: UUID)
}
