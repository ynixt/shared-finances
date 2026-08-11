package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.InactiveAccountDeletionProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.InactiveAccountDeletionResult
import com.ynixt.sharedfinances.domain.services.InactiveAccountDeletionService
import com.ynixt.sharedfinances.domain.services.mail.TransactionalEmailSender
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.resources.services.mail.AccountLifecycleMailMessageComposer
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class InactiveAccountDeletionServiceImpl(
    private val properties: InactiveAccountDeletionProperties,
    private val planProperties: PlanProperties,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val planLimitService: PlanLimitService,
    private val composer: AccountLifecycleMailMessageComposer,
    private val dispatchService: TransactionalEmailSender,
    private val accountDeletionService: AccountDeletionService,
    private val clock: Clock,
) : InactiveAccountDeletionService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun runCleanup(): InactiveAccountDeletionResult? {
        if (!properties.enabled || !planProperties.enabled) return null

        val now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
        val users = userRepository.findAllWithTrackedActivity().collectList().awaitSingle()
        var warningsSent = 0
        var accountsDeleted = 0

        for (user in users) {
            runCatching {
                when (process(user, now)) {
                    Outcome.WARNING_SENT -> warningsSent++
                    Outcome.DELETED -> accountsDeleted++
                    Outcome.NONE -> Unit
                }
            }.onFailure { error ->
                logger.warn("Failed to process inactive user {}", user.id, error)
            }
        }

        logger.info(
            "Inactive account cleanup finished. Sent {} warning(s); deleted {} account(s).",
            warningsSent,
            accountsDeleted,
        )
        return InactiveAccountDeletionResult(warningsSent, accountsDeleted)
    }

    private suspend fun process(
        user: UserEntity,
        now: OffsetDateTime,
    ): Outcome {
        val userId = user.id ?: return Outcome.NONE
        val retention = planLimitService.resolve(user.role, PlanLimitKey.INACTIVITY_RETENTION_MONTHS).value ?: return Outcome.NONE
        val deletionAt = user.lastLoginAt.plusMonths(retention.toLong())
        val dueStage = WARNING_STAGES.firstOrNull { !now.isBefore(deletionAt.minusDays(it.toLong())) } ?: return Outcome.NONE

        if (user.inactivityNoticeStage == null || user.inactivityNoticeStage!! > dueStage) {
            val ownedGroupNames =
                groupRepository
                    .findAllByOwnerUserId(userId)
                    .map { it.name }
                    .collectList()
                    .awaitSingle()
            dispatchService.send(composer.buildInactivityWarning(user, dueStage, deletionAt, ownedGroupNames))
            userRepository.recordInactivityNoticeStage(userId, dueStage).awaitSingle()
            return Outcome.WARNING_SENT
        }

        if (!now.isBefore(deletionAt) && user.inactivityNoticeStage == MOST_URGENT_STAGE) {
            accountDeletionService.deleteAccountForUser(userId)
            return Outcome.DELETED
        }

        return Outcome.NONE
    }

    private enum class Outcome {
        NONE,
        WARNING_SENT,
        DELETED,
    }

    companion object {
        val WARNING_STAGES = listOf(1, 7, 30)
        const val MOST_URGENT_STAGE = 1
    }
}
