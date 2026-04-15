package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.UnconfirmedAccountCleanupService
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class UnconfirmedAccountCleanupServiceImpl(
    private val authProperties: AuthProperties,
    private val userRepository: UserRepository,
    private val accountDeletionService: AccountDeletionService,
    private val clock: Clock,
) : UnconfirmedAccountCleanupService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun runCleanup(): Int? {
        if (!authProperties.features.emailConfirmationEnabled) {
            return null
        }

        logger.info("Unconfirmed account cleanup started")

        val cutoff =
            OffsetDateTime.ofInstant(
                clock.instant().minus(Duration.ofMinutes(authProperties.emailConfirmation.ttlMinutes)),
                ZoneOffset.UTC,
            )

        val ids =
            userRepository
                .findUnverifiedUserIdsCreatedBefore(cutoff)
                .collectList()
                .awaitSingle()

        var deleted = 0

        for (id in ids) {
            runCatching {
                accountDeletionService.deleteAccountForUser(id)
                deleted++
            }.onFailure { ex ->
                logger.warn("Failed to delete stale unverified user {}", id, ex)
            }
        }

        return deleted
    }
}
