package com.ynixt.sharedfinances.domain.services

data class InactiveAccountDeletionResult(
    val warningsSent: Int,
    val accountsDeleted: Int,
)

interface InactiveAccountDeletionService {
    suspend fun runCleanup(): InactiveAccountDeletionResult?
}
