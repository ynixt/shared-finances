package com.ynixt.sharedfinances.domain.services

/**
 * Deletes user accounts that were never email-verified and are older than the confirmation TTL.
 */
interface UnconfirmedAccountCleanupService {
    /**
     * When email confirmation is disabled in config, returns `null` (no work, no storage access).
     * Otherwise finds eligible users and deletes each via [AccountDeletionService]; returns how many succeeded.
     */
    suspend fun runCleanup(): Int?
}
